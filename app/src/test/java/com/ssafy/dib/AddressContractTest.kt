package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.member.AddressListResponse
import com.ssafy.dib.data.remote.member.CreateAddressRequest
import com.ssafy.dib.data.remote.member.UpdateAddressRequest
import com.ssafy.dib.data.repository.toDomain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressContractTest {
    @Test
    fun addressResponseUsesOnlyPersistedSchemaFields() {
        val response = DibJson.instance.decodeFromString(
            AddressListResponse.serializer(),
            """{"items":[{"addressId":4,"number":"06236","address":"서울특별시 강남구 테헤란로 212","name":"회사","apiAddressId":"road-8821"}]}"""
        ).items.single().toDomain()

        assertEquals("4", response.addressId)
        assertEquals("06236", response.postalCode)
        assertEquals("road-8821", response.apiAddressId)
    }

    @Test
    fun updatePreservesAddressApiIdentifier() {
        val encoded = DibJson.instance.encodeToString(
            UpdateAddressRequest.serializer(),
            UpdateAddressRequest("06236", "서울특별시 강남구 테헤란로 212", "회사", JsonPrimitive("road-8821"))
        )

        assertTrue(encoded.contains("\"apiAddressId\":\"road-8821\""))
        assertFalse(encoded.contains("recipient"))
        assertFalse(encoded.contains("isDefault"))
    }

    @Test
    fun createUsesOnlyAddressSearchResultFields() {
        val encoded = DibJson.instance.encodeToString(
            CreateAddressRequest.serializer(),
            CreateAddressRequest("06236", "서울특별시 강남구 테헤란로 212", "회사", JsonPrimitive("road-8821"))
        )

        assertTrue(encoded.contains("\"number\":\"06236\""))
        assertTrue(encoded.contains("\"apiAddressId\":\"road-8821\""))
        assertFalse(encoded.contains("recipient"))
        assertFalse(encoded.contains("phoneNumber"))
        assertFalse(encoded.contains("isDefault"))
    }
}
