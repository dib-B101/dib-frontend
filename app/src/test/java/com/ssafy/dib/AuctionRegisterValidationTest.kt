package com.ssafy.dib

import com.ssafy.dib.feature.auction.isValidAuctionStartPrice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionRegisterValidationTest {
    @Test
    fun startPriceMustBeAtLeastOneThousandWon() {
        assertFalse(isValidAuctionStartPrice(0))
        assertFalse(isValidAuctionStartPrice(999))
        assertTrue(isValidAuctionStartPrice(1_000))
    }
}
