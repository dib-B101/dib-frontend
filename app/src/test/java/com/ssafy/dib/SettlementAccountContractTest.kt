package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auth.PhoneVerificationPurpose
import com.ssafy.dib.data.remote.auth.PhoneVerificationRequest
import com.ssafy.dib.data.remote.settlement.SaveSettlementAccountRequest
import com.ssafy.dib.data.remote.settlement.SettlementAccountResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementAccountContractTest {
    @Test
    fun sensitiveVerificationUsesDocumentedPurpose() {
        val encoded = DibJson.instance.encodeToString(
            PhoneVerificationRequest.serializer(),
            PhoneVerificationRequest("01012345678", PhoneVerificationPurpose.CHANGE_SENSITIVE)
        )

        assertTrue(encoded.contains("CHANGE_SENSITIVE"))
    }

    // 정산 계좌 저장은 휴대폰 재인증 토큰을 보내지 않는다 (가입 때 이미 본인인증을 마친 계정)
    @Test
    fun settlementAccountRequestCarriesNoVerificationToken() {
        val encoded = DibJson.instance.encodeToString(
            SaveSettlementAccountRequest.serializer(),
            SaveSettlementAccountRequest("우리은행", "100212345678", "김띱")
        )

        assertTrue(!encoded.contains("phoneVerificationToken"))
    }

    @Test
    fun settlementAccountContractsUseUnmaskedInputAndMaskedOutput() {
        val request = SaveSettlementAccountRequest("우리은행", "100212345678", "김띱")
        val encoded = DibJson.instance.encodeToString(SaveSettlementAccountRequest.serializer(), request)
        val response = DibJson.instance.decodeFromString(
            SettlementAccountResponse.serializer(),
            """{"bankName":"우리은행","maskedAccountNumber":"1002-***-5678","accountHolder":"김띱"}"""
        )

        assertTrue(encoded.contains("100212345678"))
        assertEquals("1002-***-5678", response.maskedAccountNumber)
        assertEquals("김띱", response.accountHolder)
    }
}
