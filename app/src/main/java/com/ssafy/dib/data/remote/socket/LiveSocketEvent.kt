package com.ssafy.dib.data.remote.socket

import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

data class LiveRealtimeUpdate(
    val eventType: String,
    val liveBroadcastId: String? = null,
    val auctionId: String? = null,
    val productId: String? = null,
    val liveTitle: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val streamUrl: String? = null,
    val currentPrice: Int? = null,
    val startPrice: Int? = null,
    val bidCount: Int? = null,
    val isHighestBidder: Boolean? = null,
    val remainingSeconds: Int? = null,
    val status: String? = null,
    val viewerCount: Int? = null,
    val commandId: String? = null,
    val bidAccepted: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val minAllowedAmount: Int? = null,
    val occurredAt: String? = null
)

class LiveSocketEventParser(
    private val now: () -> Instant = Instant::now,
    private val auctionParser: AuctionSocketEventParser = AuctionSocketEventParser(now)
) {
    fun parse(envelope: SocketEnvelope): LiveRealtimeUpdate? {
        auctionParser.parse(envelope)?.let { update ->
            return LiveRealtimeUpdate(
                eventType = update.eventType,
                auctionId = update.auctionId,
                currentPrice = update.currentPrice,
                bidCount = update.bidCount,
                isHighestBidder = update.isHighestBidder,
                remainingSeconds = update.remainingSeconds,
                status = update.status,
                commandId = update.commandId,
                bidAccepted = when (update.eventType) {
                    SocketEventTypes.BID_ACCEPTED -> true
                    SocketEventTypes.BID_REJECTED -> false
                    else -> null
                },
                message = update.message,
                errorCode = update.errorCode,
                minAllowedAmount = update.minAllowedAmount,
                occurredAt = update.occurredAt
            )
        }

        val payload = envelope.payload
        val occurredAt = payload.string("occurredAt") ?: envelope.occurredAt
        return when (envelope.eventType) {
            SocketEventTypes.LIVE_SNAPSHOT -> {
                val live = payload.obj("liveBroadcast") ?: payload
                val auction = payload.obj("activeAuction")
                val product = payload.obj("product") ?: auction?.obj("product") ?: auction?.obj("productSummary")
                LiveRealtimeUpdate(
                    eventType = envelope.eventType,
                    liveBroadcastId = live.string("liveBroadcastId") ?: payload.string("liveBroadcastId"),
                    auctionId = auction?.string("auctionId"),
                    productId = product?.string("productId"),
                    liveTitle = live.string("title"),
                    title = product?.string("title"),
                    thumbnailUrl = product?.string("thumbnailUrl"),
                    streamUrl = live.string("streamUrl").playableMediaUrl(),
                    currentPrice = auction?.int("currentPrice"),
                    startPrice = auction?.int("startPrice"),
                    bidCount = auction?.int("bidCount"),
                    isHighestBidder = auction?.obj("myBid")?.boolean("isHighestBidder"),
                    remainingSeconds = remaining(
                        auction?.string("endedAt") ?: auction?.string("scheduledEndAt"),
                        payload.string("serverTime")
                    ),
                    status = auction?.string("status"),
                    viewerCount = payload.int("viewerCount") ?: live.int("viewCount"),
                    occurredAt = occurredAt ?: payload.string("serverTime")
                )
            }
            SocketEventTypes.LIVE_STARTED -> LiveRealtimeUpdate(
                eventType = envelope.eventType,
                liveBroadcastId = payload.string("liveBroadcastId"),
                liveTitle = payload.string("title"),
                streamUrl = payload.string("streamUrl").playableMediaUrl(),
                status = "LIVE",
                occurredAt = occurredAt ?: payload.string("startedAt")
            )
            SocketEventTypes.LIVE_AUCTION_OPENED -> LiveRealtimeUpdate(
                eventType = envelope.eventType,
                liveBroadcastId = payload.string("liveBroadcastId"),
                auctionId = payload.string("auctionId"),
                productId = payload.obj("product")?.string("productId"),
                title = payload.obj("product")?.string("title"),
                thumbnailUrl = payload.obj("product")?.string("thumbnailUrl"),
                currentPrice = payload.int("startPrice"),
                startPrice = payload.int("startPrice"),
                bidCount = 0,
                remainingSeconds = remaining(
                    endedAt = payload.string("endedAt"),
                    startedAt = payload.string("startedAt"),
                    auctionTimeSeconds = payload.int("auctionTime")
                ),
                status = "ACTIVE",
                occurredAt = occurredAt
            )
            SocketEventTypes.LIVE_AUCTION_STATUS_UPDATED -> LiveRealtimeUpdate(
                eventType = envelope.eventType,
                liveBroadcastId = payload.string("liveBroadcastId"),
                auctionId = payload.string("auctionId"),
                currentPrice = payload.int("currentPrice"),
                bidCount = payload.int("bidCount"),
                remainingSeconds = remaining(payload.string("endedAt")),
                status = payload.string("status"),
                occurredAt = occurredAt
            )
            SocketEventTypes.LIVE_AUCTION_CLOSED -> {
                val result = payload.string("result")
                LiveRealtimeUpdate(
                    eventType = envelope.eventType,
                    liveBroadcastId = payload.string("liveBroadcastId"),
                    auctionId = payload.string("auctionId"),
                    currentPrice = payload.int("finalPrice"),
                    remainingSeconds = 0,
                    status = if (result in setOf("CANCELLED", "CANCELED")) "CANCELED" else "ENDED",
                    message = if (result == "SOLD") "Live 경매가 낙찰됐어요." else "Live 경매가 종료됐어요.",
                    occurredAt = occurredAt
                )
            }
            SocketEventTypes.LIVE_VIEWER_COUNT_UPDATED -> LiveRealtimeUpdate(
                eventType = envelope.eventType,
                liveBroadcastId = payload.string("liveBroadcastId"),
                viewerCount = payload.int("viewerCount"),
                occurredAt = occurredAt
            )
            SocketEventTypes.LIVE_ENDED -> LiveRealtimeUpdate(
                eventType = envelope.eventType,
                liveBroadcastId = payload.string("liveBroadcastId"),
                viewerCount = payload.int("viewCount"),
                status = "ENDED",
                message = "Live 방송이 종료됐어요.",
                occurredAt = occurredAt
            )
            else -> null
        }
    }

    private fun remaining(
        endedAt: String?,
        serverTime: String? = null,
        startedAt: String? = null,
        auctionTimeSeconds: Int? = null
    ): Int? {
        val end = endedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?.plusSeconds(auctionTimeSeconds?.toLong() ?: return null)
            ?: return null
        val reference = serverTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: now()
        return Duration.between(reference, end).seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.let {
        it.intOrNull ?: it.longOrNull?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
    }
    private fun JsonObject.boolean(key: String): Boolean? = (get(key) as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
}

private fun String?.playableMediaUrl(): String? =
    this?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
