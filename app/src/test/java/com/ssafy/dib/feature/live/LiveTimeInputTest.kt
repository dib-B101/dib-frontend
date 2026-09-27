package com.ssafy.dib.feature.live

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTimeInputTest {
    @Test
    fun insertsColonAfterFourConsecutiveDigits() {
        assertEquals("19:30", formatLiveTimeInput("1930"))
        assertEquals("19:3", formatLiveTimeInput("193"))
        assertEquals("19", formatLiveTimeInput("19"))
    }

    @Test
    fun acceptsAlreadyFormattedAndPastedTime() {
        assertEquals("19:30", formatLiveTimeInput("19:30"))
        assertEquals("19:30", formatLiveTimeInput("19:30 extra"))
    }
}
