package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auth.LoginResponse
import com.ssafy.dib.data.remote.auth.SignUpRequest
import com.ssafy.dib.data.remote.auth.SignUpResponse
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthContractTest {
    @Test
    fun signupRequestIncludesBackendRequiredDeviceId() {
        val request = SignUpRequest(
            email = "dib@example.com",
            password = "password",
            name = "디비",
            nickname = "디비",
            gender = "FEMALE",
            birthDate = "2000-01-01",
            phoneNumber = "01012345678",
            phoneVerificationToken = "verified",
            deviceId = "android-device"
        )

        val encoded = DibJson.instance.encodeToString(SignUpRequest.serializer(), request)

        org.junit.Assert.assertTrue(encoded.contains("\"deviceId\":\"android-device\""))
    }

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
}
