package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auth.PasswordResetLinkRequest
import com.ssafy.dib.data.remote.auth.PasswordResetRequest
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
import com.ssafy.dib.data.remote.auth.PhoneVerificationRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordResetContractTest {
    @Test
    fun resetLinkUsesDedicatedVerificationPurposeAndToken() {
        val verification = DibJson.instance.encodeToString(
            PhoneVerificationRequest.serializer(),
            PhoneVerificationRequest("01012345678", PhoneVerificationPurpose.RESET_PASSWORD)
        )
        val request = DibJson.instance.encodeToString(
            PasswordResetLinkRequest.serializer(),
            PasswordResetLinkRequest("member@example.com", "verified-phone-token")
        )

        assertTrue(verification.contains("\"purpose\":\"RESET_PASSWORD\""))
        assertTrue(request.contains("\"email\":\"member@example.com\""))
        assertTrue(request.contains("\"phoneVerificationToken\":\"verified-phone-token\""))
    }

    @Test
    fun passwordResetUsesTokenAndNewPassword() {
        val request = DibJson.instance.encodeToString(
            PasswordResetRequest.serializer(),
            PasswordResetRequest("reset-token", "NewPassword1!")
        )

        assertTrue(request.contains("\"resetToken\":\"reset-token\""))
        assertTrue(request.contains("\"newPassword\":\"NewPassword1!\""))
    }
}
