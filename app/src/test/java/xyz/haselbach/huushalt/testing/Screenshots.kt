package xyz.haselbach.huushalt.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import java.io.File

/**
 * Saves a screenshot to app/build/screenshots/ for a visual check. Draws the window directly,
 * because captureToImage() waits for a frame Robolectric doesn't deliver.
 */
fun AndroidComposeTestRule<*, out ComponentActivity>.screenshot(name: String) {
    waitForIdle()
    val dir = File("build/screenshots").apply { mkdirs() }
    val view = activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    runOnUiThread {
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        // Dialogs and bottom sheets live in windows of their own; draw them on top.
        openWindows().filter { it !== view && it.isShown }.forEach { window ->
            val location = IntArray(2).also(window::getLocationOnScreen)
            canvas.save()
            canvas.translate(location[0].toFloat(), location[1].toFloat())
            window.draw(canvas)
            canvas.restore()
        }
    }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

/** Root views of all windows, in the order they were added (Robolectric keeps them here too). */
private fun openWindows(): List<View> = runCatching {
    val global = Class.forName("android.view.WindowManagerGlobal")
    val instance = global.getMethod("getInstance").invoke(null)
    val field = global.getDeclaredField("mViews").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    (field.get(instance) as List<View>).toList()
}.getOrDefault(emptyList())
