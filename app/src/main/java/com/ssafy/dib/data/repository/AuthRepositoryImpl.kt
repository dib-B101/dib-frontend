package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.auth.KakaoAuthRequest
import com.ssafy.dib.data.remote.auth.KakaoAuthResponse
import com.ssafy.dib.data.remote.auth.KakaoSignupRequest
import com.ssafy.dib.data.remote.auth.LogoutRequest
import com.ssafy.dib.data.remote.auth.FirebasePhoneVerificationRequest
import com.ssafy.dib.data.remote.auth.FirebasePhoneAuthGateway
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal fun firebasePhoneFailure(exception: Throwable): ApiResult.Failure {
    val causes = generateSequence(exception) { it.cause }.toList()
    val firebaseError = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()?.errorCode
    return firebasePhoneFailureForCode(
        firebaseError,
        networkError = causes.any { it is FirebaseNetworkException },
        billingDisabled = causes.any { it.message?.contains("BILLING_NOT_ENABLED") == true }
    )
}

internal fun firebasePhoneFailureForCode(
    firebaseError: String?,
    networkError: Boolean = false,
    billingDisabled: Boolean = false
): ApiResult.Failure {
    val code = when {
        billingDisabled || firebaseError == "ERROR_BILLING_NOT_ENABLED" -> "SMS_BILLING_NOT_ENABLED"
        firebaseError == "ERROR_INVALID_VERIFICATION_CODE" -> "INVALID_CODE"
        firebaseError == "ERROR_SESSION_EXPIRED" || firebaseError == "ERROR_INVALID_VERIFICATION_ID" -> "VERIFICATION_EXPIRED"
        firebaseError == "ERROR_INVALID_PHONE_NUMBER" -> "INVALID_PHONE"
        firebaseError == "ERROR_TOO_MANY_REQUESTS" || firebaseError == "ERROR_QUOTA_EXCEEDED" -> "RATE_LIMITED"
        firebaseError == "ERROR_APP_NOT_AUTHORIZED" -> "APP_VERIFICATION_FAILED"
        networkError -> "NETWORK_ERROR"
        else -> "FIREBASE_PHONE_AUTH_FAILED"
    }
    return ApiResult.Failure(ApiFailure(null, code, ""))
}

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: AuthSessionStore,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val phoneAuth: FirebasePhoneAuthGateway? = null
) : AuthRepository {
    private val pendingPhones = ConcurrentHashMap<String, Pair<String, PhoneVerificationPurpose>>()
    override fun currentSession(): AuthSession? = sessionStore.read()

    override fun requestSignUpPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.SIGN_UP)

    override fun requestSensitivePhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.CHANGE_SENSITIVE)

    override fun requestFindEmailPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.FIND_EMAIL)

    override fun requestPasswordResetPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge> =
        requestPhoneVerification(phoneNumber, PhoneVerificationPurpose.RESET_PASSWORD)

    private fun requestPhoneVerification(phoneNumber: String, purpose: PhoneVerificationPurpose): ApiResult<PhoneVerificationChallenge> {
        val gateway = phoneAuth ?: return firebaseFailure("Firebase 휴대전화 인증이 설정되지 않았어요.")
        return try {
            val challenge = gateway.request(phoneNumber)
            if (challenge.automaticCredential != null) {
                val idToken = gateway.signIn(challenge.automaticCredential)
                when (val result = remote.verifyFirebasePhone(FirebasePhoneVerificationRequest(idToken, phoneNumber, purpose))) {
                    is ApiResult.Success -> ApiResult.Success(PhoneVerificationChallenge(
                        verificationId = UUID.randomUUID().toString(),
                        expiresAt = result.value.expiresAt,
                        retryAfterSeconds = 60,
                        autoVerificationToken = result.value.verificationToken
                    ), result.status)
                    is ApiResult.Failure -> result
                }
            } else {
                val id = challenge.verificationId ?: error("Firebase 인증 요청 정보를 받지 못했어요.")
                pendingPhones[id] = phoneNumber to purpose
                ApiResult.Success(PhoneVerificationChallenge(id, Instant.ofEpochMilli(now() + 60_000).toString(), 60), 202)
            }
        } catch (e: Exception) {
            firebasePhoneFailure(e)
        }
    }

    override fun confirmPhoneVerification(
        verificationId: String,
        code: String
    ): ApiResult<PhoneVerificationConfirmation> {
        val gateway = phoneAuth ?: return firebaseFailure("Firebase 휴대전화 인증이 설정되지 않았어요.")
        val (phone, purpose) = pendingPhones[verificationId]
            ?: return firebaseFailure("인증 요청이 만료됐어요. 다시 요청해주세요.")
        return try {
            val idToken = gateway.signInWithCode(verificationId, code)
            when (val result = remote.verifyFirebasePhone(FirebasePhoneVerificationRequest(idToken, phone, purpose))) {
                is ApiResult.Success -> {
                    pendingPhones.remove(verificationId)
                    ApiResult.Success(PhoneVerificationConfirmation(result.value.verificationToken, result.value.expiresAt), result.status)
                }
                is ApiResult.Failure -> result
            }
        } catch (e: Exception) {
            firebasePhoneFailure(e)
        }
    }

    private fun firebaseFailure(message: String): ApiResult.Failure =
        ApiResult.Failure(ApiFailure(null, "FIREBASE_PHONE_AUTH_FAILED", message))

    override fun checkEmailAvailability(email: String): ApiResult<Boolean> =
        when (val result = remote.checkEmailAvailability(email)) {
            is ApiResult.Success -> ApiResult.Success(result.value.available, result.status)
            is ApiResult.Failure -> result
        }

    override fun findEmail(phoneVerificationToken: String, phoneNumber: String): ApiResult<String> =
        when (val result = remote.findEmail(phoneVerificationToken, phoneNumber)) {
            is ApiResult.Success -> ApiResult.Success(result.value.maskedEmail, result.status)
            is ApiResult.Failure -> result
        }

    override fun requestPasswordResetLink(email: String, phoneNumber: String, phoneVerificationToken: String): ApiResult<Unit> =
        remote.requestPasswordResetLink(PasswordResetLinkRequest(email, phoneNumber, phoneVerificationToken))

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
                phoneVerificationToken = command.phoneVerificationToken,
                deviceId = deviceId
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
