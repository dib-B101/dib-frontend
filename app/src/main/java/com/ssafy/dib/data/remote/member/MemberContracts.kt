package com.ssafy.dib.data.remote.member

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MemberProfileResponse(
    val memberId: JsonElement,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val nickname: String,
    val gender: String,
    val birthDate: String,
    val status: String,
    val role: String,
    // 받은 별점 평균(0~5). 후기가 없으면 서버가 null 을 준다 — 0.0 으로 받으면 "0점" 이라는 가짜 숫자가 된다
    val score: Double? = null,
    val profileImageUrl: String? = null
)

@Serializable data class MemberProfileUpdateRequest(val nickname: String)

@Serializable
data class MemberProfileUpdateResponse(
    val memberId: JsonElement,
    val nickname: String,
    val profileImageUrl: String? = null,
    val updatedAt: String
)

@Serializable data class MemberWithdrawalRequest(val reason: String? = null)

@Serializable
data class MemberWithdrawalResponse(
    val requestedAt: String,
    val scheduledAt: String,
    val status: String
)
