package com.ssafy.dib.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Opens an external payment page once and refreshes server state when the user returns. */
@Composable
fun ExternalPaymentReturnEffect(
    requestKey: String?,
    paymentUrl: String?,
    onReturn: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnReturn by rememberUpdatedState(onReturn)
    var awaitingReturn by remember(requestKey) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, requestKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingReturn) {
                awaitingReturn = false
                currentOnReturn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(requestKey, paymentUrl) {
        val url = paymentUrl?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        awaitingReturn = true
        if (runCatching { uriHandler.openUri(url) }.isFailure) awaitingReturn = false
    }
}
