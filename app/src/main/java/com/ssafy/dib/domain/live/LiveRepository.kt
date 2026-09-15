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

data class LiveFeedPage(
    val items: List<LiveFeedItem>,
    val nextCursor: String?,
    val hasNext: Boolean
)

data class LiveChatMessage(
    val liveChattingId: String,
    val memberId: String,
    val nickname: String?,
    val content: String,
    val time: String
)

data class LiveChatMessagePage(
    val items: List<LiveChatMessage>,
    val hasMore: Boolean
)

data class LiveBroadcastDetail(
    val liveBroadcastId: String,
    val memberId: String,
    val title: String,
    val description: String?,
    val status: String,
    val streamUrl: String?,
    val viewCount: Int,
    val auctions: List<AuctionSummary>,
    val currentAuction: AuctionSummary?
)

data class LiveBroadcastSummary(
    val liveBroadcastId: String,
    val title: String,
    val description: String?,
    val status: String,
    val streamUrl: String?,
    val scheduledAt: String?,
    val viewCount: Int
)

data class LiveBroadcastPage(
    val items: List<LiveBroadcastSummary>,
    val nextCursor: String?,
    val hasNext: Boolean
)

data class LiveStreamSession(val streamUrl: String, val expiresAt: String?, val provider: String?)

interface LiveRepository {
    fun getFeed(cursor: String? = null, size: Int = 20): ApiResult<LiveFeedPage>
    fun getDetail(liveBroadcastId: String): ApiResult<LiveBroadcastDetail>
    fun getMine(status: String? = null, cursor: String? = null, size: Int = 30): ApiResult<LiveBroadcastPage>
    fun create(title: String, description: String?, scheduledAt: String, streamUrl: String?, idempotencyKey: String): ApiResult<String>
    fun update(liveBroadcastId: String, title: String, description: String?, scheduledAt: String, streamUrl: String?): ApiResult<String>
    fun setItems(liveBroadcastId: String, auctionIds: List<String>): ApiResult<List<AuctionSummary>>
    fun prepareStream(liveBroadcastId: String, idempotencyKey: String): ApiResult<LiveStreamSession>
    fun start(liveBroadcastId: String, idempotencyKey: String): ApiResult<String>
    fun startAuction(liveBroadcastId: String, auctionId: String, idempotencyKey: String): ApiResult<String>
    fun end(liveBroadcastId: String, idempotencyKey: String): ApiResult<String>
    fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String? = null, size: Int = 50): ApiResult<LiveChatMessagePage>
}
