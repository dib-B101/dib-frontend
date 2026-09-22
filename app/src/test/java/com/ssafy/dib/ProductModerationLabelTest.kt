package com.ssafy.dib

import com.ssafy.dib.data.remote.product.ProductCardDto
import com.ssafy.dib.data.repository.toDomain
import com.ssafy.dib.feature.auction.productModerationStatusLabel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductModerationLabelTest {
    @Test
    fun distinguishesAiAndAdminReviewWithoutChangingUnknownPayloads() {
        assertEquals("AI 검수 중", productModerationStatusLabel("PENDING", "ai", null))
        assertEquals("관리자 검토 중", productModerationStatusLabel("PENDING", "fallback", null))
        assertEquals("관리자 검토 중", productModerationStatusLabel("PENDING", "ai", "2026-09-22T01:00:00Z"))
        assertEquals("검수 중", productModerationStatusLabel("PENDING", null, null))
    }

    @Test
    fun mapsModerationFieldsFromMyProductList() {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString(
            ProductCardDto.serializer(),
            """{"productId":12,"title":"필름 카메라","status":"PENDING","moderationStage":"fallback","moderatedAt":"2026-09-22T01:00:00Z"}"""
        )

        val product = dto.toDomain()

        assertEquals("fallback", product.moderationStage)
        assertEquals("2026-09-22T01:00:00Z", product.moderatedAt)
    }
}
