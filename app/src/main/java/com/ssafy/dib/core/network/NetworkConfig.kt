package com.ssafy.dib.core.network

import com.ssafy.dib.BuildConfig

data class NetworkConfig(
    val apiBaseUrl: String,
    val webSocketUrl: String
) {
    val isRestConfigured: Boolean get() = apiBaseUrl.isNotBlank()
    val isWebSocketConfigured: Boolean get() = webSocketUrl.isNotBlank()

    fun requireApiBaseUrl(): String = requireConfiguredUrl(
        value = apiBaseUrl,
        propertyName = "DIB_API_BASE_URL",
        supportedSchemes = listOf("https://", "http://")
    )

    fun requireWebSocketUrl(): String = requireConfiguredUrl(
        value = webSocketUrl,
        propertyName = "DIB_WS_URL",
        supportedSchemes = listOf("wss://", "ws://")
    )

    private fun requireConfiguredUrl(
        value: String,
        propertyName: String,
        supportedSchemes: List<String>
    ): String {
        val normalized = value.trim()
        check(normalized.isNotBlank()) { "$propertyName Gradle property is not configured." }
        check(supportedSchemes.any(normalized::startsWith)) {
            "$propertyName must start with ${supportedSchemes.joinToString(" or ")}."
        }
        return normalized.trimEnd('/')
    }

    companion object {
        fun fromBuildConfig() = NetworkConfig(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            webSocketUrl = BuildConfig.WEB_SOCKET_URL
        )
    }
}
