package xyz.haselbach.huushalt.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/*
 * huushalt's own colours: a fresh green, with a warm terracotta as accent (streaks, celebrations,
 * the "add" button). Every role is set, so nothing falls back to Material's default purple, and
 * surfaces have a hint of green instead of plain grey.
 */
internal val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F0CF),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8DA),
    onSecondaryContainer = Color(0xFF0A1F15),
    tertiary = Color(0xFFA34A22),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF380D00),
    background = Color(0xFFF6FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FBF6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceTint = Color(0xFF2E6B4F),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C1),
    surfaceBright = Color(0xFFF6FBF6),
    surfaceDim = Color(0xFFD6DBD7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5F0),
    surfaceContainer = Color(0xFFEAEFEA),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDFE4DF),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEDF2ED),
    inversePrimary = Color(0xFF98D4B3),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF98D4B3),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF125238),
    onPrimaryContainer = Color(0xFFB4F0CF),
    secondary = Color(0xFFB4CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFD0E8DA),
    tertiary = Color(0xFFFFB597),
    onTertiary = Color(0xFF5A1B00),
    tertiaryContainer = Color(0xFF82340D),
    onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    surfaceTint = Color(0xFF98D4B3),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
    surfaceBright = Color(0xFF353B37),
    surfaceDim = Color(0xFF0F1512),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
    inverseSurface = Color(0xFFDFE4DF),
    inverseOnSurface = Color(0xFF2C322E),
    inversePrimary = Color(0xFF2E6B4F),
)

/**
 * Corner radii (Material 3 shape scale). Cards and the progress card use medium, dialogs and
 * sheets extra large; chips and small buttons small. Pills and avatars use CircleShape.
 */
internal val HuushaltShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Material 3 theme with dark mode (N5). huushalt's own colours by default; colours from the
 * wallpaper on Android 12+ when turned on in the settings.
 */
@Composable
fun HuushaltTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, shapes = HuushaltShapes, content = content)
}
