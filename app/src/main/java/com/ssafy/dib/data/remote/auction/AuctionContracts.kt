package com.ssafy.dib.data.remote.auction

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuctionListResponse(
    val items: List<AuctionDto> = emptyList(),
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
    val product: AuctionProductDto? = null
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
