package com.ssafy.dib.data.remote.auth

import kotlinx.serialization.Serializable

@Serializable
enum class PhoneVerificationPurpose {
    SIGN_UP,
    FIND_EMAIL,
    RESET_PASSWORD,
    CHANGE_SENSITIVE
}

@Serializable
data class PhoneVerificationRequest(
    val phoneNumber: String,
    val purpose: PhoneVerificationPurpose
)

@Serializable
data class PhoneVerificationResponse(
    val verificationId: String,
    val expiresAt: String,
    val retryAfterSeconds: Long
)

@Serializable
data class PhoneVerificationConfirmRequest(val code: String)

@Serializable
data class PhoneVerificationConfirmResponse(
    val verificationToken: String,
    val expiresAt: String
)

@Serializable
data class EmailAvailabilityResponse(val available: Boolean)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    val nickname: String,
    val gender: String,
    val birthDate: String,
    val phoneNumber: String,
    val phoneVerificationToken: String
)

@Serializable
data class SignUpResponse(
    val memberId: String,
    val email: String,
    val nickname: String,
    val status: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class MemberDto(
    val memberId: String = "",
    val email: String = "",
    val nickname: String = "",
    val status: String = "",
    val role: String = ""
)

@Serializable
data class LoginResponse(
    val member: MemberDto,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceId: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long
)

@Serializable
data class LogoutRequest(val deviceId: String)
