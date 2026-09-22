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

// 라이브 상품 경매 시작 응답. 콘솔이 소켓 이벤트를 기다리지 않고 바로 카운트다운을 그릴 수 있게 종료 시각을 함께 받는다
data class LiveAuctionStart(val status: String, val scheduledEndAt: String?)

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

data class LiveItemPlan(
    val auctionId: String,
    val startPrice: Long? = null,
    val auctionTime: Long? = null
)

data class LiveStreamSession(
    val serverUrl: String,
    val token: String,
    val roomName: String,
    val participantName: String,
    val provider: String = "LIVEKIT"
)

interface LiveRepository {
    fun getFeed(cursor: String? = null, size: Int = 20): ApiResult<LiveFeedPage>
    fun getDetail(liveBroadcastId: String): ApiResult<LiveBroadcastDetail>
    fun getMine(status: String? = null, cursor: String? = null, size: Int = 30): ApiResult<LiveBroadcastPage>
    fun create(title: String, description: String?, scheduledAt: String, streamUrl: String?, idempotencyKey: String): ApiResult<String>
    fun update(liveBroadcastId: String, title: String, description: String?, scheduledAt: String, streamUrl: String?): ApiResult<String>
    fun setItems(liveBroadcastId: String, items: List<LiveItemPlan>): ApiResult<List<AuctionSummary>>
    fun prepareStream(liveBroadcastId: String, idempotencyKey: String): ApiResult<LiveStreamSession>
    fun start(liveBroadcastId: String, idempotencyKey: String): ApiResult<String>
    fun startAuction(liveBroadcastId: String, auctionId: String, idempotencyKey: String): ApiResult<LiveAuctionStart>
    fun end(liveBroadcastId: String, idempotencyKey: String): ApiResult<String>
    fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String? = null, size: Int = 50): ApiResult<LiveChatMessagePage>
}
