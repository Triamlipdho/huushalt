package xyz.haselbach.huushalt.ui.components

import xyz.haselbach.huushalt.ui.theme.Sizes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.ChoreColor
import xyz.haselbach.huushalt.domain.ChoreLook
import xyz.haselbach.huushalt.domain.ChoreSymbol

val ChoreSymbol.icon: ImageVector
    get() = when (this) {
        ChoreSymbol.TRASH -> Icons.Filled.Delete
        ChoreSymbol.RECYCLING -> Icons.Filled.Recycling
        ChoreSymbol.PLANTS -> Icons.Filled.LocalFlorist
        ChoreSymbol.WINDOW -> Icons.Filled.Window
        ChoreSymbol.FRIDGE -> Icons.Filled.Kitchen
        ChoreSymbol.OVEN -> Icons.Filled.Microwave
        ChoreSymbol.KETTLE -> Icons.Filled.CoffeeMaker
        ChoreSymbol.DISHES -> Icons.Filled.Restaurant
        ChoreSymbol.KITCHEN -> Icons.Filled.Countertops
        ChoreSymbol.BED -> Icons.Filled.Bed
        ChoreSymbol.LAUNDRY -> Icons.Filled.LocalLaundryService
        ChoreSymbol.IRONING -> Icons.Filled.Iron
        ChoreSymbol.TOWELS -> Icons.Filled.Checkroom
        ChoreSymbol.SHOWER -> Icons.Filled.Shower
        ChoreSymbol.BATH -> Icons.Filled.Bathtub
        ChoreSymbol.DRAIN -> Icons.Filled.Plumbing
        ChoreSymbol.AIR -> Icons.Filled.Air
        ChoreSymbol.SMOKE_ALARM -> Icons.Filled.LocalFireDepartment
        ChoreSymbol.VACUUM -> Icons.Filled.CleaningServices
        ChoreSymbol.DUSTING -> Icons.Filled.AutoAwesome
        ChoreSymbol.PETS -> Icons.Filled.Pets
        ChoreSymbol.SHOPPING -> Icons.Filled.ShoppingCart
        ChoreSymbol.PAPERWORK -> Icons.Filled.Description
        ChoreSymbol.REPAIR -> Icons.Filled.Build
        ChoreSymbol.CAR -> Icons.Filled.DirectionsCar
        ChoreSymbol.SOFA -> Icons.Filled.Weekend
        ChoreSymbol.HOME -> Icons.Filled.Home
    }

/** Background and icon colour of a symbol, readable in light and dark mode. */
data class SymbolColors(val container: Color, val content: Color)

@Composable
fun ChoreColor.colors(dark: Boolean = isSystemInDarkTheme()): SymbolColors = colorsFor(dark)

/** As [colors], outside of composition (contrast test). */
fun ChoreColor.colorsFor(dark: Boolean): SymbolColors {
    // Pastel containers with dark icons in light mode; deep containers with light icons in dark mode.
    val (light, deep) = when (this) {
        ChoreColor.BLUE -> 0xFFD6E4FF to 0xFF1F3A66
        ChoreColor.TEAL -> 0xFFC8EFEA to 0xFF124F4A
        ChoreColor.GREEN -> 0xFFD3F1C9 to 0xFF24501C
        ChoreColor.YELLOW -> 0xFFF7EBB0 to 0xFF594A0E
        ChoreColor.ORANGE -> 0xFFFFDDC2 to 0xFF663313
        ChoreColor.RED -> 0xFFFFD6D3 to 0xFF6B1F1A
        ChoreColor.PINK -> 0xFFFBD5E8 to 0xFF62203F
        ChoreColor.PURPLE -> 0xFFE5DAFA to 0xFF41296B
        ChoreColor.GREY -> 0xFFE2E4E8 to 0xFF3B4048
    }
    val (onLight, onDeep) = when (this) {
        ChoreColor.BLUE -> 0xFF1F3A66 to 0xFFD6E4FF
        ChoreColor.TEAL -> 0xFF124F4A to 0xFFC8EFEA
        ChoreColor.GREEN -> 0xFF24501C to 0xFFD3F1C9
        ChoreColor.YELLOW -> 0xFF594A0E to 0xFFF7EBB0
        ChoreColor.ORANGE -> 0xFF663313 to 0xFFFFDDC2
        ChoreColor.RED -> 0xFF6B1F1A to 0xFFFFD6D3
        ChoreColor.PINK -> 0xFF62203F to 0xFFFBD5E8
        ChoreColor.PURPLE -> 0xFF41296B to 0xFFE5DAFA
        ChoreColor.GREY -> 0xFF3B4048 to 0xFFE2E4E8
    }
    return if (dark) SymbolColors(Color(deep), Color(onDeep)) else SymbolColors(Color(light), Color(onLight))
}

