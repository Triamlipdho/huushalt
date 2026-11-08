package xyz.haselbach.huushalt.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.data.Backup

/** Export, import and deleting all data (F9.3, F9.6). */
class DataViewModel(private val app: HuushaltApplication) : AndroidViewModel(app) {

    enum class Message { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_FAILED, DELETED }

    private val messages = Channel<Message>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    /** A backup that was read and waits for the user to confirm replacing everything. */
    private val _pendingImport = MutableStateFlow<Backup?>(null)
    val pendingImport: StateFlow<Backup?> = _pendingImport.asStateFlow()

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = app.backups.toJson(app.backups.read())
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(json) }
                }
            }.isSuccess
            messages.send(if (ok) Message.EXPORTED else Message.EXPORT_FAILED)
        }
    }

    /** Reads the file completely; nothing changes until [confirmImport]. */
    fun readImport(uri: Uri) {
        viewModelScope.launch {
            val backup = runCatching {
                val text = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                app.backups.fromJson(text)
            }.getOrNull()
            if (backup == null) messages.send(Message.IMPORT_FAILED) else _pendingImport.value = backup
        }
    }

    fun confirmImport() {
        val backup = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            val ok = runCatching { app.backups.restore(backup) }.isSuccess
            messages.send(if (ok) Message.IMPORTED else Message.IMPORT_FAILED)
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun deleteAll() {
        viewModelScope.launch {
            app.backups.deleteAll()
            app.notifier.cancel()
            messages.send(Message.DELETED)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DataViewModel(this[APPLICATION_KEY] as HuushaltApplication) }
        }
    }
}
