package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.auth.KakaoAuthRequest
import com.ssafy.dib.data.remote.auth.KakaoAuthResponse
import com.ssafy.dib.data.remote.auth.KakaoSignupRequest
import com.ssafy.dib.data.remote.auth.LogoutRequest
import com.ssafy.dib.data.remote.auth.PhoneVerificationConfirmRequest
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
import com.ssafy.dib.data.remote.auth.PhoneVerificationRequest
import com.ssafy.dib.data.remote.auth.PasswordResetLinkRequest
import com.ssafy.dib.data.remote.auth.PasswordResetRequest
import com.ssafy.dib.data.remote.auth.RefreshTokenRequest
import com.ssafy.dib.data.remote.auth.SignUpRequest
import com.ssafy.dib.domain.auth.AuthRepository
import com.ssafy.dib.domain.auth.AuthSession
import com.ssafy.dib.domain.auth.AuthSessionStore
import com.ssafy.dib.domain.auth.KakaoAuthenticationResult
import com.ssafy.dib.domain.auth.KakaoSignupCommand
import com.ssafy.dib.domain.auth.PhoneVerificationChallenge
import com.ssafy.dib.domain.auth.PhoneVerificationConfirmation
import com.ssafy.dib.domain.auth.SignUpCommand
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: AuthSessionStore,
    private val now: () -> Long = System::currentTimeMillis
) : AuthRepository {
    override fun currentSession(): AuthSession? = sessionStore.read()

    override fun requestSignUpPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.SIGN_UP)

    override fun requestSensitivePhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.CHANGE_SENSITIVE)

    override fun requestFindEmailPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.FIND_EMAIL)

    override fun requestPasswordResetPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.RESET_PASSWORD)

    private fun requestPhoneVerification(phoneNumber: String, purpose: PhoneVerificationPurpose): ApiResult<PhoneVerificationChallenge> =
        when (val result = remote.requestPhoneVerification(
            PhoneVerificationRequest(phoneNumber, purpose)
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

    override fun findEmail(phoneVerificationToken: String): ApiResult<String> =
        when (val result = remote.findEmail(phoneVerificationToken)) {
            is ApiResult.Success -> ApiResult.Success(result.value.maskedEmail, result.status)
            is ApiResult.Failure -> result
        }

    override fun requestPasswordResetLink(email: String, phoneVerificationToken: String): ApiResult<Unit> =
        remote.requestPasswordResetLink(PasswordResetLinkRequest(email, phoneVerificationToken))

    override fun resetPassword(resetToken: String, newPassword: String): ApiResult<Unit> =
        remote.resetPassword(PasswordResetRequest(resetToken, newPassword))

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
                    memberId = result.value.memberId.idValue(),
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
                    memberId = result.value.member.memberId.idValue(),
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

    override fun authenticateWithKakao(
        authorizationCode: String,
        redirectUri: String,
        deviceId: String
    ): ApiResult<KakaoAuthenticationResult> {
        return when (val result = remote.authenticateWithKakao(
            KakaoAuthRequest(authorizationCode, redirectUri, deviceId)
        )) {
            is ApiResult.Success -> if (result.value.isNewMember) {
                val signupToken = result.value.signupToken
                    ?: return ApiResult.Failure(invalidKakaoResponse())
                ApiResult.Success(
                    KakaoAuthenticationResult.SignupRequired(
                        signupToken,
                        result.value.kakaoProfile?.nickname,
                        result.value.kakaoProfile?.profileImageUrl
                    ),
                    result.status
                )
            } else {
                val session = result.value.toSession(now())
                    ?: return ApiResult.Failure(invalidKakaoResponse())
                sessionStore.save(session)
                ApiResult.Success(KakaoAuthenticationResult.LoggedIn(session), result.status)
            }
            is ApiResult.Failure -> result
        }
    }

    override fun signUpWithKakao(command: KakaoSignupCommand): ApiResult<AuthSession> {
        return when (val result = remote.signUpWithKakao(
            KakaoSignupRequest(
                command.signupToken,
                command.email,
                command.name,
                command.nickname,
                command.gender,
                command.birthDate,
                command.phoneNumber,
                command.phoneVerificationToken,
                command.deviceId
            )
        )) {
            is ApiResult.Success -> {
                val session = result.value.toSession(now())
                    ?: return ApiResult.Failure(invalidKakaoResponse())
                sessionStore.save(session)
                ApiResult.Success(session, result.status)
            }
            is ApiResult.Failure -> result
        }
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

private fun JsonElement?.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: this?.toString()?.trim('"').orEmpty()

private fun KakaoAuthResponse.toSession(now: Long): AuthSession? {
    val responseMember = member ?: return null
    val access = accessToken ?: return null
    val refresh = refreshToken ?: return null
    val expiresIn = accessExpiresIn ?: return null
    return AuthSession(
        memberId = responseMember.memberId.idValue(),
        email = responseMember.email,
        nickname = responseMember.nickname,
        accessToken = access,
        refreshToken = refresh,
        accessExpiresAtEpochMillis = now + expiresIn * 1_000L
    )
}

private fun invalidKakaoResponse() = com.ssafy.dib.core.network.ApiFailure(
    status = null,
    code = "INVALID_KAKAO_RESPONSE",
    message = "카카오 로그인 응답이 올바르지 않습니다."
)
