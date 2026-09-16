package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.DomainNotificationParser
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.domain.notification.NotificationCategory
import com.ssafy.dib.domain.notification.DomainNotification
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainNotificationParserTest {
    private val parser = DomainNotificationParser()

    @Test
    fun parsesContractPayloadAndUsesEnvelopeEventId() {
        val notification = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.DOMAIN_NOTIFICATION,
                eventId = "event-17",
                payload = buildJsonObject {
                    put("type", "ORDER_PAID")
                    put("resourceType", "ORDER")
                    put("resourceId", 31)
                    put("title", "결제가 완료됐어요")
                    put("body", "판매자가 상품을 준비하고 있어요.")
                    put("occurredAt", "2026-09-13T03:00:00Z")
                }
            )
        )!!

        assertEquals("event-17", notification.eventId)
        assertEquals("31", notification.resourceId)
        assertEquals(NotificationCategory.Trade, notification.category)
    }

    @Test
    fun parsesBackendNotificationIdWhenEnvelopeHasNoEventId() {
        val notification = parser.parse(
            SocketEnvelope(
                eventType = SocketEventTypes.DOMAIN_NOTIFICATION,
                payload = buildJsonObject {
                    put("notificationId", "81")
                    put("type", "AUCTION_WON")
                    put("resourceType", "AUCTION")
                    put("resourceId", 31)
                    put("title", "차순위 낙찰 안내")
                    put("body", "24시간 안에 수락해주세요.")
                    put("isRead", false)
                    put("occurredAt", "2026-09-16T03:00:00Z")
                }
            )
        )!!

        assertEquals("81", notification.eventId)
        assertEquals(true, notification.isRunnerUpOffer)
        assertEquals(false, notification.isRead)
    }

    @Test
    fun rejectsIncompleteOrDifferentEvents() {
        assertNull(parser.parse(SocketEnvelope(eventType = SocketEventTypes.PONG)))
        assertNull(
            parser.parse(
                SocketEnvelope(
                    eventType = SocketEventTypes.DOMAIN_NOTIFICATION,
                    eventId = "event-18",
                    payload = buildJsonObject { put("type", "LIVE_STARTED") }
                )
            )
        )
    }

    @Test
    fun categorizesBookmarkBeforeAuctionAndTreatsBidAsTrade() {
        fun notification(type: String, resourceType: String) = DomainNotification(
            eventId = type,
            type = type,
            resourceType = resourceType,
            resourceId = "1",
            title = "title",
            body = "body",
            occurredAt = "2026-09-14T00:00:00Z"
        )

        assertEquals(NotificationCategory.Bookmark, notification("BOOKMARK_ENDING_SOON", "AUCTION").category)
        assertEquals(NotificationCategory.Trade, notification("BID_OUTBID", "AUCTION").category)
        assertEquals(NotificationCategory.Live, notification("LIVE_STARTED", "LIVE_BROADCAST").category)
    }
}
