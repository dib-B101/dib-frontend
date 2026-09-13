package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auth.MaskedEmailResponse
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
import com.ssafy.dib.data.remote.auth.PhoneVerificationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindEmailContractTest {
    @Test
    fun emailLookupUsesDedicatedPhoneVerificationPurpose() {
        val encoded = DibJson.instance.encodeToString(
            PhoneVerificationRequest.serializer(),
            PhoneVerificationRequest("01012345678", PhoneVerificationPurpose.FIND_EMAIL)
        )

        assertTrue(encoded.contains("\"purpose\":\"FIND_EMAIL\""))
    }

    @Test
    fun lookupResponseExposesOnlyMaskedEmail() {
        val response = DibJson.instance.decodeFromString(
            MaskedEmailResponse.serializer(),
            """{"maskedEmail":"di***@example.com"}"""
        )

        assertEquals("di***@example.com", response.maskedEmail)
        assertFalse(response.maskedEmail.startsWith("dib-user"))
    }
}
