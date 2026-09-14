package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.AccessTokenRefresher
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.RefreshTokenRequest
import com.ssafy.dib.data.remote.auth.RefreshTokenResponse
import com.ssafy.dib.domain.auth.AuthSessionStore

class SessionTokenRefresher(
    private val sessionStore: AuthSessionStore,
    private val deviceId: String,
    private val refreshRequest: (RefreshTokenRequest) -> ApiResult<RefreshTokenResponse>,
    private val now: () -> Long = System::currentTimeMillis
) : AccessTokenRefresher {
    @Synchronized
    override fun refresh(failedAccessToken: String): String? {
        val current = sessionStore.read() ?: return null
        if (current.accessToken != failedAccessToken) return current.accessToken

        return when (val result = refreshRequest(RefreshTokenRequest(current.refreshToken, deviceId))) {
            is ApiResult.Success -> {
                val refreshed = current.copy(
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    accessExpiresAtEpochMillis = now() + result.value.accessExpiresIn * 1_000L
                )
                sessionStore.save(refreshed)
                refreshed.accessToken
            }
            is ApiResult.Failure -> {
                if (result.error.requiresLogin) sessionStore.clear()
                null
            }
        }
    }
}
