package com.ssafy.dib

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ssafy.dib.core.navigation.AppNavHost
import com.ssafy.dib.core.session.SessionInactivityTracker
import com.ssafy.dib.ui.theme.DibTheme

class MainActivity : ComponentActivity() {
    private val sessionInactivityTracker by lazy { SessionInactivityTracker(this) }
    private val localNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalDevelopmentNetworkAccess()
        enableEdgeToEdge()

        setContent {
            DibTheme {
                AppNavHost(sessionInactivityTracker)
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

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionInactivityTracker.recordInteraction()
    }
}

private fun String.isLocalDevelopmentAddress(): Boolean =
    contains("10.0.2.2") || contains("127.0.0.1") || contains("localhost")
