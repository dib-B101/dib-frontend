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
    val score: Double?,   // 받은 별점 평균(0~5). 평가가 없으면 null
    val profileImageUrl: String? = null
)

data class MemberProfileUpdate(val memberId: String, val nickname: String, val updatedAt: String, val profileImageUrl: String?)
data class MemberImageUpload(val fileName: String, val mediaType: String, val bytes: ByteArray)
data class MemberWithdrawal(val requestedAt: String, val scheduledAt: String, val status: String)

interface MemberRepository {
    fun getMe(): ApiResult<MemberProfile>
    fun updateNickname(nickname: String): ApiResult<MemberProfileUpdate>
    fun updateProfileImage(nickname: String?, image: MemberImageUpload): ApiResult<MemberProfileUpdate>
    fun requestWithdrawal(reason: String? = null): ApiResult<MemberWithdrawal>
}
