package com.ssafy.dib.feature.home

import com.ssafy.dib.domain.auction.AuctionSummary

/** Wireframe-only display data; these values do not represent live auctions. */
enum class ProductPhoto { Camera, Headphones, Placeholder }

data class HomeAuction(
    val id: String,
    val name: String,
    val price: Int,
    val bidCount: Int,
    val remainingSeconds: Int,
    val category: String,
    val photo: ProductPhoto,
    val productId: String = id,
    val pricePrefix: String = "현재가",
    val status: String = "ACTIVE",
    val isHighestBidder: Boolean? = null,
    val myBidAmount: Int? = null,
    val bookmarked: Boolean = false,
    val sellerMemberId: String = "seller01",
    val imageUrls: List<String> = emptyList(),
    val startPrice: Int = price,
    val sellerNickname: String? = null,
    val sellerRating: Double? = null,
    val sellerTradeCount: Int? = null,
    val productDescription: String? = null,
    val productCondition: String? = null,
    val productModelName: String? = null,
    val productReleaseYear: Int? = null,
    val productMarketPrice: Long? = null
) {
    val priceLabel: String get() = "%,d원".format(price)
    val meta: String get() = "입찰 ${bidCount}회 · ${remainingTimeLabel(remainingSeconds)} 남음"
}

internal val recommended = listOf(
    HomeAuction(id = "headphones", name = "무선 헤드폰", price = 52_000, bidCount = 7, remainingSeconds = 1_080, category = "디지털", photo = ProductPhoto.Headphones),
    HomeAuction(id = "sneakers", name = "빈티지 스니커즈", price = 81_000, bidCount = 4, remainingSeconds = 4_320, category = "패션", photo = ProductPhoto.Placeholder)
)

internal val deadlineAuction =
    HomeAuction(id = "camera", name = "빈티지 필름 카메라", price = 34_500, bidCount = 5, remainingSeconds = 204, category = "라이프", photo = ProductPhoto.Camera, startPrice = 20_000)

internal val popularAuctions = listOf(
    HomeAuction(id = "keyboard", name = "기계식 키보드", price = 48_000, bidCount = 12, remainingSeconds = 7_200, category = "디지털", photo = ProductPhoto.Placeholder),
    recommended[1],
    recommended[0]
)

internal val allAuctions = listOf(
    HomeAuction(id = "retro-console", name = "레트로 게임기", price = 63_000, bidCount = 9, remainingSeconds = 1_440, category = "디지털", photo = ProductPhoto.Placeholder),
    HomeAuction(id = "projector", name = "미니 빔프로젝터", price = 45_000, bidCount = 0, remainingSeconds = 7_200, category = "디지털", photo = ProductPhoto.Placeholder, pricePrefix = "시작가"),
    HomeAuction(id = "cross-bag", name = "가죽 크로스백", price = 28_500, bidCount = 6, remainingSeconds = 2_760, category = "패션", photo = ProductPhoto.Placeholder),
    HomeAuction(id = "lp-player", name = "LP 플레이어", price = 91_000, bidCount = 11, remainingSeconds = 10_800, category = "라이프", photo = ProductPhoto.Placeholder)
)

internal val allHomeAuctions = recommended + deadlineAuction + popularAuctions + allAuctions

internal fun AuctionSummary.toHomeAuction() = HomeAuction(
    id = auctionId,
    productId = productId,
    name = title,
    price = currentPrice.takeIf { it > 0 } ?: startPrice,
    bidCount = bidCount,
    remainingSeconds = remainingSeconds,
    category = categoryName,
    photo = ProductPhoto.Placeholder,
    pricePrefix = if (currentPrice > 0) "현재가" else "시작가",
    status = status,
    isHighestBidder = isHighestBidder,
    myBidAmount = myBidAmount,
    bookmarked = bookmarked,
    sellerMemberId = sellerMemberId,
    imageUrls = imageUrls,
    startPrice = startPrice,
    sellerNickname = sellerNickname,
    sellerRating = sellerRating,
    sellerTradeCount = sellerTradeCount,
    productDescription = productDescription,
    productCondition = productCondition,
    productModelName = productModelName,
    productReleaseYear = productReleaseYear,
    productMarketPrice = productMarketPrice
)

internal fun remainingTimeLabel(seconds: Int): String = when {
    seconds <= 0 -> "마감"
    seconds < 60 -> "${seconds}초"
    seconds < 3_600 -> "${seconds / 60}분"
    else -> "${seconds / 3_600}시간 ${seconds % 3_600 / 60}분"
}
