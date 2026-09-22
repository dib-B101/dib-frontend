package com.ssafy.dib

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssafy.dib.core.navigation.AppNavHost
import com.ssafy.dib.ui.theme.DibTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var oauthCallbackUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oauthCallbackUri = intent?.data
        enableEdgeToEdge()

        setContent {
            DibTheme {
                AppNavHost(
                    oauthCallbackUri = oauthCallbackUri,
                    onOAuthCallbackConsumed = {
                        oauthCallbackUri = null
                        intent?.setData(null)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        oauthCallbackUri = intent.data
    }
}
