package xyz.haselbach.huushalt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.haselbach.huushalt.settings.ThemeMode
import xyz.haselbach.huushalt.ui.HuushaltApp
import xyz.haselbach.huushalt.ui.NavRequest
import xyz.haselbach.huushalt.ui.NavTarget
import xyz.haselbach.huushalt.ui.theme.HuushaltTheme

class MainActivity : ComponentActivity() {

    /** Screen requested by a notification (F5.3) or an app shortcut (F9.7). */
    private val navRequest = mutableStateOf<NavRequest?>(null)
    private var navSeq = 0

    // Internal for tests; called for the launch intent and new intents.
    internal fun handle(intent: Intent?) {
        val target = when (intent?.action) {
            ACTION_OPEN_TODAY -> NavTarget.TODAY
            ACTION_NEW_CHORE -> NavTarget.NEW_CHORE
            ACTION_CATALOG -> NavTarget.CATALOG
            else -> return
        }
        navRequest.value = NavRequest(target, ++navSeq)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        val settings = (application as HuushaltApplication).settings.settings
        setContent {
            val theme = settings.collectAsStateWithLifecycle(null).value?.theme ?: ThemeMode.SYSTEM
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            HuushaltTheme(darkTheme = dark) {
                HuushaltApp(navRequest = navRequest.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    companion object {
        const val ACTION_OPEN_TODAY = "xyz.haselbach.huushalt.action.OPEN_TODAY"
        const val ACTION_NEW_CHORE = "xyz.haselbach.huushalt.action.NEW_CHORE"
        const val ACTION_CATALOG = "xyz.haselbach.huushalt.action.CATALOG"
    }
}
