package com.ssafy.dib.data.remote.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
data class MaskedEmailResponse(val maskedEmail: String)

@Serializable
data class PasswordResetLinkRequest(val email: String, val phoneVerificationToken: String)

@Serializable
data class PasswordResetRequest(val resetToken: String, val newPassword: String)

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
    val memberId: JsonElement,
    val email: String,
    val nickname: String,
    val status: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long = 1_800L
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class MemberDto(
    val memberId: JsonElement? = null,
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
data class KakaoAuthRequest(
    val authorizationCode: String,
    val redirectUri: String,
    val deviceId: String
)

@Serializable
data class KakaoProfileDto(
    val nickname: String? = null,
    val profileImageUrl: String? = null
)

@Serializable
data class KakaoAuthResponse(
    val isNewMember: Boolean,
    val member: MemberDto? = null,
    val signupToken: String? = null,
    val kakaoProfile: KakaoProfileDto? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val accessExpiresIn: Long? = null
)

@Serializable
data class KakaoSignupRequest(
    val signupToken: String,
    val email: String,
    val name: String,
    val nickname: String,
    val gender: String,
    val birthDate: String,
    val phoneNumber: String,
    val phoneVerificationToken: String,
    val deviceId: String
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
