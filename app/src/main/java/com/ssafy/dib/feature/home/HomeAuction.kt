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
    val meta: String get() = "${if (bidCount == 0) "첫 입찰 대기 중" else "입찰 ${bidCount}회"} · ${remainingTimeLabel(remainingSeconds)} 남음"
}

internal val recommended = listOf(
    HomeAuction(id = "headphones", name = "노이즈 캔슬링 헤드폰", price = 52_000, bidCount = 7, remainingSeconds = 1_080, category = "디지털", photo = ProductPhoto.Headphones, startPrice = 35_000, productDescription = "깨끗하게 사용한 무선 헤드폰입니다. 이어 패드와 충전 케이블을 함께 보내드려요.", productCondition = "LIKE_NEW", productModelName = "DIB Studio H1"),
    HomeAuction(id = "sneakers", name = "필름 카메라 입문 세트", price = 81_000, bidCount = 4, remainingSeconds = 4_320, category = "취미", photo = ProductPhoto.Camera, startPrice = 60_000, productDescription = "필름 촬영을 바로 시작할 수 있도록 스트랩과 보호 케이스를 함께 구성했습니다.", productCondition = "GOOD", productModelName = "Classic 35")
)

internal val deadlineAuction =
    HomeAuction(id = "camera", name = "빈티지 필름 카메라", price = 34_500, bidCount = 5, remainingSeconds = 204, category = "취미", photo = ProductPhoto.Camera, startPrice = 20_000, productDescription = "셔터와 노출계를 점검한 필름 카메라입니다. 생활 사용감은 사진에서 확인해주세요.", productCondition = "GOOD", productModelName = "Classic 35")

internal val popularAuctions = listOf(
    HomeAuction(id = "keyboard", name = "기계식 키보드", price = 48_000, bidCount = 12, remainingSeconds = 7_200, category = "디지털", photo = ProductPhoto.Placeholder),
    recommended[1],
    recommended[0]
)

internal val allAuctions = listOf(
    HomeAuction(id = "retro-console", name = "레트로 카메라 바디", price = 63_000, bidCount = 9, remainingSeconds = 1_440, category = "취미", photo = ProductPhoto.Camera, startPrice = 42_000, productDescription = "보관 상태가 좋은 수동 필름 카메라 바디입니다.", productCondition = "GOOD"),
    HomeAuction(id = "projector", name = "오버이어 헤드폰", price = 45_000, bidCount = 0, remainingSeconds = 7_200, category = "디지털", photo = ProductPhoto.Headphones, pricePrefix = "시작가", productDescription = "착용감이 편안한 오버이어 헤드폰입니다. 실내에서만 사용했어요.", productCondition = "LIKE_NEW"),
    HomeAuction(id = "cross-bag", name = "클래식 필름 카메라", price = 28_500, bidCount = 6, remainingSeconds = 2_760, category = "취미", photo = ProductPhoto.Camera, startPrice = 18_000, productDescription = "가볍게 들고 다니기 좋은 필름 카메라입니다.", productCondition = "NORMAL"),
    HomeAuction(id = "lp-player", name = "프리미엄 무선 헤드폰", price = 91_000, bidCount = 11, remainingSeconds = 10_800, category = "디지털", photo = ProductPhoto.Headphones, startPrice = 55_000, productDescription = "풍부한 저음과 노이즈 캔슬링을 지원하는 무선 헤드폰입니다.", productCondition = "GOOD")
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
    seconds % 3_600 < 60 -> "${seconds / 3_600}시간"
    else -> "${seconds / 3_600}시간 ${seconds % 3_600 / 60}분"
}
