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
    // DEVICE_MISMATCH 는 서버에서 400 BAD_REQUEST 라 재로그인 대상이 아니고,
    // ACCESS_TOKEN_EXPIRED 는 서버 ErrorCode 에 아예 없는 코드라 둘 다 제외한다
    val requiresLogin: Boolean
        get() = code in setOf(
            "UNAUTHORIZED",
            "REFRESH_TOKEN_EXPIRED",
            "SESSION_REVOKED"
        )
}

object ApiErrorCodes {
    const val NETWORK_UNAVAILABLE = "NETWORK_UNAVAILABLE"
    const val INVALID_RESPONSE = "INVALID_RESPONSE"
    const val CLIENT_NOT_CONFIGURED = "CLIENT_NOT_CONFIGURED"
    const val INVALID_CURSOR = "INVALID_CURSOR"
    const val BACKEND_NOT_IMPLEMENTED = "BACKEND_NOT_IMPLEMENTED"
}
