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
        // 서버와 같은 10원 단위 규칙 — 1,005원처럼 끝자리가 어긋나면 시작할 수 없다
        assertFalse(isScheduledAuctionReady("1", "SCHEDULED", 1_005, 300))
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
