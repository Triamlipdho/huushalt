package xyz.haselbach.huushalt.testing

import android.graphics.Bitmap
import android.graphics.Canvas
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
    runOnUiThread { view.draw(Canvas(bitmap)) }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
