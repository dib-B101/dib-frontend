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
    val pricePrefix: String = "현재가",
    val status: String = "ACTIVE",
    val isHighestBidder: Boolean? = null,
    val myBidAmount: Int? = null
) {
    val priceLabel: String get() = "%,d원".format(price)
    val meta: String get() = "입찰 ${bidCount}명 · ${remainingTimeLabel(remainingSeconds)} 남음"
}

internal val recommended = listOf(
    HomeAuction("headphones", "무선 헤드폰", 52_000, 7, 1_080, "디지털", ProductPhoto.Headphones),
    HomeAuction("sneakers", "빈티지 스니커즈", 81_000, 4, 4_320, "패션", ProductPhoto.Placeholder)
)

internal val deadlineAuction =
    HomeAuction("camera", "빈티지 필름 카메라", 34_500, 5, 204, "라이프", ProductPhoto.Camera)

internal val popularAuctions = listOf(
    HomeAuction("keyboard", "기계식 키보드", 48_000, 12, 7_200, "디지털", ProductPhoto.Placeholder),
    recommended[1],
    recommended[0]
)

internal val allAuctions = listOf(
    HomeAuction("retro-console", "레트로 게임기", 63_000, 9, 1_440, "디지털", ProductPhoto.Placeholder),
    HomeAuction("projector", "미니 빔프로젝터", 45_000, 0, 7_200, "디지털", ProductPhoto.Placeholder, "시작가"),
    HomeAuction("cross-bag", "가죽 크로스백", 28_500, 6, 2_760, "패션", ProductPhoto.Placeholder),
    HomeAuction("lp-player", "LP 플레이어", 91_000, 11, 10_800, "라이프", ProductPhoto.Placeholder)
)

internal val allHomeAuctions = recommended + deadlineAuction + popularAuctions + allAuctions

internal fun AuctionSummary.toHomeAuction() = HomeAuction(
    id = auctionId,
    name = title,
    price = currentPrice.takeIf { it > 0 } ?: startPrice,
    bidCount = bidCount,
    remainingSeconds = remainingSeconds,
    category = categoryName,
    photo = ProductPhoto.Placeholder,
    pricePrefix = if (currentPrice > 0) "현재가" else "시작가",
    status = status,
    isHighestBidder = isHighestBidder,
    myBidAmount = myBidAmount
)

internal fun remainingTimeLabel(seconds: Int): String = when {
    seconds <= 0 -> "마감"
    seconds < 60 -> "${seconds}초"
    seconds < 3_600 -> "${seconds / 60}분"
    else -> "${seconds / 3_600}시간 ${seconds % 3_600 / 60}분"
}
