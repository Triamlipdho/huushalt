package xyz.haselbach.huushalt.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing on the 4/8 dp grid (see docs/DESIGN.md). Use these instead of ad-hoc values, so screens
 * keep the same rhythm. Typography and colours come from MaterialTheme; shapes from [HuushaltShapes].
 */
object Spacing {
    /** Between a value and its label, inside dense stat and badge cards. */
    val xs = 4.dp
    /** Between chips, buttons and other neighbouring touch targets; never less. */
    val sm = 8.dp
    /** Inside small cards; between rows of a card. */
    val md = 12.dp
    /** Screen edge margin, card padding, list item padding. */
    val lg = 16.dp
    /** Between sections; swipe icon inset. */
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Fixed sizes shared by several screens. */
object Sizes {
    /** Minimum touch target (Material: 48 dp). */
    val touchTarget = 48.dp
    /** Chore avatar in lists. */
    val avatar = 40.dp
    /** Chore avatar in the compact week view. */
    val avatarSmall = 32.dp
    /** Chore avatar on the detail screen. */
    val avatarLarge = 56.dp
    /** Thin bar of a bar chart. */
    val bar = 16.dp
    /** Stroke of the progress ring. */
    val ringStroke = 6.dp
}
