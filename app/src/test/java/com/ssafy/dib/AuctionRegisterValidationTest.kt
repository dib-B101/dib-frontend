package com.ssafy.dib

import com.ssafy.dib.feature.auction.formatAuctionDuration
import com.ssafy.dib.feature.auction.isScheduledAuctionReady
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionRegisterValidationTest {
    @Test
    fun onlyCompleteScheduledAuctionCanStart() {
        assertFalse(isScheduledAuctionReady(null, "SCHEDULED", 1_000, 300))
        assertFalse(isScheduledAuctionReady("1", "ACTIVE", 1_000, 300))
        assertFalse(isScheduledAuctionReady("1", "SCHEDULED", 999, 300))
        assertFalse(isScheduledAuctionReady("1", "SCHEDULED", 1_000, 299))
        assertTrue(isScheduledAuctionReady("1", "SCHEDULED", 1_000, 300))
    }

    @Test
    fun durationUsesReadableMinutesAndHours() {
        assertTrue(formatAuctionDuration(300) == "5분")
        assertTrue(formatAuctionDuration(3_600) == "1시간")
        assertTrue(formatAuctionDuration(5_400) == "1시간 30분")
    }
}
