package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.notification.NotificationPageResponse
import com.ssafy.dib.data.remote.notification.NotificationUnreadCountResponse
import com.ssafy.dib.data.repository.toDomain
import com.ssafy.dib.domain.notification.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationContractTest {
    @Test
    fun decodesServerUnreadCount() {
        val response = DibJson.instance.decodeFromString(
            NotificationUnreadCountResponse.serializer(),
            """{"unreadCount":42}"""
        )

        assertEquals(42, response.unreadCount)
    }

    @Test
    fun mapsPersistedRunnerUpOffer() {
        val page = DibJson.instance.decodeFromString(
            NotificationPageResponse.serializer(),
            """{"items":[{"notificationId":81,"type":"AUCTION_WON","title":"차순위 낙찰 안내","content":"58,000원으로 24시간 안에 수락해주세요.","isRead":false,"auctionId":31,"createdAt":"2026-09-16T03:00:00"}],"nextCursor":"81","hasNext":true}"""
        )

        val notification = page.items.single().toDomain()

        assertEquals("81", notification.eventId)
        assertEquals("31", notification.resourceId)
        assertEquals(NotificationCategory.Trade, notification.category)
        assertEquals(true, notification.isRunnerUpOffer)
        assertEquals(false, notification.isRead)
    }
}
