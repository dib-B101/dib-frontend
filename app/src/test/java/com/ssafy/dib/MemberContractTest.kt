package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.member.MemberProfileResponse
import com.ssafy.dib.data.remote.member.MemberProfileUpdateRequest
import com.ssafy.dib.data.remote.member.MemberWithdrawalRequest
import com.ssafy.dib.data.remote.member.MemberWithdrawalResponse
import com.ssafy.dib.data.repository.toDomain
import com.ssafy.dib.feature.main.withdrawalTimeLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class MemberContractTest {
    @Test
    fun mapsMyProfileWithNumericMemberId() {
        val response = DibJson.instance.decodeFromString(
            MemberProfileResponse.serializer(),
            """{"memberId":17,"email":"dib@example.com","name":"김띱","phoneNumber":"01012345678","nickname":"dib러버","gender":"FEMALE","birthDate":"2000-01-01","status":"ACTIVE","role":"USER","score":4.5}"""
        )

        val profile = response.toDomain()

        assertEquals("17", profile.memberId)
        assertEquals("dib@example.com", profile.email)
        assertEquals("dib러버", profile.nickname)
        assertEquals(4.5, profile.score!!, 0.0)
    }

    // 후기를 못 받은 회원은 서버가 score: null 을 준다. 여기서 터지면 마이 탭 프로필 전체가 안 뜬다
    @Test
    fun mapsMyProfileWithoutScore() {
        val response = DibJson.instance.decodeFromString(
            MemberProfileResponse.serializer(),
            """{"memberId":17,"email":"dib@example.com","name":"김띱","phoneNumber":"01012345678","nickname":"dib러버","gender":"FEMALE","birthDate":"2000-01-01","status":"ACTIVE","role":"USER","score":null}"""
        )

        val profile = response.toDomain()

        assertEquals(null, profile.score)
    }

    @Test
    fun nicknameUpdatePayloadContainsOnlyNickname() {
        val encoded = DibJson.instance.encodeToString(
            MemberProfileUpdateRequest.serializer(),
            MemberProfileUpdateRequest("새닉네임")
        )

        assertEquals("{\"nickname\":\"새닉네임\"}", encoded)
    }

    @Test
    fun withdrawalRequestOmitsEmptyOptionalReason() {
        val encoded = DibJson.instance.encodeToString(
            MemberWithdrawalRequest.serializer(),
            MemberWithdrawalRequest()
        )
        val response = DibJson.instance.decodeFromString(
            MemberWithdrawalResponse.serializer(),
            """{"requestedAt":"2026-09-13T09:00:00Z","scheduledAt":"2026-09-20T09:00:00Z","status":"WITHDRAWN"}"""
        )

        assertEquals("{}", encoded)
        assertEquals("WITHDRAWN", response.status)
    }

    @Test
    fun withdrawalScheduleUsesTheDisplayedLocalTimeZone() {
        assertEquals(
            "2026년 9월 20일 18:00",
            withdrawalTimeLabel("2026-09-20T09:00:00Z", ZoneId.of("Asia/Seoul"))
        )
    }
}
