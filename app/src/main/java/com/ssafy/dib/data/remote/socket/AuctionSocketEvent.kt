package com.ssafy.dib.data.remote.socket

import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

data class AuctionRealtimeUpdate(
    val auctionId: String,
    val currentPrice: Int? = null,
    val bidCount: Int? = null,
    val remainingSeconds: Int? = null,
    val status: String? = null,
    val isHighestBidder: Boolean? = null,
    val message: String? = null,
    val occurredAt: String? = null
)

class AuctionSocketEventParser(private val now: () -> Instant = Instant::now) {
    fun parse(envelope: SocketEnvelope): AuctionRealtimeUpdate? {
        val payload = envelope.payload
        val auctionId = payload.string("auctionId") ?: return null
        val occurredAt = payload.string("occurredAt") ?: envelope.occurredAt
        return when (envelope.eventType) {
            SocketEventTypes.AUCTION_SNAPSHOT -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                currentPrice = payload.int("currentPrice"),
                bidCount = payload.int("bidCount"),
                remainingSeconds = remaining(payload.string("endedAt"), payload.string("serverTime")),
                status = payload.string("status"),
                isHighestBidder = payload.obj("myBid")?.boolean("isHighestBidder"),
                occurredAt = occurredAt
            )
            SocketEventTypes.HIGHEST_BID_UPDATED -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                currentPrice = payload.int("currentPrice"),
                bidCount = payload.int("bidCount"),
                remainingSeconds = remaining(payload.string("endedAt")),
                occurredAt = occurredAt
            )
            SocketEventTypes.AUCTION_EXTENDED -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                remainingSeconds = remaining(payload.string("endedAt")),
                message = "마감 시간이 ${payload.int("extensionSeconds") ?: 15}초 연장됐어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.AUCTION_ENDED -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                currentPrice = payload.int("finalPrice"),
                remainingSeconds = 0,
                status = "ENDED",
                message = if (payload.string("result") == "SOLD") "경매가 낙찰됐어요." else "경매가 종료됐어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.BID_ACCEPTED -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                currentPrice = payload.int("newCurrentPrice"),
                remainingSeconds = remaining(payload.string("endedAt")),
                isHighestBidder = payload.boolean("isHighestBidder"),
                message = "입찰이 접수됐어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.BID_REJECTED -> AuctionRealtimeUpdate(
                auctionId = auctionId,
                currentPrice = payload.int("currentPrice"),
                remainingSeconds = remaining(payload.string("endedAt")),
                message = payload.string("message") ?: "입찰이 반영되지 않았어요.",
                occurredAt = occurredAt
            )
            else -> null
        }
    }

    private fun remaining(endedAt: String?, serverTime: String? = null): Int? {
        val end = endedAt.toInstantOrNull() ?: return null
        val reference = serverTime.toInstantOrNull() ?: now()
        return Duration.between(reference, end).seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.let { it.intOrNull ?: it.longOrNull?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() }
    private fun JsonObject.boolean(key: String): Boolean? = (get(key) as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
