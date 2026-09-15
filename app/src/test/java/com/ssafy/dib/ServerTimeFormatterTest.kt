package com.ssafy.dib

import com.ssafy.dib.core.time.formatServerTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerTimeFormatterTest {
    @Test
    fun convertsServerUtcTimeAcrossLocalDateBoundary() {
        assertEquals(
            "2026.09.16 00:30",
            formatServerTime(
                "2026-09-15T15:30:00Z",
                zoneId = ZoneId.of("Asia/Seoul")
            )
        )
    }

    @Test
    fun invalidOrMissingServerTimeHasNoLabel() {
        assertNull(formatServerTime(null))
        assertNull(formatServerTime("not-an-instant"))
    }
}
