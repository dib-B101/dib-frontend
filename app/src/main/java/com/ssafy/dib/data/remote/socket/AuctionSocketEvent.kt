package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.time.remainingWholeSeconds
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

data class AuctionRealtimeUpdate(
    val eventType: String,
    val auctionId: String,
    val commandId: String? = null,
    val currentPrice: Int? = null,
    val bidCount: Int? = null,
    val remainingSeconds: Int? = null,
    val status: String? = null,
    val isHighestBidder: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val minAllowedAmount: Int? = null,
    val orderId: String? = null,
    val occurredAt: String? = null
)

class AuctionSocketEventParser(private val now: () -> Instant = Instant::now) {
    fun parse(envelope: SocketEnvelope): AuctionRealtimeUpdate? {
        val payload = envelope.payload
        val auctionId = payload.string("auctionId") ?: return null
        val occurredAt = payload.string("occurredAt") ?: envelope.occurredAt
        return when (envelope.eventType) {
            SocketEventTypes.AUCTION_SNAPSHOT -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                currentPrice = payload.int("currentPrice"),
                bidCount = payload.int("bidCount"),
                remainingSeconds = remaining(
                    payload.string("scheduledEndAt") ?: payload.string("endedAt"),
                    payload.string("serverTime")
                ),
                status = payload.string("status"),
                isHighestBidder = payload.obj("myBid")?.boolean("isHighestBidder"),
                occurredAt = occurredAt
            )
            SocketEventTypes.HIGHEST_BID_UPDATED -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                currentPrice = payload.int("currentPrice"),
                bidCount = payload.int("bidCount"),
                remainingSeconds = remaining(payload.string("endedAt"), occurredAt),
                occurredAt = occurredAt
            )
            SocketEventTypes.AUCTION_EXTENDED -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                remainingSeconds = remaining(payload.string("endedAt"), occurredAt),
                // 서버 규칙은 "연장" 이 아니라 남은 시간을 15초로 되돌리는 것이다 (Auction.EXTEND_WINDOW_SECONDS)
                message = "마감까지 남은 시간이 ${payload.int("extensionSeconds") ?: 15}초로 다시 맞춰졌어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.AUCTION_ENDED -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                currentPrice = payload.int("finalPrice"),
                remainingSeconds = 0,
                status = "ENDED",
                orderId = payload.string("orderId"),
                message = if (payload.string("result") == "SOLD") "경매가 낙찰됐어요." else "경매가 종료됐어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.BID_ACCEPTED -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                commandId = payload.string("commandId") ?: envelope.commandId,
                currentPrice = payload.int("newCurrentPrice"),
                remainingSeconds = remaining(payload.string("endedAt"), occurredAt),
                isHighestBidder = payload.boolean("isHighestBidder"),
                message = "입찰이 접수됐어요.",
                occurredAt = occurredAt
            )
            SocketEventTypes.BID_REJECTED -> AuctionRealtimeUpdate(
                eventType = envelope.eventType,
                auctionId = auctionId,
                commandId = payload.string("commandId") ?: envelope.commandId,
                currentPrice = payload.int("currentPrice"),
                remainingSeconds = remaining(payload.string("endedAt"), occurredAt),
                message = payload.string("message") ?: "입찰이 반영되지 않았어요.",
                errorCode = payload.string("code"),
                minAllowedAmount = payload.int("minAllowedAmount"),
                occurredAt = occurredAt
            )
            else -> null
        }
    }

    // 기준 시각은 서버가 준 값(serverTime 또는 이벤트 발생 시각)을 우선한다. 단말 시계로 재면 전송 지연만큼 항상 짧게 나온다
    private fun remaining(endedAt: String?, serverTime: String? = null): Int? {
        val end = endedAt.toInstantOrNull() ?: return null
        val reference = serverTime.toInstantOrNull() ?: now()
        return remainingWholeSeconds(reference, end)
    }

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.let { it.intOrNull ?: it.longOrNull?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() }
    private fun JsonObject.boolean(key: String): Boolean? = (get(key) as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
