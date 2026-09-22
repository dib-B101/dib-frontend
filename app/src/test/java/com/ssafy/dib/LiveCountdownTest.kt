package com.ssafy.dib

import com.ssafy.dib.feature.live.remainingSecondsUntil
import java.time.Instant
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveCountdownTest {
    private val now = Instant.parse("2026-09-14T09:00:00Z")

    @Test
    fun `future end time returns remaining seconds`() {
        val endedAt = now.plusSeconds(60).toString()
        assertEquals(60, remainingSecondsUntil(endedAt, now))
    }

    @Test
    fun `past end time is clamped to zero`() {
        val endedAt = now.minusSeconds(60).toString()
        assertEquals(0, remainingSecondsUntil(endedAt, now))
    }

    @Test
    fun `null or garbage input returns null`() {
        assertNull(remainingSecondsUntil(null, now))
        assertNull(remainingSecondsUntil("not-a-date", now))
    }

    @Test
    fun `offset date time string parses`() {
        // UTC 기준 2026-09-14T09:01:00Z 와 같은 순간(+09:00 오프셋)
        val endedAt = OffsetDateTime.parse("2026-09-14T18:01:00+09:00").toString()
        assertEquals(60, remainingSecondsUntil(endedAt, now))
    }
}
