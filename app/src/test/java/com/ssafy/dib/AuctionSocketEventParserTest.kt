package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.AuctionSocketEventParser
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import java.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionSocketEventParserTest {
    private val parser = AuctionSocketEventParser { Instant.parse("2026-09-11T06:00:00Z") }

    @Test
    fun snapshotUsesServerTimeAndRestoresPersonalBidState() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.AUCTION_SNAPSHOT,
                payload = buildJsonObject {
                    put("auctionId", 3)
                    put("currentPrice", 12_500)
                    put("bidCount", 4)
                    put("scheduledEndAt", "2026-09-11T06:10:00Z")
                    put("serverTime", "2026-09-11T06:00:00Z")
                    put("status", "ACTIVE")
                    put("myBid", buildJsonObject { put("isHighestBidder", true) })
                }
            )
        )!!

        assertEquals("3", update.auctionId)
        assertEquals(12_500, update.currentPrice)
        assertEquals(600, update.remainingSeconds)
        assertTrue(update.isHighestBidder == true)
    }

    // 서버가 마감을 15초로 되돌린 이벤트가 0.7초 늦게 도착해도 14 가 아니라 15 로 보여야 한다:
    // 기준 시각은 이벤트 발생 시각(occurredAt)을 쓰고, 초는 올림으로 센다
    @Test
    fun extendedEventCountsWholeSecondsFromServerOccurrence() {
        val lateParser = AuctionSocketEventParser { Instant.parse("2026-09-11T06:00:00.700Z") }
        val update = lateParser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.AUCTION_EXTENDED,
                occurredAt = "2026-09-11T06:00:00Z",
                payload = buildJsonObject {
                    put("auctionId", 3)
                    put("endedAt", "2026-09-11T06:00:15Z")
                    put("extensionSeconds", 15)
                }
            )
        )!!

        assertEquals(15, update.remainingSeconds)
        assertEquals("마감까지 남은 시간이 15초로 다시 맞춰졌어요.", update.message)

        val withoutOccurredAt = lateParser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.HIGHEST_BID_UPDATED,
                payload = buildJsonObject {
                    put("auctionId", 3)
                    put("currentPrice", 12_000)
                    put("endedAt", "2026-09-11T06:00:15Z")
                }
            )
        )!!
        assertEquals(15, withoutOccurredAt.remainingSeconds)
    }

    @Test
    fun endedEventStopsCountdownAndUsesFinalPrice() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.AUCTION_ENDED,
                payload = buildJsonObject {
                    put("auctionId", "auction-1")
                    put("result", "SOLD")
                    put("finalPrice", 52_000)
                    put("orderId", 81)
                    put("endedAt", "2026-09-11T06:00:00Z")
                }
            )
        )!!

        assertEquals(0, update.remainingSeconds)
        assertEquals(52_000, update.currentPrice)
        assertEquals("ENDED", update.status)
        assertEquals("81", update.orderId)
        assertEquals("경매가 낙찰됐어요.", update.message)
    }

    @Test
    fun rejectedBidKeepsCommandAndRetryGuidance() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.BID_REJECTED,
                commandId = "command-7",
                payload = buildJsonObject {
                    put("commandId", "command-7")
                    put("auctionId", "auction-1")
                    put("code", "INVALID_AMOUNT")
                    put("message", "현재가보다 높은 금액을 입력해주세요.")
                    put("currentPrice", 52_000)
                    put("minAllowedAmount", 52_001)
                    put("endedAt", "2026-09-11T06:10:00Z")
                }
            )
        )!!

        assertEquals("command-7", update.commandId)
        assertEquals("INVALID_AMOUNT", update.errorCode)
        assertEquals(52_001, update.minAllowedAmount)
        assertEquals(52_000, update.currentPrice)
    }
}
