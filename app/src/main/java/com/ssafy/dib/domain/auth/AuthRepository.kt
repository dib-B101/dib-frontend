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

data class PhoneVerificationChallenge(
    val verificationId: String,
    val expiresAt: String,
    val retryAfterSeconds: Long
)

data class PhoneVerificationConfirmation(
    val verificationToken: String,
    val expiresAt: String
)

data class SignUpCommand(
    val email: String,
    val password: String,
    val name: String,
    val nickname: String,
    val gender: String,
    val birthDate: String,
    val phoneNumber: String,
    val phoneVerificationToken: String
)

interface AuthSessionStore {
    fun read(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

interface AuthRepository {
    fun currentSession(): AuthSession?
    fun requestSignUpPhoneVerification(phoneNumber: String): ApiResult<PhoneVerificationChallenge>
    fun confirmPhoneVerification(
        verificationId: String,
        code: String
    ): ApiResult<PhoneVerificationConfirmation>
    fun checkEmailAvailability(email: String): ApiResult<Boolean>
    fun signUp(command: SignUpCommand): ApiResult<AuthSession>
    fun login(email: String, password: String, deviceId: String): ApiResult<AuthSession>
    fun refresh(deviceId: String): ApiResult<AuthSession>
    fun logout(deviceId: String): ApiResult<Unit>
}
