package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.product.CategoryListResponse
import com.ssafy.dib.data.remote.product.ProductCreatePayload
import com.ssafy.dib.data.remote.product.ProductDetailResponse
import com.ssafy.dib.data.remote.product.ProductListResponse
import com.ssafy.dib.data.remote.product.ProductUpdatePayload
import com.ssafy.dib.data.remote.product.decodeProductDetail
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
            condition = "GOOD",
            modelName = "FM2",
            releaseYear = 1982,
            marketPrice = 120_000,
            startPrice = 30_000,
            auctionTime = 300
        )

        val encoded = DibJson.instance.encodeToString(ProductCreatePayload.serializer(), payload)

        assertTrue(encoded.contains("\"categoryId\":3"))
        assertTrue(encoded.contains("\"condition\":\"GOOD\""))
        assertTrue(encoded.contains("\"modelName\":\"FM2\""))
        assertTrue(encoded.contains("\"releaseYear\":1982"))
        assertTrue(encoded.contains("\"marketPrice\":120000"))
        assertTrue(encoded.contains("\"startPrice\":30000"))
        assertTrue(encoded.contains("\"auctionTime\":300"))
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

    @Test
    fun productDetailAcceptsFlatBackendResponse() {
        val response = decodeProductDetail(
            DibJson.instance.parseToJsonElement(
                """{"productId":8,"memberId":17,"categoryId":3,"title":"빈티지 카메라","description":"정상 작동","condition":"GOOD","modelName":"FM2","releaseYear":1982,"marketPrice":120000,"thumbnailUrl":"https://cdn.example/thumb.jpg","status":"REGISTERED","nickname":"필름상점"}"""
            )
        )

        val product = response.toDomain()

        assertEquals("8", product.productId)
        assertEquals("17", product.memberId)
        assertEquals("필름상점", product.sellerNickname)
    }

    @Test
    fun myProductCardMapsModerationState() {
        val response = DibJson.instance.decodeFromString(
            ProductListResponse.serializer(),
            """{"items":[{"productId":12,"title":"달빛 유약 머그컵","condition":"GOOD","status":"REGISTERED","thumbnailUrl":"https://cdn.example/mug.jpg","auctionId":31,"startPrice":30000,"auctionTime":300,"auctionStatus":"SCHEDULED"}],"nextCursor":"product-12","hasNext":true}"""
        )

        val product = response.items.single().toDomain()

        assertEquals("12", product.productId)
        assertEquals("달빛 유약 머그컵", product.title)
        assertEquals("REGISTERED", product.status)
        assertEquals("https://cdn.example/mug.jpg", product.thumbnailUrl)
        assertEquals("31", product.auctionId)
        assertEquals(30_000L, product.startPrice)
        assertEquals(300L, product.auctionTimeSeconds)
        assertEquals("SCHEDULED", product.auctionStatus)
        assertEquals("product-12", response.nextCursor)
        assertTrue(response.hasNext)
    }

    @Test
    fun myProductCardUsesBackendProductStatusField() {
        val response = DibJson.instance.decodeFromString(
            ProductListResponse.serializer(),
            """{"items":[{"productId":12,"title":"달빛 유약 머그컵","condition":"GOOD","productStatus":"APPROVED"}]}"""
        )

        assertEquals("APPROVED", response.items.single().toDomain().status)
    }

    @Test
    fun similarProductResponseAllowsCursorFieldsToBeOmitted() {
        val response = DibJson.instance.decodeFromString(
            ProductListResponse.serializer(),
            """{"items":[{"productId":21,"title":"필름 렌즈","condition":"LIKE_NEW","status":"REGISTERED","thumbnailUrl":"https://cdn.example/lens.jpg"}]}"""
        )

        val product = response.items.single().toDomain()

        assertEquals("21", product.productId)
        assertEquals("필름 렌즈", product.title)
        assertEquals("LIKE_NEW", product.condition)
        assertEquals(null, response.nextCursor)
        assertTrue(!response.hasNext)
    }

    @Test
    fun productUpdatePayloadKeepsNumericFieldsAndOmitsNulls() {
        val payload = ProductUpdatePayload(
            title = "필름 카메라",
            description = "정상 작동",
            categoryId = JsonPrimitive(3),
            condition = "GOOD",
            modelName = null,
            releaseYear = 1982,
            marketPrice = 120_000
        )

        val encoded = DibJson.instance.encodeToString(ProductUpdatePayload.serializer(), payload)

        assertTrue(encoded.contains("\"categoryId\":3"))
        assertTrue(encoded.contains("\"releaseYear\":1982"))
        assertTrue(!encoded.contains("modelName"))
    }

}
