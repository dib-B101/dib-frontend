package com.ssafy.dib

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ssafy.dib.core.navigation.AppNavHost
import com.ssafy.dib.core.session.SessionInactivityTracker
import com.ssafy.dib.ui.theme.DibTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val sessionInactivityTracker by lazy { SessionInactivityTracker(this) }
    private val localNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recreate()
    }
    private var oauthCallbackUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oauthCallbackUri = intent?.data
        requestLocalDevelopmentNetworkAccess()
        enableEdgeToEdge()

        setContent {
            DibTheme {
                AppNavHost(
                    sessionInactivityTracker = sessionInactivityTracker,
                    oauthCallbackUri = oauthCallbackUri,
                    onOAuthCallbackConsumed = {
                        oauthCallbackUri = null
                        intent?.setData(null)
                    }
                )
            }
        }
    }

    private fun requestLocalDevelopmentNetworkAccess() {
        if (
            !BuildConfig.DEBUG ||
            Build.VERSION.SDK_INT < 37 ||
            !BuildConfig.API_BASE_URL.isLocalDevelopmentAddress() ||
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        oauthCallbackUri = intent.data
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionInactivityTracker.recordInteraction()
    }
}

private fun String.isLocalDevelopmentAddress(): Boolean =
    contains("10.0.2.2") || contains("127.0.0.1") || contains("localhost")
