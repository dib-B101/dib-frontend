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

data class AuctionCommandResult(val auctionId: String, val message: String)

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
    fun createAuction(productId: String, startPrice: Long, auctionTime: Long, idempotencyKey: String): ApiResult<AuctionCommandResult>
    fun updateAuction(auctionId: String, startPrice: Long, auctionTime: Long): ApiResult<AuctionCommandResult>
    fun cancelAuction(auctionId: String, idempotencyKey: String): ApiResult<Unit>
    fun startAuction(auctionId: String, idempotencyKey: String): ApiResult<String>
}
