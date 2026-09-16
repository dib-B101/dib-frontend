package com.ssafy.dib

import com.ssafy.dib.feature.auction.DetailAuctionState
import com.ssafy.dib.feature.auction.detailAuctionState
import com.ssafy.dib.feature.main.auctionStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductDetailStateTest {
    @Test
    fun onlyActiveAuctionCanResolveToBiddableState() {
        assertEquals(DetailAuctionState.Active, detailAuctionState("ACTIVE", 60, false))
        assertEquals(DetailAuctionState.HighestBidder, detailAuctionState("ACTIVE", 60, true))
        assertEquals(DetailAuctionState.Scheduled, detailAuctionState("SCHEDULED", 60, false))
        assertEquals(DetailAuctionState.Cancelled, detailAuctionState("CANCELLED", 60, false))
        assertEquals(DetailAuctionState.Cancelled, detailAuctionState("CANCELED", 60, false))
        assertEquals(DetailAuctionState.Lost, detailAuctionState("ENDED", 60, false))
    }

    @Test
    fun expiredActiveAuctionUsesFinalBidState() {
        assertEquals(DetailAuctionState.Lost, detailAuctionState("ACTIVE", 0, false))
        assertEquals(DetailAuctionState.Won, detailAuctionState("ACTIVE", 0, true))
    }

    @Test
    fun currentCanceledSpellingUsesCancelledLabel() {
        assertEquals("취소", auctionStatusLabel("CANCELED"))
    }
}
