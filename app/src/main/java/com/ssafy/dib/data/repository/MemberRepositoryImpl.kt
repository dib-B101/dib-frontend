package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.member.MemberProfileResponse
import com.ssafy.dib.data.remote.member.MemberProfileUpdateResponse
import com.ssafy.dib.data.remote.member.MemberRemoteDataSource
import com.ssafy.dib.domain.member.MemberProfile
import com.ssafy.dib.domain.member.MemberProfileUpdate
import com.ssafy.dib.domain.member.MemberRepository
import com.ssafy.dib.domain.member.MemberWithdrawal
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class MemberRepositoryImpl(private val remote: MemberRemoteDataSource) : MemberRepository {
    override fun getMe(): ApiResult<MemberProfile> = when (val result = remote.getMe()) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }

    override fun updateNickname(nickname: String): ApiResult<MemberProfileUpdate> = when (val result = remote.updateNickname(nickname)) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }

    override fun requestWithdrawal(reason: String?): ApiResult<MemberWithdrawal> = when (val result = remote.requestWithdrawal(reason)) {
        is ApiResult.Success -> ApiResult.Success(
            MemberWithdrawal(result.value.requestedAt, result.value.scheduledAt, result.value.status),
            result.status
        )
        is ApiResult.Failure -> result
    }
}

internal fun MemberProfileResponse.toDomain() = MemberProfile(
    memberId = memberId.idValue(), email = email, name = name, phoneNumber = phoneNumber,
    nickname = nickname, gender = gender, birthDate = birthDate, status = status, role = role, score = score
)

internal fun MemberProfileUpdateResponse.toDomain() = MemberProfileUpdate(memberId.idValue(), nickname, updatedAt)

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
