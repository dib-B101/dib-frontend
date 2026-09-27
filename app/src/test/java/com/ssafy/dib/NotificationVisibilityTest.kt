package com.ssafy.dib

import com.ssafy.dib.domain.notification.DomainNotification
import com.ssafy.dib.domain.notification.isEnabledBy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationVisibilityTest {
    @Test fun eachToggleControlsItsOwnCategory() {
        assertFalse(notification("BID_OUTBID", "AUCTION").isEnabledBy(false, true, true))
        assertFalse(notification("LIVE_STARTED", "LIVE").isEnabledBy(true, false, true))
        assertFalse(notification("BOOKMARK_ENDING_SOON", "AUCTION").isEnabledBy(true, true, false))
        assertTrue(notification("SYSTEM", "SYSTEM").isEnabledBy(false, false, false))
        assertTrue(notification("BID_OUTBID", "AUCTION").isEnabledBy(true, false, false))
    }

    private fun notification(type: String, resourceType: String) = DomainNotification(
        eventId = "event", type = type, resourceType = resourceType, resourceId = "1",
        title = "title", body = "body", occurredAt = "2026-09-26T00:00:00Z"
    )
}
