package com.ssafy.dib.feature.main

import com.ssafy.dib.domain.member.MemberAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedShippingAddressTest {
    @Test
    fun fillsAllFieldsFromStructuredSavedAddress() {
        val saved = MemberAddress("1", "06236", "서울 강남구 테헤란로 123", "집", "building-1",
            "101동 1001호", "김구매", "01012345678")

        assertEquals(SavedShippingFields("06236", "서울 강남구 테헤란로 123", "101동 1001호",
            "김구매", "01012345678"), saved.toShippingFields("회원 이름", "01099998888"))
    }

    @Test
    fun splitsLegacyDetailAndUsesMemberContact() {
        val saved = MemberAddress("1", "06236", "서울 강남구 테헤란로 123 101동 1001호", "집", "building-1")

        assertEquals(SavedShippingFields("06236", "서울 강남구 테헤란로 123", "101동 1001호",
            "회원 이름", "01099998888"), saved.toShippingFields("회원 이름", "01099998888"))
    }

    @Test
    fun preservesAmbiguousLegacyAddressWithoutDroppingText() {
        val saved = MemberAddress("1", "12345", "서울 강남구 임의 주소 101호", "집", "building-1")

        assertEquals("서울 강남구 임의 주소 101호", saved.toShippingFields().address)
    }
}
