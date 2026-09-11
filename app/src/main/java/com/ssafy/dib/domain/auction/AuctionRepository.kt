package com.ssafy.dib.domain.auction

import com.ssafy.dib.core.network.ApiResult

data class AuctionSummary(
    val auctionId: String,
    val productId: String,
    val title: String,
    val categoryName: String,
    val currentPrice: Int,
    val startPrice: Int,
    val bidCount: Int,
    val remainingSeconds: Int,
    val status: String,
    val bookmarked: Boolean,
    val isHighestBidder: Boolean? = null
)

interface AuctionRepository {
    fun getActiveGeneralAuctions(size: Int = 20): ApiResult<List<AuctionSummary>>
    fun getAuction(auctionId: String): ApiResult<AuctionSummary>
}
