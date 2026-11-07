package xyz.haselbach.huushalt.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import xyz.haselbach.huushalt.HuushaltApplication

/**
 * @property opensSettings the next [request] opens the system settings instead of the permission
 * dialog, because the user declined the dialog for good or is on Android 12 or older.
 */
class NotificationAccess(val canNotify: Boolean, val opensSettings: Boolean, val request: () -> Unit)

/**
 * Whether reminders can be shown, and how to fix it (F8.3). On Android 13+ the first request shows
 * the system dialog; once the user declined for good, only the app's notification settings help.
 */
@Composable
fun rememberNotificationAccess(permissionRequested: Boolean, onRequested: () -> Unit): NotificationAccess {
    val context = LocalContext.current
    val notifier = (context.applicationContext as HuushaltApplication).notifier
    var canNotify by remember { mutableStateOf(notifier.canNotify()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = notifier.canNotify() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = notifier.canNotify()
    }
    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (
        !permissionRequested ||
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
        )
    return NotificationAccess(canNotify, opensSettings = !canAsk) {
        if (canAsk) {
            onRequested()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
