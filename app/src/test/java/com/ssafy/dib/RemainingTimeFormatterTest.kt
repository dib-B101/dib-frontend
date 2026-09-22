package com.ssafy.dib

import com.ssafy.dib.core.time.formatRemainingTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingTimeFormatterTest {
    @Test fun formatsShortCountdown() {
        assertEquals("00:00", formatRemainingTime(0))
        assertEquals("00:59", formatRemainingTime(59))
        assertEquals("01:00", formatRemainingTime(60))
        assertEquals("59:59", formatRemainingTime(3_599))
    }

    @Test fun formatsHoursAndDays() {
        assertEquals("1시간 00분", formatRemainingTime(3_600))
        assertEquals("23시간 59분", formatRemainingTime(86_399))
        assertEquals("1일 0시간", formatRemainingTime(86_400))
        assertEquals("6일 23시간", formatRemainingTime(7 * 86_400 - 3_600))
        assertEquals("7일 0시간", formatRemainingTime(7 * 86_400))
    }
}