/** A chore's symbol in a coloured circle (F1.5). Decorative: the chore name says what it is. */
@Composable
fun ChoreAvatar(look: ChoreLook, modifier: Modifier = Modifier, size: Dp = Sizes.avatar) {
    val colors = look.color.colors()
    Box(
        modifier = modifier.size(size).background(colors.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(look.symbol.icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(size * 0.55f))
    }
}

/** German name, for TalkBack in the symbol picker. */
val ChoreSymbol.label: Int
    get() = when (this) {
        ChoreSymbol.TRASH -> R.string.symbol_trash
        ChoreSymbol.RECYCLING -> R.string.symbol_recycling
        ChoreSymbol.PLANTS -> R.string.symbol_plants
        ChoreSymbol.WINDOW -> R.string.symbol_window
        ChoreSymbol.FRIDGE -> R.string.symbol_fridge
        ChoreSymbol.OVEN -> R.string.symbol_oven
        ChoreSymbol.KETTLE -> R.string.symbol_kettle
        ChoreSymbol.DISHES -> R.string.symbol_dishes
        ChoreSymbol.KITCHEN -> R.string.symbol_kitchen
        ChoreSymbol.BED -> R.string.symbol_bed
        ChoreSymbol.LAUNDRY -> R.string.symbol_laundry
        ChoreSymbol.IRONING -> R.string.symbol_ironing
        ChoreSymbol.TOWELS -> R.string.symbol_towels
        ChoreSymbol.SHOWER -> R.string.symbol_shower
        ChoreSymbol.BATH -> R.string.symbol_bath
        ChoreSymbol.DRAIN -> R.string.symbol_drain
        ChoreSymbol.AIR -> R.string.symbol_air
        ChoreSymbol.SMOKE_ALARM -> R.string.symbol_smoke_alarm
        ChoreSymbol.VACUUM -> R.string.symbol_vacuum
        ChoreSymbol.DUSTING -> R.string.symbol_dusting
        ChoreSymbol.PETS -> R.string.symbol_pets
        ChoreSymbol.SHOPPING -> R.string.symbol_shopping
        ChoreSymbol.PAPERWORK -> R.string.symbol_paperwork
        ChoreSymbol.REPAIR -> R.string.symbol_repair
        ChoreSymbol.CAR -> R.string.symbol_car
        ChoreSymbol.SOFA -> R.string.symbol_sofa
        ChoreSymbol.HOME -> R.string.symbol_home
    }

val ChoreColor.label: Int
    get() = when (this) {
        ChoreColor.BLUE -> R.string.color_blue
        ChoreColor.TEAL -> R.string.color_teal
        ChoreColor.GREEN -> R.string.color_green
        ChoreColor.YELLOW -> R.string.color_yellow
        ChoreColor.ORANGE -> R.string.color_orange
        ChoreColor.RED -> R.string.color_red
        ChoreColor.PINK -> R.string.color_pink
        ChoreColor.PURPLE -> R.string.color_purple
        ChoreColor.GREY -> R.string.color_grey
    }
