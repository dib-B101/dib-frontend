package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.DomainNotificationParser
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.domain.notification.NotificationCategory
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
}
