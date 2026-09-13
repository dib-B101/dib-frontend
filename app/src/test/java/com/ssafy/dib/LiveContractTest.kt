package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.live.LiveFeedResponse
import com.ssafy.dib.data.remote.live.LiveChatMessageListResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiveContractTest {
    @Test
    fun decodesLiveFeedWithCurrentAuctionAndProduct() {
        val response = DibJson.instance.decodeFromString(
            LiveFeedResponse.serializer(),
            """{"items":[{"liveBroadcast":{"liveBroadcastId":5,"memberId":17,"title":"도자기 경매","status":"LIVE","streamUrl":"https://stream.example/live.m3u8","viewCount":248},"activeAuction":{"auctionId":31,"productId":8,"startPrice":10000,"currentPrice":12500,"bidCount":4,"auctionTime":300,"status":"ACTIVE"},"product":{"productId":8,"title":"달빛 유약 머그컵","thumbnailUrl":"https://cdn.example/mug.jpg"}}],"hasNext":false,"serverTime":"2026-09-13T08:00:00Z"}"""
        )

        val item = response.items.single()
        assertEquals("5", item.liveBroadcast.liveBroadcastId.toString())
        assertEquals("도자기 경매", item.liveBroadcast.title)
        assertNotNull(item.activeAuction)
        assertEquals("달빛 유약 머그컵", item.product?.title)
    }

    @Test
    fun decodesLiveChatHistoryWithNumericIds() {
        val response = DibJson.instance.decodeFromString(
            LiveChatMessageListResponse.serializer(),
            """{"items":[{"liveChattingId":81,"memberId":17,"content":"다음 상품 궁금해요","time":"2026-09-13T08:00:00Z"}],"hasMore":false}"""
        )

        assertEquals("81", response.items.single().liveChattingId.toString())
        assertEquals("17", response.items.single().memberId.toString())
        assertEquals("다음 상품 궁금해요", response.items.single().content)
    }
}
