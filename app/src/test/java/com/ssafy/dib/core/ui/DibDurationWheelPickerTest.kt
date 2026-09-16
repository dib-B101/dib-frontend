package com.ssafy.dib.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DibDurationWheelPickerTest {
    @Test
    fun durationParts_splitsDaysHoursAndMinutes() {
        assertEquals(DurationParts(days = 2, hours = 3, minutes = 15), durationParts(184_500L))
    }

    @Test
    fun durationSeconds_combinesAndBoundsWheelValues() {
        assertEquals(184_500L, durationSeconds(days = 2, hours = 3, minutes = 15))
        assertEquals(30L * 86_400L + 23L * 3_600L + 59L * 60L, durationSeconds(99, 99, 99))
    }
}
