package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.member.MemberProfileResponse
import com.ssafy.dib.data.remote.member.MemberProfileUpdateRequest
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberContractTest {
    @Test
    fun mapsMyProfileWithNumericMemberId() {
        val response = DibJson.instance.decodeFromString(
            MemberProfileResponse.serializer(),
            """{"memberId":17,"email":"dib@example.com","name":"김띱","phoneNumber":"01012345678","nickname":"dib러버","gender":"FEMALE","birthDate":"2000-01-01","status":"ACTIVE","role":"USER","score":100}"""
        )

        val profile = response.toDomain()

        assertEquals("17", profile.memberId)
        assertEquals("dib@example.com", profile.email)
        assertEquals("dib러버", profile.nickname)
        assertEquals(100, profile.score)
    }

    @Test
    fun nicknameUpdatePayloadContainsOnlyNickname() {
        val encoded = DibJson.instance.encodeToString(
            MemberProfileUpdateRequest.serializer(),
            MemberProfileUpdateRequest("새닉네임")
        )

        assertEquals("{\"nickname\":\"새닉네임\"}", encoded)
    }
}
