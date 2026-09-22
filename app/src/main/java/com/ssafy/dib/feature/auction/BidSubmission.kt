package com.ssafy.dib.feature.auction

data class BidSubmission(val amount: Int)

// 입찰 금액은 서버(TradeInputValidator)가 10원 단위만 받는다. 어떤 값을 계산하든 마지막엔 이 함수로 올려 맞춘다
internal fun roundUpToBidUnit(amount: Int): Int =
    if (amount % 10 == 0) amount else ((amount.toLong() / 10 + 1) * 10).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

// 서버 Auction.minNextBid와 같은 구간을 써야 안내 금액이 실제 입찰 가능 금액과 일치한다.
// 시작가가 1001원처럼 어긋난 예전 데이터도 있어, 최소 금액은 항상 10원 단위로 올려서 돌려준다
internal fun minimumBidAmount(currentPrice: Int, bidCount: Int): Int {
    if (bidCount == 0) return roundUpToBidUnit(currentPrice)
    val increment = when {
        currentPrice < 10_000 -> 500
        currentPrice < 100_000 -> 1_000
        currentPrice < 1_000_000 -> 5_000
        else -> 10_000
    }
    return roundUpToBidUnit((currentPrice.toLong() + increment).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
}

// +1,000 같은 빠른 입력 버튼. 기준 금액이 10원 단위가 아니어도 결과는 항상 10원 단위·최소 금액 이상이 되게 한다
internal fun steppedBidAmount(base: Int, increment: Int, minimum: Int): Int =
    roundUpToBidUnit((base.coerceAtLeast(minimum).toLong() + increment).coerceAtMost(999_999_990L).toInt())

internal fun isValidBidAmount(amount: Int, minimum: Int): Boolean =
    amount >= minimum && amount > 0 && amount % 10 == 0
