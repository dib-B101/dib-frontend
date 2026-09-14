package com.ssafy.dib.data.remote.auction

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import com.ssafy.dib.data.remote.live.LiveBroadcastDto
import com.ssafy.dib.data.remote.product.ProductSellerSummaryDto

@Serializable
data class AuctionListResponse(
    val items: List<AuctionDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class AuctionRecommendationResponse(
    val liveItems: List<LiveBroadcastDto> = emptyList(),
    val generalItems: List<AuctionDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class AuctionDto(
    val auctionId: JsonElement,
    val memberId: JsonElement? = null,
    val productId: JsonElement? = null,
    val categoryId: JsonElement? = null,
    val title: String? = null,
    val productName: String? = null,
    val categoryName: String? = null,
    val startPrice: Long = 0,
    val currentPrice: Long = 0,
    val bidCount: Int = 0,
    val auctionTime: Long = 0,
    val scheduledEndAt: String? = null,
    val endedAt: String? = null,
    val serverTime: String? = null,
    val status: String = "",
    val bookmarked: Boolean = false,
    val myBid: MyBidDto? = null,
    val product: AuctionProductDto? = null,
    val sellerSummary: ProductSellerSummaryDto? = null
)

@Serializable
data class MyBidDto(
    val amount: Long? = null,
    val isHighestBidder: Boolean = false
)

@Serializable
data class AuctionProductDto(
    val productId: JsonElement? = null,
    val name: String? = null,
    val title: String? = null,
    val categoryName: String? = null,
    val thumbnailUrl: String? = null,
    val images: List<JsonElement> = emptyList()
)

@Serializable data class CreateAuctionRequest(val productId: JsonElement, val startPrice: Long, val auctionTime: Long)
@Serializable data class UpdateAuctionRequest(val startPrice: Long, val auctionTime: Long, val liveBroadcastId: JsonElement? = null)
@Serializable data class AuctionCommandResponse(val auctionId: JsonElement? = null, val message: String = "")
@Serializable data class StartAuctionResponse(val message: String = "")
@Serializable data class BookmarkResponse(val bookmarked: Boolean)

@Serializable
data class BidHistoryListResponse(
    val items: List<BidHistoryDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class BidHistoryDto(
    val bidId: JsonElement,
    val auctionId: JsonElement,
    val amount: Long,
    val createdAt: String
)

@Serializable
data class AuctionBidHistoryListResponse(
    val items: List<MaskedBidHistoryDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class MaskedBidHistoryDto(
    val bidId: JsonElement,
    val maskedBidderId: String,
    val amount: Long,
    val createdAt: String
)

@Serializable
data class AuctionBidSnapshotResponse(
    val auctionId: JsonElement,
    val currentPrice: Long,
    val auctionTime: Long,
    val startedAt: String? = null,
    val scheduledEndAt: String? = null,
    val bidCount: Int = 0,
    val bidderCount: Int = 0,
    val isHighestBidder: Boolean = false,
    val serverTime: String? = null
)
