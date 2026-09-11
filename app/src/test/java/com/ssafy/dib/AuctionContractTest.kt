package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auction.AuctionListResponse
import com.ssafy.dib.data.repository.toDomain
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionContractTest {
    @Test
    fun numericIdsAndNestedProductMapToHomeSummary() {
        val response = DibJson.instance.decodeFromString(
            AuctionListResponse.serializer(),
            """{
                "items":[{
                    "auctionId":3,
                    "productId":8,
                    "startPrice":10000,
                    "currentPrice":12500,
                    "bidCount":4,
                    "scheduledEndAt":"2026-09-11T06:10:00Z",
                    "serverTime":"2026-09-11T06:00:00Z",
                    "status":"ACTIVE",
                    "bookmarked":true,
                    "myBid":{"amount":12000,"isHighestBidder":false},
                    "product":{"name":"빈티지 카메라","categoryName":"디지털기기"}
                }],
                "hasNext":false
            }""".trimIndent()
        )

        val auction = response.items.single().toDomain(Instant.EPOCH)

        assertEquals("3", auction.auctionId)
        assertEquals("8", auction.productId)
        assertEquals("빈티지 카메라", auction.title)
        assertEquals(12_500, auction.currentPrice)
        assertEquals(600, auction.remainingSeconds)
        assertTrue(auction.bookmarked)
        assertEquals(12_000, auction.myBidAmount)
    }
}
