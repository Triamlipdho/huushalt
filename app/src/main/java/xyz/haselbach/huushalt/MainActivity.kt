package xyz.haselbach.huushalt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.haselbach.huushalt.settings.ThemeMode
import xyz.haselbach.huushalt.ui.HuushaltApp
import xyz.haselbach.huushalt.ui.theme.HuushaltTheme

class MainActivity : ComponentActivity() {

    /** Incremented whenever a notification asks to show the daily overview (F5.3). */
    private val openTodayRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as HuushaltApplication).settings.settings
        setContent {
            val theme = settings.collectAsStateWithLifecycle(null).value?.theme ?: ThemeMode.SYSTEM
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            HuushaltTheme(darkTheme = dark) {
                HuushaltApp(openTodayRequests = openTodayRequests.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_TODAY) openTodayRequests.intValue++
    }

    companion object {
        const val ACTION_OPEN_TODAY = "xyz.haselbach.huushalt.action.OPEN_TODAY"
    }
}
