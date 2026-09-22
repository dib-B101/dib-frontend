package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.auction.AuctionListResponse
import com.ssafy.dib.data.remote.auction.AuctionCommandResponse
import com.ssafy.dib.data.remote.auction.CreateAuctionRequest
import com.ssafy.dib.data.remote.auction.BookmarkResponse
import com.ssafy.dib.data.remote.auction.BidHistoryListResponse
import com.ssafy.dib.data.remote.auction.AuctionBidHistoryListResponse
import com.ssafy.dib.data.remote.auction.AuctionRecommendationResponse
import com.ssafy.dib.data.remote.auction.AuctionBidSnapshotResponse
import com.ssafy.dib.data.remote.auction.SaleHistoryResponse
import com.ssafy.dib.data.repository.toDomain
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class AuctionContractTest {
    @Test
    fun sellerHistoryContractKeepsAuctionProductOrderAndCursor() {
        val response = DibJson.instance.decodeFromString(
            SaleHistoryResponse.serializer(),
            """{"items":[{"auction":{"auctionId":3,"memberId":17,"productId":8,"startPrice":10000,"currentPrice":10000,"auctionTime":7200,"status":"SCHEDULED"},"product":{"productId":8,"title":"필름 카메라","thumbnailUrl":"https://cdn.example/camera.jpg"},"order":{"orderId":21,"status":"PENDING"}}],"nextCursor":"sale-3","hasNext":true}"""
        )

        val sale = response.items.single()
        val auction = sale.auction.copy(product = sale.product).toDomain(Instant.EPOCH)

        assertEquals("3", auction.auctionId)
        assertEquals("필름 카메라", auction.title)
        assertEquals(7_200L, auction.auctionTimeSeconds)
        assertEquals("21", sale.order?.orderId.toString())
        assertEquals("PENDING", sale.order?.status)
        assertEquals("sale-3", response.nextCursor)
        assertTrue(response.hasNext)
    }

    @Test
    fun sellerHistoryKeepsAuctionWithoutOrder() {
        val response = DibJson.instance.decodeFromString(
            SaleHistoryResponse.serializer(),
            """{"items":[{"auction":{"auctionId":4,"productId":9,"startPrice":20000,"currentPrice":20000,"auctionTime":3600,"status":"ACTIVE"},"product":{"productId":9,"title":"진행 중 경매"}}]}"""
        )

        assertEquals("4", response.items.single().auction.auctionId.toString())
        assertEquals(null, response.items.single().order)
    }

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
                    "product":{"name":"빈티지 카메라","categoryName":"디지털기기","description":"필름 테스트를 마친 카메라입니다.","condition":"GOOD","modelName":"FM2","releaseYear":1982,"marketPrice":180000,"thumbnailUrl":"https://cdn.example/thumb.jpg","images":[{"imageUrl":"https://cdn.example/front.jpg"},"https://cdn.example/side.jpg"]},
                    "sellerSummary":{"nickname":"필름상점","rating":4.9,"completedTradeCount":32}
                }],
                "nextCursor":"auction-3",
                "hasNext":true
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
        assertEquals("auction-3", response.nextCursor)
        assertTrue(response.hasNext)
        assertEquals(12_000, auction.myBidAmount)
        assertEquals("필름상점", auction.sellerNickname)
        assertEquals(4.9, auction.sellerRating)
        assertEquals(32, auction.sellerTradeCount)
        assertEquals("필름 테스트를 마친 카메라입니다.", auction.productDescription)
        assertEquals("GOOD", auction.productCondition)
        assertEquals("FM2", auction.productModelName)
        assertEquals(1982, auction.productReleaseYear)
        assertEquals(180_000L, auction.productMarketPrice)
        assertEquals(
            listOf(
                "https://cdn.example/front.jpg",
                "https://cdn.example/side.jpg"
            ),
            auction.imageUrls
        )
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

    @Test
    fun bookmarkCommandResponseKeepsServerState() {
        val selected = DibJson.instance.decodeFromString(BookmarkResponse.serializer(), """{"bookmarked":true}""")
        val removed = DibJson.instance.decodeFromString(BookmarkResponse.serializer(), """{"bookmarked":false}""")

        assertTrue(selected.bookmarked)
        assertEquals(false, removed.bookmarked)
    }

    @Test
    fun bidHistoryContractAcceptsNumericAndStringIds() {
        val response = DibJson.instance.decodeFromString(
            BidHistoryListResponse.serializer(),
            """{"items":[{"bidId":41,"auctionId":"auction-3","amount":58000,"createdAt":"2026-09-13T08:00:00Z"}],"nextCursor":"bid-41","hasNext":true}"""
        )

        assertEquals("41", response.items.single().bidId.toString())
        assertEquals("\"auction-3\"", response.items.single().auctionId.toString())
        assertEquals(58_000L, response.items.single().amount)
        assertEquals("bid-41", response.nextCursor)
        assertTrue(response.hasNext)
    }

    @Test
    fun publicBidHistoryContractUsesMaskedBidderAndCursor() {
        val response = DibJson.instance.decodeFromString(
            AuctionBidHistoryListResponse.serializer(),
            """{"items":[{"bidId":52,"maskedBidderId":"dib***42","amount":61500,"createdAt":"2026-09-13T08:05:00Z"}],"nextCursor":"bid-52","hasNext":true}"""
        )

        assertEquals("dib***42", response.items.single().maskedBidderId)
        assertEquals(61_500L, response.items.single().amount)
        assertEquals("bid-52", response.nextCursor)
        assertTrue(response.hasNext)
    }

    @Test
    fun recommendationContractSeparatesLiveAndGeneralItems() {
        val response = DibJson.instance.decodeFromString(
            AuctionRecommendationResponse.serializer(),
            """{"liveItems":[{"liveBroadcastId":8,"memberId":3,"title":"오늘의 빈티지","status":"LIVE","viewCount":41}],"generalItems":[{"auctionId":12,"memberId":3,"productId":9,"title":"필름 카메라","startPrice":30000,"currentPrice":42000,"status":"ACTIVE"}],"hasNext":false}"""
        )

        assertEquals("오늘의 빈티지", response.liveItems.single().title)
        assertEquals("12", response.generalItems.single().auctionId.toString())
        assertEquals(42_000L, response.generalItems.single().currentPrice)
    }

    @Test
    fun bidSnapshotUsesServerTimeForRemainingSeconds() {
        val response = DibJson.instance.decodeFromString(
            AuctionBidSnapshotResponse.serializer(),
            """{"auctionId":12,"currentPrice":42500,"auctionTime":3600,"startedAt":"2026-09-13T10:00:00Z","scheduledEndAt":"2026-09-13T10:10:30Z","bidCount":9,"bidderCount":4,"isHighestBidder":true,"serverTime":"2026-09-13T10:10:00Z"}"""
        ).toDomain(Instant.EPOCH)

        assertEquals("12", response.auctionId)
        assertEquals(42_500, response.currentPrice)
        assertEquals(30, response.remainingSeconds)
        assertEquals(9, response.bidCount)
        assertTrue(response.isHighestBidder)
    }

    @Test
    fun endedBidSnapshotCannotRestoreRemainingTime() {
        val response = DibJson.instance.decodeFromString(
            AuctionBidSnapshotResponse.serializer(),
            """{"auctionId":12,"status":"ENDED","currentPrice":42500,"auctionTime":3600,"scheduledEndAt":"2026-09-13T10:10:30Z","serverTime":"2026-09-13T10:09:00Z"}"""
        ).toDomain(Instant.EPOCH)

        assertEquals("ENDED", response.status)
        assertEquals(0, response.remainingSeconds)
    }
}
