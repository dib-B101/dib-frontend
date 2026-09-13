package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.live.LiveRemoteDataSource
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveRepository
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.domain.live.LiveBroadcastDetail
import com.ssafy.dib.domain.live.LiveBroadcastSummary
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class LiveRepositoryImpl(
    private val remote: LiveRemoteDataSource,
    private val now: () -> Instant = Instant::now
) : LiveRepository {
    override fun getFeed(size: Int): ApiResult<List<LiveFeedItem>> = when (val result = remote.getFeed(size)) {
        is ApiResult.Success -> ApiResult.Success(result.value.items.map { item ->
            val live = item.liveBroadcast
            LiveFeedItem(
                liveBroadcastId = live.liveBroadcastId.idValue(),
                memberId = live.memberId?.idValue().orEmpty(),
                title = live.title,
                description = live.description,
                streamUrl = live.streamUrl,
                viewCount = live.viewCount.coerceAtLeast(0),
                currentAuction = item.activeAuction?.copy(product = item.activeAuction.product ?: item.product)?.toDomain(now())
            )
        }, result.status)
        is ApiResult.Failure -> result
    }

    override fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String?, size: Int): ApiResult<List<LiveChatMessage>> =
        when (val result = remote.getMessages(liveBroadcastId, beforeLiveChattingId, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map { message ->
                LiveChatMessage(
                    liveChattingId = message.liveChattingId.idValue(),
                    memberId = message.memberId.idValue(),
                    nickname = message.nickname,
                    content = message.content,
                    time = message.time
                )
            }, result.status)
            is ApiResult.Failure -> result
        }

    override fun getDetail(liveBroadcastId: String): ApiResult<LiveBroadcastDetail> = when (val result = remote.getDetail(liveBroadcastId)) {
        is ApiResult.Success -> ApiResult.Success(result.value.let { live ->
            LiveBroadcastDetail(
                liveBroadcastId = live.liveBroadcastId.idValue(),
                memberId = live.memberId?.idValue().orEmpty(),
                title = live.title,
                description = live.description,
                status = live.status,
                streamUrl = live.streamUrl,
                viewCount = live.viewCount.coerceAtLeast(0),
                auctions = live.auctions.map { it.toDomain(now()) },
                currentAuction = live.currentAuction?.toDomain(now())
            )
        }, result.status)
        is ApiResult.Failure -> result
    }

    override fun getMine(status: String?, size: Int): ApiResult<List<LiveBroadcastSummary>> = when (val result = remote.getMine(status, size)) {
        is ApiResult.Success -> ApiResult.Success(result.value.items.map { live ->
            LiveBroadcastSummary(
                liveBroadcastId = live.liveBroadcastId.idValue(),
                title = live.title,
                description = live.description,
                status = live.status,
                streamUrl = live.streamUrl,
                scheduledAt = live.scheduledAt,
                viewCount = live.viewCount.coerceAtLeast(0)
            )
        }, result.status)
        is ApiResult.Failure -> result
    }

    override fun create(title: String, description: String?, scheduledAt: String, streamUrl: String?): ApiResult<String> =
        when (val result = remote.create(title, description, scheduledAt, streamUrl)) {
            is ApiResult.Success -> ApiResult.Success(result.value.liveBroadcastId.idValue(), result.status)
            is ApiResult.Failure -> result
        }

    override fun setItems(liveBroadcastId: String, auctionIds: List<String>): ApiResult<List<com.ssafy.dib.domain.auction.AuctionSummary>> =
        when (val result = remote.setItems(liveBroadcastId, auctionIds)) {
            is ApiResult.Success -> ApiResult.Success(result.value.auctions.map { it.toDomain(now()) }, result.status)
            is ApiResult.Failure -> result
        }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
