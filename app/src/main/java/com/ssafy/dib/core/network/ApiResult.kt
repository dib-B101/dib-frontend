package com.ssafy.dib.core.network

import kotlinx.serialization.json.JsonElement

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T, val status: Int) : ApiResult<T>
    data class Failure(val error: ApiFailure) : ApiResult<Nothing>
}

data class ApiFailure(
    val status: Int?,
    val code: String,
    val message: String,
    val path: String? = null,
    val traceId: String? = null,
    val timestamp: String? = null,
    val fieldErrors: JsonElement? = null,
    val cause: Throwable? = null
) {
    val requiresLogin: Boolean
        get() = code in setOf(
            "UNAUTHORIZED",
            "REFRESH_TOKEN_EXPIRED",
            "SESSION_REVOKED",
            "DEVICE_MISMATCH"
        )
}

object ApiErrorCodes {
    const val NETWORK_UNAVAILABLE = "NETWORK_UNAVAILABLE"
    const val INVALID_RESPONSE = "INVALID_RESPONSE"
    const val CLIENT_NOT_CONFIGURED = "CLIENT_NOT_CONFIGURED"
}
