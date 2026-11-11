package xyz.haselbach.huushalt.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.haselbach.huushalt.domain.ChoreColor
import xyz.haselbach.huushalt.ui.components.colorsFor

/**
 * WCAG 2.2 AA contrast for huushalt's own colours (N6, docs/DESIGN.md): 4.5:1 for text,
 * 3:1 for UI components such as button outlines and the progress ring.
 */
class ContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb).toDouble() / minOf(la, lb)
    }

    private fun check(scheme: String, name: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$scheme: $name is %.2f:1, needs %.1f:1".format(ratio, min), ratio >= min)
    }

    private fun textPairs(c: ColorScheme) = listOf(
        "onPrimary/primary" to (c.onPrimary to c.primary),
        "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
        "onTertiaryContainer/tertiaryContainer" to (c.onTertiaryContainer to c.tertiaryContainer),
        "onSurface/surface" to (c.onSurface to c.surface),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
        "onSurfaceVariant/surfaceContainerHigh" to (c.onSurfaceVariant to c.surfaceContainerHigh),
        // Section headers and text buttons
        "primary/surface" to (c.primary to c.surface),
        // "2 Tage überfällig" in lists and on the detail card
        "error/surface" to (c.error to c.surface),
        "error/secondaryContainer" to (c.error to c.secondaryContainer),
        // Streak on the progress card
        "tertiary/primaryContainer" to (c.tertiary to c.primaryContainer),
        "inverseOnSurface/inverseSurface" to (c.inverseOnSurface to c.inverseSurface),
    )

    private fun componentPairs(c: ColorScheme) = listOf(
        // Ring of the quiet "erledigt" button, colour swatch outline
        "outline/surface" to (c.outline to c.surface),
        // Progress ring on the progress card
        "primary/primaryContainer" to (c.primary to c.primaryContainer),
    )

    @Test
    fun themeColoursMeetAa() {
        listOf("light" to LightColors, "dark" to DarkColors).forEach { (name, scheme) ->
            textPairs(scheme).forEach { (pair, colors) -> check(name, pair, colors.first, colors.second, 4.5) }
            componentPairs(scheme).forEach { (pair, colors) -> check(name, pair, colors.first, colors.second, 3.0) }
        }
    }

    @Test
    fun choreSymbolsAreReadableOnTheirCircle() {
        ChoreColor.entries.forEach { color ->
            listOf(false, true).forEach { dark ->
                val c = color.colorsFor(dark)
                check(if (dark) "dark" else "light", "symbol $color", c.content, c.container, 4.5)
            }
        }
    }
}
