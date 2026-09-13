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

interface LiveRepository {
    fun getFeed(size: Int = 20): ApiResult<List<LiveFeedItem>>
}
