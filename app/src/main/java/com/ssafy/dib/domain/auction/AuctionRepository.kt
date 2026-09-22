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
    // 아직 시작가를 정하지 않은 SCHEDULED 경매는 서버가 null 을 내려준다. 0 과 구분해야 "가격 미정" 을 보여줄 수 있다
    val currentPriceOrNull: Int? = null,
    val startPriceOrNull: Int? = null,
    val bidCount: Int,
    val auctionTimeSeconds: Long = 0,
    val remainingSeconds: Int,
    // 경매 종료 절대 시각(ISO-8601). 남은 초는 화면에서 매 틱 계산해야 탭 전환 뒤에도 정확하다
    val endedAt: String? = null,
    val status: String,
    val bookmarked: Boolean,
    val isHighestBidder: Boolean? = null,
    val myBidAmount: Int? = null,
    val myOrderId: String? = null,
    val imageUrls: List<String> = emptyList(),
    val sellerNickname: String? = null,
    val sellerRating: Double? = null,
    // 받은 평가 건수. 0 이면 평점을 화면에 그리지 않는다
    val sellerReviewCount: Int? = null,
    val sellerTradeCount: Int? = null,
    val productDescription: String? = null,
    val productCondition: String? = null,
    val productModelName: String? = null,
    val productReleaseYear: Int? = null,
    val productMarketPrice: Long? = null
)

data class SellerAuction(
    val auctionId: String,
    val productId: String,
    val startPrice: Int,
    val currentPrice: Int,
    val bidCount: Int,
    val status: String,
    val auctionTimeSeconds: Long
)
data class AuctionCommandResult(val auctionId: String, val message: String)
data class AuctionPage(val items: List<AuctionSummary>, val nextCursor: String?, val hasNext: Boolean)
data class SaleHistoryItem(val auction: AuctionSummary, val orderId: String?, val orderStatus: String?)
data class SaleHistoryPage(val items: List<SaleHistoryItem>, val nextCursor: String?, val hasNext: Boolean)
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

    fun getMySales(auctionStatus: String? = null, cursor: String? = null, size: Int = 30): ApiResult<SaleHistoryPage>
    fun getAuction(auctionId: String): ApiResult<AuctionSummary>
    fun getSellerAuctions(sellerId: String): ApiResult<List<SellerAuction>>
    fun getRecommendations(size: Int = 20): ApiResult<HomeRecommendations>
    fun getBookmarks(cursor: String? = null, size: Int = 30): ApiResult<AuctionPage>
    fun getMyBids(cursor: String? = null, size: Int = 30): ApiResult<BidHistoryPage>
    fun getBidHistory(auctionId: String, cursor: String? = null, size: Int = 20): ApiResult<AuctionBidHistoryPage>
    fun getBidSnapshot(auctionId: String): ApiResult<AuctionBidSnapshot>
    fun setBookmark(productId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<Boolean>
    fun createAuction(productId: String, startPrice: Long, auctionTime: Long, idempotencyKey: String): ApiResult<AuctionCommandResult>
    fun updateAuction(auctionId: String, startPrice: Long, auctionTime: Long): ApiResult<AuctionCommandResult>
    fun cancelAuction(auctionId: String, idempotencyKey: String): ApiResult<Unit>
    fun relistAuction(auctionId: String, idempotencyKey: String): ApiResult<AuctionCommandResult>
    fun startAuction(auctionId: String, idempotencyKey: String, startPrice: Long? = null, auctionTime: Long? = null): ApiResult<String>
}
