package xyz.haselbach.huushalt.testing

import kotlinx.coroutines.runBlocking
import org.robolectric.RuntimeEnvironment
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.settings.AppSettings

/**
 * DataStore keeps one instance per process, so settings written in one Robolectric test are
 * still there in the next. Call this at the start of every test that depends on settings.
 */
fun resetSettings() = runBlocking {
    (RuntimeEnvironment.getApplication() as HuushaltApplication).settings.update { AppSettings() }
}
