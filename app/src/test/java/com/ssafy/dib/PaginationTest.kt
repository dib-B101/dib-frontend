package com.ssafy.dib

import com.ssafy.dib.core.navigation.hasUsableNextCursor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginationTest {
    @Test
    fun acceptsOnlyANewNonBlankCursorWhenServerHasNextPage() {
        assertTrue(hasUsableNextCursor(true, "next-2", "next-1"))
        assertFalse(hasUsableNextCursor(false, "next-2", "next-1"))
        assertFalse(hasUsableNextCursor(true, null, "next-1"))
        assertFalse(hasUsableNextCursor(true, "", "next-1"))
        assertFalse(hasUsableNextCursor(true, "next-1", "next-1"))
    }
}
