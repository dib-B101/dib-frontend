package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auction.AuctionDto
import com.ssafy.dib.data.remote.auction.AuctionRemoteDataSource
import com.ssafy.dib.domain.auction.AuctionRepository
import com.ssafy.dib.domain.auction.AuctionSummary
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class AuctionRepositoryImpl(
    private val remote: AuctionRemoteDataSource,
    private val now: () -> Instant = Instant::now
) : AuctionRepository {
    override fun getGeneralAuctions(
        size: Int,
        categoryId: String?,
        status: String,
        minPrice: Long?,
        maxPrice: Long?,
        sort: String?
    ): ApiResult<List<AuctionSummary>> =
        when (val result = remote.getGeneralAuctions(size, categoryId, status, minPrice, maxPrice, sort)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map { it.toDomain(now()) }, result.status)
            is ApiResult.Failure -> result
        }

    override fun getAuction(auctionId: String): ApiResult<AuctionSummary> =
        when (val result = remote.getAuction(auctionId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(now()), result.status)
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
        imageUrls = detailedImages.ifEmpty { listOfNotNull(product?.thumbnailUrl?.takeIf(String::isNotBlank)) }
    )
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')

private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
