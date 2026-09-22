package com.ssafy.dib

import com.ssafy.dib.feature.auction.isValidBidAmount
import com.ssafy.dib.feature.auction.minimumBidAmount
import com.ssafy.dib.feature.auction.roundUpToBidUnit
import com.ssafy.dib.feature.auction.steppedBidAmount
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

    // 시작가가 1,001원처럼 어긋난 경매도 안내 최소 금액·빠른 입력 결과는 항상 10원 단위여야 입찰이 통과한다
    @Test fun snapsOddPricesUpToTenWonUnit() {
        assertEquals(1_010, minimumBidAmount(1_001, 0))
        assertEquals(1_510, minimumBidAmount(1_001, 3))
        assertEquals(1_020, roundUpToBidUnit(1_011))
        assertEquals(1_020, roundUpToBidUnit(1_020))
        assertEquals(2_010, steppedBidAmount(1_001, 1_000, 1_010))
        assertEquals(2_010, steppedBidAmount(500, 1_000, 1_010))
    }
}
