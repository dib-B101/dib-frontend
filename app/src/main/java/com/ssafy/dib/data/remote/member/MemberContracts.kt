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
    val score: Int = 0
)

@Serializable data class MemberProfileUpdateRequest(val nickname: String)

@Serializable
data class MemberProfileUpdateResponse(
    val memberId: JsonElement,
    val nickname: String,
    val updatedAt: String
)
