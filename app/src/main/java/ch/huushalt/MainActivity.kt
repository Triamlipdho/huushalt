package ch.huushalt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.huushalt.ui.HomeScreen
import ch.huushalt.ui.theme.HuushaltTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuushaltTheme {
                HomeScreen()
            }
        }
    }
}
