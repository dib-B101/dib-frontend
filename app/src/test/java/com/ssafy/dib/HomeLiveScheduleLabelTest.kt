package com.ssafy.dib

import com.ssafy.dib.feature.home.homeLiveScheduleLabel
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLiveScheduleLabelTest {
    @Test
    fun formatsServerScheduleInDeviceTimeZone() {
        assertEquals(
            "9월 15일 20:30 예정",
            homeLiveScheduleLabel("2026-09-15T11:30:00Z", ZoneOffset.ofHours(9))
        )
    }

    @Test
    fun fallsBackWhenScheduleIsMissingOrInvalid() {
        assertEquals("방송 예정", homeLiveScheduleLabel(null, ZoneOffset.UTC))
        assertEquals("방송 예정", homeLiveScheduleLabel("invalid", ZoneOffset.UTC))
    }
}
