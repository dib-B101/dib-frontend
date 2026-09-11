package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.auth.LogoutRequest
import com.ssafy.dib.data.remote.auth.PhoneVerificationConfirmRequest
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
import com.ssafy.dib.data.remote.auth.PhoneVerificationRequest
import com.ssafy.dib.data.remote.auth.RefreshTokenRequest
import com.ssafy.dib.data.remote.auth.SignUpRequest
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.auth.AuthSession
import com.ssafy.dib.domain.auth.AuthSessionStore
import com.ssafy.dib.domain.auth.PhoneVerificationChallenge
import com.ssafy.dib.domain.auth.PhoneVerificationConfirmation
import com.ssafy.dib.domain.auth.SignUpCommand

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: AuthSessionStore,
    private val now: () -> Long = System::currentTimeMillis
) : AuthRepository {
    override fun currentSession(): AuthSession? = sessionStore.read()

    override fun requestSignUpPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        when (val result = remote.requestPhoneVerification(
            PhoneVerificationRequest(phoneNumber, PhoneVerificationPurpose.SIGN_UP)
        )) {
            is ApiResult.Success -> ApiResult.Success(
                PhoneVerificationChallenge(
                    verificationId = result.value.verificationId,
                    expiresAt = result.value.expiresAt,
                    retryAfterSeconds = result.value.retryAfterSeconds
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun confirmPhoneVerification(
        verificationId: String,
        code: String
    ): ApiResult<PhoneVerificationConfirmation> =
        when (val result = remote.confirmPhoneVerification(
            verificationId,
            PhoneVerificationConfirmRequest(code)
        )) {
            is ApiResult.Success -> ApiResult.Success(
                PhoneVerificationConfirmation(
                    verificationToken = result.value.verificationToken,
                    expiresAt = result.value.expiresAt
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun checkEmailAvailability(email: String): ApiResult<Boolean> =
        when (val result = remote.checkEmailAvailability(email)) {
            is ApiResult.Success -> ApiResult.Success(result.value.available, result.status)
            is ApiResult.Failure -> result
        }

    override fun signUp(command: SignUpCommand): ApiResult<AuthSession> =
        when (val result = remote.signUp(
            SignUpRequest(
                email = command.email,
                password = command.password,
                name = command.name,
                nickname = command.nickname,
                gender = command.gender,
                birthDate = command.birthDate,
                phoneNumber = command.phoneNumber,
                phoneVerificationToken = command.phoneVerificationToken
            )
        )) {
            is ApiResult.Success -> {
                val session = AuthSession(
                    memberId = result.value.memberId,
                    email = result.value.email,
                    nickname = result.value.nickname,
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    accessExpiresAtEpochMillis = now() + result.value.accessExpiresIn * 1_000L
                )
                sessionStore.save(session)
                ApiResult.Success(session, result.status)
            }
            is ApiResult.Failure -> result
        }

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
