package com.ssafy.dib.domain.member

import com.ssafy.dib.core.network.ApiResult

data class MemberProfile(
    val memberId: String,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val nickname: String,
    val gender: String,
    val birthDate: String,
    val status: String,
    val role: String,
    val score: Double
)

data class MemberProfileUpdate(val memberId: String, val nickname: String, val updatedAt: String)
data class MemberWithdrawal(val requestedAt: String, val scheduledAt: String, val status: String)

interface MemberRepository {
    fun getMe(): ApiResult<MemberProfile>
    fun updateNickname(nickname: String): ApiResult<MemberProfileUpdate>
    fun requestWithdrawal(reason: String? = null): ApiResult<MemberWithdrawal>
}
