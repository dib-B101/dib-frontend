package com.ssafy.dib.domain.live

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.domain.auction.AuctionSummary

data class LiveFeedItem(
    val liveBroadcastId: String,
    val memberId: String,
    val title: String,
    val description: String?,
    val streamUrl: String?,
    val viewCount: Int,
    val currentAuction: AuctionSummary?
)

data class LiveChatMessage(
    val liveChattingId: String,
    val memberId: String,
    val nickname: String?,
    val content: String,
    val time: String
)

interface LiveRepository {
    fun getFeed(size: Int = 20): ApiResult<List<LiveFeedItem>>
    fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String? = null, size: Int = 50): ApiResult<List<LiveChatMessage>>
}
