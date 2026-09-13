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
    val viewCount: Int = 0
)
