package com.ssafy.dib.domain.auth

import com.ssafy.dib.core.network.ApiResult

data class AuthSession(
    val memberId: String,
    val email: String,
    val nickname: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtEpochMillis: Long
) {
    fun needsRefresh(nowEpochMillis: Long, bufferMillis: Long = 60_000L): Boolean =
        accessExpiresAtEpochMillis <= nowEpochMillis + bufferMillis
}

interface AuthSessionStore {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

interface AuthRepository {
    fun currentSession(): AuthSession?
    fun login(email: String, password: String, deviceId: String): ApiResult<AuthSession>
    fun refresh(deviceId: String): ApiResult<AuthSession>
    fun logout(deviceId: String): ApiResult<Unit>
}
