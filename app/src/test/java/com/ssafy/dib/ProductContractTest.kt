package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.product.CategoryListResponse
import com.ssafy.dib.data.remote.product.ProductCreatePayload
import com.ssafy.dib.data.repository.toDomain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductContractTest {
    @Test
    fun mapsCategoryIdentifiers() {
        val response = DibJson.instance.decodeFromString(
            CategoryListResponse.serializer(),
            """{"items":[{"categoryId":3,"name":"디지털"}]}"""
        )

        assertEquals("3", response.items.single().toDomain().categoryId)
    }

    @Test
    fun productPayloadKeepsNumericCategoryId() {
        val payload = ProductCreatePayload(
            title = "필름 카메라",
            description = "정상 작동합니다.",
            categoryId = JsonPrimitive(3),
            condition = "GOOD"
        )

        val encoded = DibJson.instance.encodeToString(ProductCreatePayload.serializer(), payload)

        assertTrue(encoded.contains("\"categoryId\":3"))
        assertTrue(encoded.contains("\"condition\":\"GOOD\""))
    }
}
