package com.ssafy.dib.feature.auction

data class BidSubmission(val amount: Int)

// 서버 Auction.minNextBid와 같은 구간을 써야 안내 금액이 실제 입찰 가능 금액과 일치한다.
internal fun minimumBidAmount(currentPrice: Int, bidCount: Int): Int {
    if (bidCount == 0) return currentPrice
    val increment = when {
        currentPrice < 10_000 -> 500
        currentPrice < 100_000 -> 1_000
        currentPrice < 1_000_000 -> 5_000
        else -> 10_000
    }
    return (currentPrice.toLong() + increment).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

internal fun isValidBidAmount(amount: Int, minimum: Int): Boolean =
    amount >= minimum && amount > 0 && amount % 10 == 0
