package com.ssafy.dib

import com.ssafy.dib.feature.auction.isValidBidAmount
import com.ssafy.dib.feature.auction.minimumBidAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BidSubmissionTest {
    @Test fun followsServerMinimumForFirstAndLaterBids() {
        assertEquals(10_000, minimumBidAmount(10_000, 0))
        assertEquals(10_000, minimumBidAmount(9_500, 1))
        assertEquals(11_000, minimumBidAmount(10_000, 1))
        assertEquals(105_000, minimumBidAmount(100_000, 1))
        assertEquals(1_010_000, minimumBidAmount(1_000_000, 1))
    }

    @Test fun requiresTenWonUnitAndMinimum() {
        assertTrue(isValidBidAmount(11_000, 11_000))
        assertFalse(isValidBidAmount(10_999, 11_000))
        assertFalse(isValidBidAmount(11_001, 11_000))
    }
}
