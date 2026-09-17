package com.ssafy.dib

import com.ssafy.dib.feature.live.formatStudioDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class SellerLiveStudioTest {
    @Test
    fun formatsElapsedBroadcastTime() {
        assertEquals("00:00:00", formatStudioDuration(0))
        assertEquals("00:01:05", formatStudioDuration(65))
        assertEquals("01:01:01", formatStudioDuration(3_661))
        assertEquals("00:00:00", formatStudioDuration(-1))
    }
}
