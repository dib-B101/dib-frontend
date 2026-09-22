package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auction.AuctionDto
import com.ssafy.dib.data.remote.auction.AuctionRemoteDataSource
import com.ssafy.dib.domain.auction.AuctionRepository
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.auction.AuctionPage
import com.ssafy.dib.domain.auction.SaleHistoryItem
import com.ssafy.dib.domain.auction.SaleHistoryPage
import com.ssafy.dib.domain.auction.AuctionCommandResult
import com.ssafy.dib.domain.auction.PlacedBid
import com.ssafy.dib.domain.auction.SellerAuction
import com.ssafy.dib.domain.auction.BidHistoryItem
import com.ssafy.dib.domain.auction.BidHistoryPage
import com.ssafy.dib.domain.auction.AuctionBidHistoryItem
import com.ssafy.dib.domain.auction.AuctionBidHistoryPage
import com.ssafy.dib.domain.auction.HomeRecommendations
import com.ssafy.dib.domain.auction.RecommendedLive
import com.ssafy.dib.domain.auction.AuctionBidSnapshot
import com.ssafy.dib.domain.auction.matchesAuctionStatusFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class AuctionRepositoryImpl(
    private val remote: AuctionRemoteDataSource,
    private val now: () -> Instant = Instant::now
) : AuctionRepository {
    override fun getMySales(auctionStatus: String?, cursor: String?, size: Int): ApiResult<SaleHistoryPage> =
        when (val result = remote.getMySales(auctionStatus, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                SaleHistoryPage(
                    items = result.value.items.map { item ->
                        SaleHistoryItem(
                            auction = item.auction.copy(product = item.auction.product ?: item.product).toDomain(now()),
                            orderId = item.order?.orderId?.idValue(),
                            orderStatus = item.order?.status
                        )
                    },
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getAuctions(scope: String, status: String, mine: Boolean, cursor: String?, size: Int): ApiResult<AuctionPage> =
        when (val result = remote.getAuctions(scope, status, mine, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionPage(
                    items = result.value.items
                        .filterForBackendContract(scope = scope, status = status)
                        .take(size)
                        .map { it.toDomain(now()) },
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
                    items = result.value.items.asSequence()
                        .filter { it.liveBroadcastId == null }
                        // OPEN 은 서버가 진행 중+예정을 함께 내려주는 값이라 둘 다 통과시킨다
                        .filter { status.isBlank() || it.status.matchesAuctionStatusFilter(status) }
                        .filter { categoryId.isNullOrBlank() || it.categoryId?.idValue() == categoryId }
                        .filter { minPrice == null || (it.currentPrice ?: 0L) >= minPrice }
                        .filter { maxPrice == null || (it.currentPrice ?: 0L) <= maxPrice }
                        .let { items ->
                            when (sort?.uppercase()) {
                                "PRICE_ASC" -> items.sortedBy { it.currentPrice ?: 0L }
                                "PRICE_DESC" -> items.sortedByDescending { it.currentPrice ?: 0L }
                                "ENDING_SOON" -> items.sortedBy { it.endedAt.orEmpty() }
                                else -> items
                            }
                        }
                        .take(size)
                        .map { it.toDomain(now()) }
                        .toList(),
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

    override fun getSellerAuctions(sellerId: String): ApiResult<List<SellerAuction>> =
        when (val result = remote.getSellerAuctions(sellerId)) {
            is ApiResult.Success -> ApiResult.Success(
                result.value.map { dto ->
                    SellerAuction(
                        auctionId = dto.auctionId?.idValue().orEmpty(),
                        productId = dto.productId?.idValue().orEmpty(),
                        startPrice = (dto.startPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                        currentPrice = (dto.currentPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                        bidCount = (dto.bidCount ?: 0).coerceAtLeast(0),
                        status = dto.status.orEmpty(),
                        auctionTimeSeconds = (dto.auctionTime ?: 0L).coerceAtLeast(0),
                        title = dto.productTitle?.takeIf(String::isNotBlank),
                        thumbnailUrl = dto.thumbnailUrl?.takeIf(String::isNotBlank)
                    )
                },
                result.status
            )
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
                            scheduledAt = live.scheduledAt,
                            itemCount = live.itemCount,
                            firstItemTitle = live.firstItemTitle,
                            firstItemThumbnailUrl = live.firstItemThumbnailUrl
                        )
                    },
                    generalItems = result.value.generalItems.take(size).map { it.toDomain(now()) }
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getBookmarks(cursor: String?, size: Int): ApiResult<AuctionPage> =
        when (val result = remote.getBookmarks(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionPage(
                    items = result.value.items.map { it.copy(bookmarked = true).toDomain(now()) },
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
                            createdAt = bid.createdAt,
                            productId = bid.productId?.idValue(),
                            title = bid.productTitle?.takeIf(String::isNotBlank),
                            thumbnailUrl = bid.thumbnailUrl?.takeIf(String::isNotBlank),
                            auctionStatus = bid.auctionStatus,
                            currentPrice = bid.currentPrice?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
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
                            createdAt = bid.createdAt,
                            bidderNickname = bid.bidderNickname?.takeIf(String::isNotBlank)
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

    override fun setBookmark(productId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<Boolean> =
        when (val result = remote.setBookmark(productId, bookmarked, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(bookmarked, result.status)
            is ApiResult.Failure -> result
        }

    override fun placeBid(auctionId: String, amount: Int, idempotencyKey: String): ApiResult<PlacedBid> =
        when (val result = remote.placeBid(auctionId, amount, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(
                PlacedBid(
                    currentPrice = (result.value.currentPrice ?: amount.toLong()).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    bidCount = result.value.bidCount ?: 0
                ),
                result.status
            )
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

    override fun relistAuction(auctionId: String, idempotencyKey: String): ApiResult<AuctionCommandResult> =
        when (val result = remote.relistAuction(auctionId, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(
                AuctionCommandResult(result.value.auctionId?.idValue() ?: auctionId, result.value.message),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun startAuction(auctionId: String, idempotencyKey: String, startPrice: Long?, auctionTime: Long?): ApiResult<String> = when (val result = remote.startAuction(auctionId, idempotencyKey, startPrice, auctionTime)) {
        is ApiResult.Success -> ApiResult.Success(result.value.message, result.status)
        is ApiResult.Failure -> result
    }
}

internal fun AuctionDto.toDomain(now: Instant): AuctionSummary {
    val referenceTime = serverTime.toInstantOrNull() ?: now
    val endTime = scheduledEndAt.toInstantOrNull() ?: endedAt.toInstantOrNull()
    val remaining = endTime?.let { Duration.between(referenceTime, it).seconds.coerceAtLeast(0) }
        ?: (auctionTime ?: 0L).coerceAtLeast(0)
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
        currentPrice = (currentPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        startPrice = (startPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        currentPriceOrNull = currentPrice?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
        startPriceOrNull = startPrice?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
        bidCount = bidCount.coerceAtLeast(0),
        auctionTimeSeconds = (auctionTime ?: 0L).coerceAtLeast(0),
        remainingSeconds = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        endedAt = scheduledEndAt ?: endedAt,
        status = status,
        bookmarked = bookmarked,
        isHighestBidder = myBid?.isHighestBidder,
        myBidAmount = myBid?.amount?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
        myOrderId = myOrderId?.idValue()?.takeIf { it.isNotBlank() && it != "null" },
        imageUrls = detailedImages.ifEmpty { listOfNotNull(product?.thumbnailUrl?.takeIf(String::isNotBlank)) },
        sellerNickname = sellerSummary?.nickname,
        sellerProfileImageUrl = sellerSummary?.profileImageUrl,
        sellerRating = sellerSummary?.rating,
        sellerReviewCount = sellerSummary?.reviewCount,
        sellerTradeCount = sellerSummary?.tradeCount ?: sellerSummary?.completedTradeCount,
        productDescription = product?.description,
        productCondition = product?.condition,
        productModelName = product?.modelName,
        productReleaseYear = product?.releaseYear,
        productMarketPrice = product?.marketPrice
    )
}

internal fun com.ssafy.dib.data.remote.auction.AuctionBidSnapshotResponse.toDomain(now: Instant): AuctionBidSnapshot {
    val referenceTime = serverTime.toInstantOrNull() ?: now
    val remaining = scheduledEndAt.toInstantOrNull()
        ?.let { Duration.between(referenceTime, it).seconds.coerceAtLeast(0) }
        ?: auctionTime.coerceAtLeast(0)
    return AuctionBidSnapshot(
        auctionId = auctionId.idValue(),
        status = status,
        currentPrice = currentPrice.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        remainingSeconds = if (status.equals("ACTIVE", ignoreCase = true)) remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else 0,
        bidCount = bidCount.coerceAtLeast(0),
        bidderCount = bidderCount.coerceAtLeast(0),
        isHighestBidder = isHighestBidder
    )
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')

private fun List<AuctionDto>.filterForBackendContract(scope: String, status: String): List<AuctionDto> = filter { auction ->
    val scopeMatches = when (scope.uppercase()) {
        "GENERAL" -> auction.liveBroadcastId == null
        "LIVE" -> auction.liveBroadcastId != null
        else -> true
    }
    scopeMatches && (status.isBlank() || auction.status.equals(status, ignoreCase = true))
}

private fun String?.toInstantOrNull(): Instant? = this?.let { value ->
    runCatching { Instant.parse(value) }.getOrElse {
        runCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
    }
}
