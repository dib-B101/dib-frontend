package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.DibJson
import org.junit.Assert.assertEquals
import org.junit.Test

class AuctionPayloadCompatibilityTest {
    @Test
    fun `backend array auction payload is accepted`() {
        val payload = DibJson.instance.parseToJsonElement(
            """[{"auctionId":1,"productId":3,"startPrice":1000,"currentPrice":2000,"status":"ACTIVE"}]"""
        )

        val result = decodeAuctionList(payload)

        assertEquals(1, result.items.size)
        assertEquals("ACTIVE", result.items.single().status)
    }

    @Test
    fun `backend recommendation array becomes general recommendations`() {
        val payload = DibJson.instance.parseToJsonElement(
            """[{"auctionId":1,"productId":3,"startPrice":1000,"currentPrice":2000,"status":"ACTIVE"}]"""
        )

        val result = decodeAuctionRecommendations(payload)

        assertEquals(0, result.liveItems.size)
        assertEquals(1, result.generalItems.size)
    }

    @Test
    fun `target page contract remains accepted`() {
        val payload = DibJson.instance.parseToJsonElement(
            """{"items":[{"auctionId":1,"startPrice":1000,"currentPrice":2000,"status":"ACTIVE"}],"nextCursor":"next","hasNext":true}"""
        )

        val result = decodeAuctionList(payload)

        assertEquals("next", result.nextCursor)
        assertEquals(true, result.hasNext)
    }

    @Test
    fun `backend bookmark array keeps product id`() {
        val payload = DibJson.instance.parseToJsonElement(
            """[{"bookmarkId":9,"memberId":2,"productId":3,"createdAt":"2026-09-17T10:00:00"}]"""
        )

        val result = decodeBookmarkList(payload)

        assertEquals("3", result.items.single().productId.toString())
    }
}
