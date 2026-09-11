package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.auth.LogoutRequest
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.auth.AuthSession

class AuthRepositoryImpl(private val remote: AuthRemoteDataSource) : AuthRepository {
    override fun login(email: String, password: String, deviceId: String): ApiResult<AuthSession> =
        when (val result = remote.login(LoginRequest(email, password, deviceId))) {
            is ApiResult.Success -> ApiResult.Success(
                value = AuthSession(
                    memberId = result.value.member.memberId,
                    email = result.value.member.email,
                    nickname = result.value.member.nickname,
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    accessExpiresInSeconds = result.value.accessExpiresIn
                ),
                status = result.status
            )
            is ApiResult.Failure -> result
        }

    override fun logout(deviceId: String): ApiResult<Unit> =
        remote.logout(LogoutRequest(deviceId))
}
