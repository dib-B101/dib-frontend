package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.LiveSocketEventParser
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import java.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSocketEventParserTest {
    private val parser = LiveSocketEventParser(now = { Instant.parse("2026-09-14T09:00:00Z") })

    @Test
    fun `snapshot restores live and active auction from server time`() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_SNAPSHOT,
                payload = buildJsonObject {
                    put("liveBroadcast", buildJsonObject {
                        put("liveBroadcastId", 9)
                        put("title", "빈티지 카메라 경매")
                        put("streamUrl", "https://stream.example/live.m3u8")
                        put("viewCount", 21)
                    })
                    put("activeAuction", buildJsonObject {
                        put("auctionId", 31)
                        put("startPrice", 30_000)
                        put("currentPrice", 42_000)
                        put("bidCount", 7)
                        put("status", "ACTIVE")
                        put("myBid", buildJsonObject { put("isHighestBidder", true) })
                        put("endedAt", "2026-09-14T09:01:00Z")
                        put("product", buildJsonObject {
                            put("productId", 12)
                            put("title", "필름 카메라")
                            put("thumbnailUrl", "https://image.example/camera.jpg")
                        })
                    })
                    put("serverTime", "2026-09-14T09:00:30Z")
                }
            )
        )!!

        assertEquals("9", update.liveBroadcastId)
        assertEquals("31", update.auctionId)
        assertEquals("12", update.productId)
        assertEquals("빈티지 카메라 경매", update.liveTitle)
        assertEquals("필름 카메라", update.title)
        assertEquals(42_000, update.currentPrice)
        assertEquals(30, update.remainingSeconds)
        assertEquals(21, update.viewerCount)
        assertEquals(true, update.isHighestBidder)
        assertEquals("https://stream.example/live.m3u8", update.streamUrl)
    }

    @Test
    fun `live started updates stream metadata`() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_STARTED,
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-1")
                    put("title", "오늘의 경매")
                    put("streamUrl", "https://stream.example/live-1.m3u8")
                    put("startedAt", "2026-09-14T09:00:00Z")
                }
            )
        )!!

        assertEquals("live-1", update.liveBroadcastId)
        assertEquals("LIVE", update.status)
        assertEquals("오늘의 경매", update.liveTitle)
        assertEquals("https://stream.example/live-1.m3u8", update.streamUrl)
    }

    @Test
    fun `live ended identifies the broadcast to remove from feed`() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_ENDED,
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-1")
                    put("viewCount", 57)
                    put("endedAt", "2026-09-14T09:30:00Z")
                }
            )
        )!!

        assertEquals("live-1", update.liveBroadcastId)
        assertEquals("ENDED", update.status)
        assertEquals(57, update.viewerCount)
        assertEquals("Live 방송이 종료됐어요.", update.message)
    }

    @Test
    fun `closed live auction maps sale result to ended status`() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_AUCTION_CLOSED,
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-1")
                    put("auctionId", 31)
                    put("result", "SOLD")
                    put("finalPrice", 52_000)
                    put("endedAt", "2026-09-14T09:10:00Z")
                }
            )
        )!!

        assertEquals("31", update.auctionId)
        assertEquals("ENDED", update.status)
        assertEquals(0, update.remainingSeconds)
        assertEquals(52_000, update.currentPrice)
        assertEquals("Live 경매가 낙찰됐어요.", update.message)

        val cancelled = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_AUCTION_CLOSED,
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-1")
                    put("auctionId", 32)
                    put("result", "CANCELLED")
                }
            )
        )!!
        assertEquals("CANCELLED", cancelled.status)
    }
}
