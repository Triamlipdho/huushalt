package xyz.haselbach.huushalt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.data.CompletionEntity
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.Schedule

data class HomeState(
    val today: LocalDate,
    val hasChores: Boolean,
    val overdue: List<Chore>,
    val dueToday: List<Chore>,
    val upcoming: List<Chore>,
    /** Chores done today, for the progress "x von y erledigt" (F7.1). */
    val doneToday: Int,
) {
    val open: Int get() = overdue.size + dueToday.size
    val total: Int get() = doneToday + open
}

sealed interface UiEvent {
    data class Completed(val choreName: String, val completionId: Long) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChoresViewModel(private val repository: ChoreRepository) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** All chores; null while loading. */
    val chores: StateFlow<List<Chore>?> = repository.chores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val doneToday: Flow<Set<Long>> = _today.flatMapLatest { repository.choreIdsDoneOn(it) }

    /** Home screen state; null while loading. */
    val home: StateFlow<HomeState?> = combine(repository.chores, doneToday, _today) { chores, done, today ->
        val order = compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() }
        val statuses = chores.associateWith { it.status(today) }
        fun matching(predicate: (DueStatus) -> Boolean) =
            chores.filter { predicate(statuses.getValue(it)) }.sortedWith(order)
        HomeState(
            today = today,
            hasChores = chores.isNotEmpty(),
            overdue = matching { it is DueStatus.Overdue },
            dueToday = matching { it is DueStatus.Today },
            upcoming = matching { it is DueStatus.Upcoming },
            doneToday = done.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = events.receiveAsFlow()

    /** Called when the app comes to the foreground, so "today" is right after midnight. */
    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    fun completions(choreId: Long): Flow<List<CompletionEntity>> = repository.completions(choreId)

    fun complete(chore: Chore) {
        viewModelScope.launch {
            val completionId = repository.complete(chore.id, LocalDate.now()) ?: return@launch
            events.send(UiEvent.Completed(chore.name, completionId))
        }
    }

    fun undoCompletion(completionId: Long) {
        viewModelScope.launch { repository.undoCompletion(completionId) }
    }

    fun postpone(choreId: Long, date: LocalDate) {
        viewModelScope.launch { repository.postpone(choreId, date) }
    }

    fun save(id: Long?, name: String, notes: String, schedule: Schedule, nextDue: LocalDate) {
        viewModelScope.launch { repository.save(id, name, notes, schedule, nextDue) }
    }

    fun delete(choreId: Long) {
        viewModelScope.launch { repository.delete(choreId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HuushaltApplication
                ChoresViewModel(app.repository)
            }
        }
    }
}
