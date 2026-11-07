package xyz.haselbach.huushalt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import xyz.haselbach.huushalt.ui.HomeScreen
import xyz.haselbach.huushalt.ui.theme.HuushaltTheme

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
