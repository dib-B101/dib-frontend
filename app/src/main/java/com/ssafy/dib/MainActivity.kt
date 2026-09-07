package com.ssafy.dib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssafy.dib.core.navigation.AppNavHost
import com.ssafy.dib.ui.theme.DibTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DibTheme {
                AppNavHost()
            }
        }
    }
}