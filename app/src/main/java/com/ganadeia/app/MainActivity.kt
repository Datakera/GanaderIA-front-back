package com.ganadeia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ganadeia.app.ui.navigation.AppNavigation
import com.ganadeia.app.ui.theme.GanadeIATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GanadeIATheme {
                AppNavigation()
            }
        }
    }
}
