package xyz.haselbach.huushalt.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/** Tapping a chore opens its sheet; this goes on to the full detail screen. */
@OptIn(ExperimentalTestApi::class)
fun AndroidComposeTestRule<*, *>.openDetails(name: String) {
    onNodeWithText(name).performClick()
    waitUntilAtLeastOneExists(hasText("Details und Verlauf"), timeoutMillis = 5_000)
    onNodeWithText("Details und Verlauf").performClick()
    waitUntilDoesNotExist(hasText("Details und Verlauf"), timeoutMillis = 5_000)
}
