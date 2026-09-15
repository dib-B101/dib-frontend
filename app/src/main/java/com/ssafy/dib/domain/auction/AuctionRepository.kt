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
    val imageUrls: List<String> = emptyList(),
    val sellerNickname: String? = null,
    val sellerRating: Double? = null,
    val sellerTradeCount: Int? = null,
    val productDescription: String? = null,
    val productCondition: String? = null,
    val productModelName: String? = null,
    val productReleaseYear: Int? = null,
    val productMarketPrice: Long? = null
)

data class AuctionCommandResult(val auctionId: String, val message: String)
data class AuctionPage(val items: List<AuctionSummary>, val nextCursor: String?, val hasNext: Boolean)
data class BidHistoryItem(val bidId: String, val auctionId: String, val amount: Int, val createdAt: String)
data class BidHistoryPage(val items: List<BidHistoryItem>, val nextCursor: String?, val hasNext: Boolean)
data class AuctionBidHistoryItem(val bidId: String, val maskedBidderId: String, val amount: Int, val createdAt: String)
data class AuctionBidHistoryPage(val items: List<AuctionBidHistoryItem>, val nextCursor: String?, val hasNext: Boolean)
data class RecommendedLive(val liveBroadcastId: String, val title: String, val description: String?, val status: String, val scheduledAt: String?)
data class HomeRecommendations(val liveItems: List<RecommendedLive>, val generalItems: List<AuctionSummary>)
data class AuctionBidSnapshot(
    val auctionId: String,
    val currentPrice: Int,
    val remainingSeconds: Int,
    val bidCount: Int,
    val bidderCount: Int,
    val isHighestBidder: Boolean
)

interface AuctionRepository {
    fun getAuctions(
        scope: String,
        status: String,
        cursor: String? = null,
        size: Int = 30
    ): ApiResult<AuctionPage>

    fun getGeneralAuctions(
        size: Int = 20,
        categoryId: String? = null,
        status: String = "ACTIVE",
        minPrice: Long? = null,
        maxPrice: Long? = null,
        sort: String? = null,
        cursor: String? = null
    ): ApiResult<AuctionPage>

    fun getActiveGeneralAuctions(size: Int = 20, categoryId: String? = null, cursor: String? = null): ApiResult<AuctionPage> =
        getGeneralAuctions(size = size, categoryId = categoryId, cursor = cursor)

    fun getAuction(auctionId: String): ApiResult<AuctionSummary>
    fun getRecommendations(size: Int = 20): ApiResult<HomeRecommendations>
    fun getBookmarks(cursor: String? = null, size: Int = 30): ApiResult<AuctionPage>
    fun getMyBids(cursor: String? = null, size: Int = 30): ApiResult<BidHistoryPage>
    fun getBidHistory(auctionId: String, cursor: String? = null, size: Int = 20): ApiResult<AuctionBidHistoryPage>
    fun getBidSnapshot(auctionId: String): ApiResult<AuctionBidSnapshot>
    fun setBookmark(auctionId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<Boolean>
    fun createAuction(productId: String, startPrice: Long, auctionTime: Long, idempotencyKey: String): ApiResult<AuctionCommandResult>
    fun updateAuction(auctionId: String, startPrice: Long, auctionTime: Long): ApiResult<AuctionCommandResult>
    fun cancelAuction(auctionId: String, idempotencyKey: String): ApiResult<Unit>
    fun startAuction(auctionId: String, idempotencyKey: String): ApiResult<String>
}
