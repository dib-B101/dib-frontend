package com.ssafy.dib.domain.auction

import com.ssafy.dib.core.network.ApiResult

data class AuctionSummary(
    val auctionId: String,
    val sellerMemberId: String,
    val productId: String,
    val title: String,
    val categoryName: String,
    val currentPrice: Int,
    val startPrice: Int,
    val bidCount: Int,
    val remainingSeconds: Int,
    val status: String,
    val bookmarked: Boolean,
    val isHighestBidder: Boolean? = null,
    val myBidAmount: Int? = null,
    val imageUrls: List<String> = emptyList()
)

interface AuctionRepository {
    fun getGeneralAuctions(
        size: Int = 20,
        categoryId: String? = null,
        status: String = "ACTIVE",
        minPrice: Long? = null,
        maxPrice: Long? = null,
        sort: String? = null
    ): ApiResult<List<AuctionSummary>>

    fun getActiveGeneralAuctions(size: Int = 20, categoryId: String? = null): ApiResult<List<AuctionSummary>> =
        getGeneralAuctions(size = size, categoryId = categoryId)

    fun getAuction(auctionId: String): ApiResult<AuctionSummary>
}
