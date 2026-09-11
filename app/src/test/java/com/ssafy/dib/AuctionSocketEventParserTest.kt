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
                    put("endedAt", "2026-09-11T06:10:00Z")
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

    @Test
    fun endedEventStopsCountdownAndUsesFinalPrice() {
        val update = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.AUCTION_ENDED,
                payload = buildJsonObject {
                    put("auctionId", "auction-1")
                    put("result", "SOLD")
                    put("finalPrice", 52_000)
                    put("endedAt", "2026-09-11T06:00:00Z")
                }
            )
        )!!

        assertEquals(0, update.remainingSeconds)
        assertEquals(52_000, update.currentPrice)
        assertEquals("ENDED", update.status)
        assertEquals("경매가 낙찰됐어요.", update.message)
    }
}
