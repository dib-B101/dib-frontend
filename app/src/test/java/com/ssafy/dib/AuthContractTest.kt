package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auth.LoginResponse
import com.ssafy.dib.data.remote.auth.SignUpResponse
import com.ssafy.dib.data.remote.auth.KakaoAuthResponse
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthContractTest {
    @Test
    fun signupAcceptsNumericMemberId() {
        val response = DibJson.instance.decodeFromString(
            SignUpResponse.serializer(),
            """{"memberId":17,"email":"dib@example.com","nickname":"디비","status":"ACTIVE","role":"USER","accessToken":"access","refreshToken":"refresh"}"""
        )

        assertEquals("17", (response.memberId as JsonPrimitive).content)
    }

    @Test
    fun loginAcceptsNumericMemberIdInsideMember() {
        val response = DibJson.instance.decodeFromString(
            LoginResponse.serializer(),
            """{"member":{"memberId":21,"email":"dib@example.com","nickname":"디비","status":"ACTIVE","role":"USER"},"accessToken":"access","refreshToken":"refresh","accessExpiresIn":1800}"""
        )

        assertEquals("21", (response.member.memberId as JsonPrimitive).content)
    }

    @Test
    fun loginStillAcceptsStringMemberId() {
        val response = DibJson.instance.decodeFromString(
            LoginResponse.serializer(),
            """{"member":{"memberId":"member-21","email":"dib@example.com"},"accessToken":"access","refreshToken":"refresh","accessExpiresIn":1800}"""
        )

        assertEquals("member-21", (response.member.memberId as JsonPrimitive).content)
    }

    @Test
    fun kakaoNewMemberResponseAllowsNullableSessionFields() {
        val response = DibJson.instance.decodeFromString(
            KakaoAuthResponse.serializer(),
            """{"isNewMember":true,"signupToken":"signup-token","kakaoProfile":{"nickname":"카카오닉네임","profileImageUrl":null},"member":null,"accessToken":null,"refreshToken":null,"accessExpiresIn":null}"""
        )

        assertEquals(true, response.isNewMember)
        assertEquals("signup-token", response.signupToken)
        assertEquals("카카오닉네임", response.kakaoProfile?.nickname)
    }
}
