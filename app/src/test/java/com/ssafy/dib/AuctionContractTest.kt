package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auction.AuctionListResponse
import com.ssafy.dib.data.remote.auction.BidDepositResponse
import com.ssafy.dib.data.remote.auction.AuctionCommandResponse
import com.ssafy.dib.data.remote.auction.CreateAuctionRequest
import com.ssafy.dib.data.repository.toDomain
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class AuctionContractTest {
    @Test
    fun numericIdsAndNestedProductMapToHomeSummary() {
        val response = DibJson.instance.decodeFromString(
            AuctionListResponse.serializer(),
            """{
                "items":[{
                    "auctionId":3,
                    "memberId":17,
                    "productId":8,
                    "startPrice":10000,
                    "currentPrice":12500,
                    "bidCount":4,
                    "scheduledEndAt":"2026-09-11T06:10:00Z",
                    "serverTime":"2026-09-11T06:00:00Z",
                    "status":"ACTIVE",
                    "bookmarked":true,
                    "myBid":{"amount":12000,"isHighestBidder":false},
                    "product":{"name":"빈티지 카메라","categoryName":"디지털기기","thumbnailUrl":"https://cdn.example/thumb.jpg","images":[{"imageUrl":"https://cdn.example/front.jpg"},"https://cdn.example/side.jpg"]}
                }],
                "hasNext":false
            }""".trimIndent()
        )

        val auction = response.items.single().toDomain(Instant.EPOCH)

        assertEquals("3", auction.auctionId)
        assertEquals("8", auction.productId)
        assertEquals("17", auction.sellerMemberId)
        assertEquals("빈티지 카메라", auction.title)
        assertEquals(12_500, auction.currentPrice)
        assertEquals(600, auction.remainingSeconds)
        assertTrue(auction.bookmarked)
        assertEquals(12_000, auction.myBidAmount)
        assertEquals(
            listOf(
                "https://cdn.example/front.jpg",
                "https://cdn.example/side.jpg"
            ),
            auction.imageUrls
        )
    }

    @Test
    fun depositPreparationMapsNestedPaymentUrlAndCalculatedAmount() {
        val response = DibJson.instance.decodeFromString(
            BidDepositResponse.serializer(),
            """{"bidDepositId":41,"auctionId":3,"amount":5200,"status":"PENDING","paymentRequest":{"checkoutUrl":"https://pay.example/checkout/41"}}"""
        )

        val deposit = response.toDomain()

        assertEquals("41", deposit.bidDepositId)
        assertEquals("3", deposit.auctionId)
        assertEquals(5_200L, deposit.amount)
        assertEquals("PENDING", deposit.status)
        assertEquals("https://pay.example/checkout/41", deposit.paymentUrl)
    }

    @Test
    fun auctionCreateContractKeepsNumericProductId() {
        val request = CreateAuctionRequest(JsonPrimitive(12), 10_000, 3_600)
        val encoded = DibJson.instance.encodeToString(CreateAuctionRequest.serializer(), request)
        val response = DibJson.instance.decodeFromString(AuctionCommandResponse.serializer(), """{"auctionId":3,"message":"경매 생성 성공"}""")

        assertTrue(encoded.contains("\"productId\":12"))
        assertEquals("3", response.auctionId.toString())
        assertEquals("경매 생성 성공", response.message)
    }
}
