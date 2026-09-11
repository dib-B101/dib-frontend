package com.ssafy.dib.domain.auth

import com.ssafy.dib.core.network.ApiResult

data class AuthSession(
    val memberId: String,
    val email: String,
    val nickname: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresInSeconds: Long
)

interface AuthRepository {
    fun login(email: String, password: String, deviceId: String): ApiResult<AuthSession>
    fun logout(deviceId: String): ApiResult<Unit>
}
