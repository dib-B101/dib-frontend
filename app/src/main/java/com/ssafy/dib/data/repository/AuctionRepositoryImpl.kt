package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auction.AuctionDto
import com.ssafy.dib.data.remote.auction.AuctionRemoteDataSource
import com.ssafy.dib.domain.auction.AuctionRepository
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.auction.AuctionPage
import com.ssafy.dib.domain.auction.AuctionCommandResult
import com.ssafy.dib.domain.auction.BidHistoryItem
import com.ssafy.dib.domain.auction.BidHistoryPage
import com.ssafy.dib.domain.auction.AuctionBidHistoryItem
import com.ssafy.dib.domain.auction.AuctionBidHistoryPage
import com.ssafy.dib.domain.auction.HomeRecommendations
import com.ssafy.dib.domain.auction.RecommendedLive
import com.ssafy.dib.domain.auction.AuctionBidSnapshot
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class AuctionRepositoryImpl(
    private val remote: AuctionRemoteDataSource,
    private val now: () -> Instant = Instant::now
) : AuctionRepository {
    override fun getAuctions(scope: String, status: String, cursor: String?, size: Int): ApiResult<AuctionPage> =
        when (val result = remote.getAuctions(scope, status, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionPage(
                    items = result.value.items.map { it.toDomain(now()) },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getGeneralAuctions(
        size: Int,
        categoryId: String?,
        status: String,
        minPrice: Long?,
        maxPrice: Long?,
        sort: String?,
        cursor: String?
    ): ApiResult<AuctionPage> =
        when (val result = remote.getGeneralAuctions(size, categoryId, status, minPrice, maxPrice, sort, cursor)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionPage(
                    items = result.value.items.map { it.toDomain(now()) },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getAuction(auctionId: String): ApiResult<AuctionSummary> =
        when (val result = remote.getAuction(auctionId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(now()), result.status)
            is ApiResult.Failure -> result
        }

    override fun getRecommendations(size: Int): ApiResult<HomeRecommendations> =
        when (val result = remote.getRecommendations(size)) {
            is ApiResult.Success -> ApiResult.Success(
                HomeRecommendations(
                    liveItems = result.value.liveItems.map { live ->
                        RecommendedLive(
                            liveBroadcastId = live.liveBroadcastId.idValue(),
                            title = live.title,
                            description = live.description,
                            status = live.status,
                            scheduledAt = live.scheduledAt
                        )
                    },
                    generalItems = result.value.generalItems.map { it.toDomain(now()) }
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getBookmarks(cursor: String?, size: Int): ApiResult<AuctionPage> =
        when (val result = remote.getBookmarks(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionPage(
                    items = result.value.items.map { it.toDomain(now()) },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getMyBids(cursor: String?, size: Int): ApiResult<BidHistoryPage> =
        when (val result = remote.getMyBids(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                BidHistoryPage(
                    items = result.value.items.map { bid ->
                        BidHistoryItem(
                            bidId = bid.bidId.idValue(),
                            auctionId = bid.auctionId.idValue(),
                            amount = bid.amount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                            createdAt = bid.createdAt
                        )
                    },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getBidHistory(auctionId: String, cursor: String?, size: Int): ApiResult<AuctionBidHistoryPage> =
        when (val result = remote.getBidHistory(auctionId, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionBidHistoryPage(
                    items = result.value.items.map { bid ->
                        AuctionBidHistoryItem(
                            bidId = bid.bidId.idValue(),
                            maskedBidderId = bid.maskedBidderId,
                            amount = bid.amount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                            createdAt = bid.createdAt
                        )
                    },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getBidSnapshot(auctionId: String): ApiResult<AuctionBidSnapshot> =
        when (val result = remote.getBidSnapshot(auctionId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(now()), result.status)
            is ApiResult.Failure -> result
        }

    override fun setBookmark(auctionId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<Boolean> =
        when (val result = remote.setBookmark(auctionId, bookmarked, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.bookmarked, result.status)
            is ApiResult.Failure -> result
        }

    override fun createAuction(productId: String, startPrice: Long, auctionTime: Long, idempotencyKey: String): ApiResult<AuctionCommandResult> =
        when (val result = remote.createAuction(productId, startPrice, auctionTime, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(AuctionCommandResult(result.value.auctionId?.idValue().orEmpty(), result.value.message), result.status)
            is ApiResult.Failure -> result
        }

    override fun updateAuction(auctionId: String, startPrice: Long, auctionTime: Long): ApiResult<AuctionCommandResult> =
        when (val result = remote.updateAuction(auctionId, startPrice, auctionTime)) {
            is ApiResult.Success -> ApiResult.Success(AuctionCommandResult(result.value.auctionId?.idValue() ?: auctionId, result.value.message), result.status)
            is ApiResult.Failure -> result
        }

    override fun cancelAuction(auctionId: String, idempotencyKey: String) = remote.cancelAuction(auctionId, idempotencyKey)

    override fun startAuction(auctionId: String, idempotencyKey: String): ApiResult<String> = when (val result = remote.startAuction(auctionId, idempotencyKey)) {
        is ApiResult.Success -> ApiResult.Success(result.value.message, result.status)
        is ApiResult.Failure -> result
    }
}

internal fun AuctionDto.toDomain(now: Instant): AuctionSummary {
    val referenceTime = serverTime.toInstantOrNull() ?: now
    val endTime = scheduledEndAt.toInstantOrNull() ?: endedAt.toInstantOrNull()
    val remaining = endTime?.let { Duration.between(referenceTime, it).seconds.coerceAtLeast(0) }
        ?: auctionTime.coerceAtLeast(0)
    val detailedImages = product?.images.orEmpty().mapNotNull { image ->
        (image as? JsonPrimitive)?.contentOrNull
            ?: runCatching {
                image.jsonObject["imageUrl"]?.let { (it as? JsonPrimitive)?.contentOrNull }
                    ?: image.jsonObject["url"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            }.getOrNull()
    }.filter(String::isNotBlank).distinct()
    return AuctionSummary(
        auctionId = auctionId.idValue(),
        sellerMemberId = memberId?.idValue().orEmpty(),
        productId = productId?.idValue() ?: product?.productId?.idValue().orEmpty(),
        title = title ?: productName ?: product?.title ?: product?.name ?: "경매 상품",
        categoryName = categoryName ?: product?.categoryName ?: "기타",
        currentPrice = currentPrice.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        startPrice = startPrice.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        bidCount = bidCount.coerceAtLeast(0),
        remainingSeconds = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        status = status,
        bookmarked = bookmarked,
        isHighestBidder = myBid?.isHighestBidder,
        myBidAmount = myBid?.amount?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
        imageUrls = detailedImages.ifEmpty { listOfNotNull(product?.thumbnailUrl?.takeIf(String::isNotBlank)) },
        sellerNickname = sellerSummary?.nickname,
        sellerRating = sellerSummary?.rating,
        sellerTradeCount = sellerSummary?.tradeCount ?: sellerSummary?.completedTradeCount
    )
}

internal fun com.ssafy.dib.data.remote.auction.AuctionBidSnapshotResponse.toDomain(now: Instant): AuctionBidSnapshot {
    val referenceTime = serverTime.toInstantOrNull() ?: now
    val remaining = scheduledEndAt.toInstantOrNull()
        ?.let { Duration.between(referenceTime, it).seconds.coerceAtLeast(0) }
        ?: auctionTime.coerceAtLeast(0)
    return AuctionBidSnapshot(
        auctionId = auctionId.idValue(),
        currentPrice = currentPrice.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        remainingSeconds = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        bidCount = bidCount.coerceAtLeast(0),
        bidderCount = bidderCount.coerceAtLeast(0),
        isHighestBidder = isHighestBidder
    )
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')

private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
