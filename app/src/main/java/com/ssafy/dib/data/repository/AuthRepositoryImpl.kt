package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.auth.LogoutRequest
import com.ssafy.dib.data.remote.auth.RefreshTokenRequest
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.auth.AuthSession
import com.ssafy.dib.domain.auth.AuthSessionStore

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: AuthSessionStore,
    private val now: () -> Long = System::currentTimeMillis
) : AuthRepository {
    override fun currentSession(): AuthSession? = sessionStore.read()

    override fun login(email: String, password: String, deviceId: String): ApiResult<AuthSession> =
        when (val result = remote.login(LoginRequest(email, password, deviceId))) {
            is ApiResult.Success -> {
                val session = AuthSession(
                    memberId = result.value.member.memberId,
                    email = result.value.member.email,
                    nickname = result.value.member.nickname,
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    accessExpiresAtEpochMillis = now() + result.value.accessExpiresIn * 1_000L
                )
                sessionStore.save(session)
                ApiResult.Success(value = session, status = result.status)
            }
            is ApiResult.Failure -> result
        }

    override fun refresh(deviceId: String): ApiResult<AuthSession> {
        val current = sessionStore.read() ?: return ApiResult.Failure(
            com.ssafy.dib.core.network.ApiFailure(
                status = 401,
                code = "SESSION_NOT_FOUND",
                message = "저장된 로그인 정보가 없습니다."
            )
        )
        return when (val result = remote.refresh(RefreshTokenRequest(current.refreshToken, deviceId))) {
            is ApiResult.Success -> {
                val session = current.copy(
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    accessExpiresAtEpochMillis = now() + result.value.accessExpiresIn * 1_000L
                )
                sessionStore.save(session)
                ApiResult.Success(session, result.status)
            }
            is ApiResult.Failure -> {
                if (result.error.requiresLogin) sessionStore.clear()
                result
            }
        }
    }

    override fun logout(deviceId: String): ApiResult<Unit> {
        val result = remote.logout(LogoutRequest(deviceId))
        sessionStore.clear()
        return result
    }
}
