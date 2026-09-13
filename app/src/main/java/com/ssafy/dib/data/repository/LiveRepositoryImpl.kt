package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.live.LiveRemoteDataSource
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveRepository
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
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
