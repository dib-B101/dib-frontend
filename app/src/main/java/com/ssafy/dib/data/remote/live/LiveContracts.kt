package com.ssafy.dib.data.remote.live

import com.ssafy.dib.data.remote.auction.AuctionDto
import com.ssafy.dib.data.remote.auction.AuctionProductDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LiveFeedResponse(
    val items: List<LiveFeedItemDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val serverTime: String? = null
)

@Serializable
data class LiveFeedItemDto(
    val liveBroadcast: LiveBroadcastDto,
    val activeAuction: AuctionDto? = null,
    val product: AuctionProductDto? = null
)

@Serializable
data class LiveBroadcastDto(
    val liveBroadcastId: JsonElement,
    val memberId: JsonElement? = null,
    val title: String = "Live",
    val description: String? = null,
    val status: String = "LIVE",
    val streamUrl: String? = null,
    val viewCount: Int = 0,
    val scheduledAt: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null
)

@Serializable data class LiveBroadcastListResponse(val items: List<LiveBroadcastDto> = emptyList(), val nextCursor: String? = null, val hasNext: Boolean = false)
@Serializable data class CreateLiveBroadcastRequest(val title: String, val description: String? = null, val scheduledAt: String, val streamUrl: String? = null)
@Serializable data class CreateLiveBroadcastResponse(val liveBroadcastId: JsonElement, val status: String, val scheduledAt: String, val createdAt: String)
@Serializable data class SetLiveItemsRequest(val auctionIds: List<JsonElement>)
@Serializable data class SetLiveItemsResponse(val liveBroadcastId: JsonElement, val auctions: List<AuctionDto> = emptyList())

@Serializable
data class LiveBroadcastDetailResponse(
    val liveBroadcastId: JsonElement,
    val memberId: JsonElement? = null,
    val title: String = "Live",
    val description: String? = null,
    val status: String = "",
    val streamUrl: String? = null,
    val scheduledAt: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val viewCount: Int = 0,
    val auctions: List<AuctionDto> = emptyList(),
    val currentAuction: AuctionDto? = null
)

@Serializable
data class LiveChatMessageListResponse(
    val items: List<LiveChatMessageDto> = emptyList(),
    val hasMore: Boolean = false
)

@Serializable
data class LiveChatMessageDto(
    val liveChattingId: JsonElement,
    val memberId: JsonElement,
    val nickname: String? = null,
    val content: String,
    val time: String
)
