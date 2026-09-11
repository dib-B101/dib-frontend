package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.product.CategoryListResponse
import com.ssafy.dib.data.remote.product.ProductCreatePayload
import com.ssafy.dib.data.remote.product.ProductDetailResponse
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

    @Test
    fun productDetailMapsImagesAndSellerSummary() {
        val response = DibJson.instance.decodeFromString(
            ProductDetailResponse.serializer(),
            """{
                "product":{"productId":8,"memberId":17,"categoryId":3,"title":"빈티지 카메라","description":"정상 작동","condition":"GOOD","modelName":"FM2","releaseYear":1982,"marketPrice":120000,"thumbnailUrl":"https://cdn.example/thumb.jpg","status":"REGISTERED","images":[{"imageUrl":"https://cdn.example/front.jpg"},"https://cdn.example/back.jpg"]},
                "sellerSummary":{"nickname":"필름상점","rating":4.9,"completedTradeCount":32}
            }""".trimIndent()
        )

        val product = response.toDomain()

        assertEquals("8", product.productId)
        assertEquals("17", product.memberId)
        assertEquals("FM2", product.modelName)
        assertEquals(listOf("https://cdn.example/front.jpg", "https://cdn.example/back.jpg"), product.imageUrls)
        assertEquals("필름상점", product.sellerNickname)
        assertEquals(32, product.sellerTradeCount)
    }
}
