package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.time.remainingWholeSeconds
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
    // 경매 종료 절대 시각(ISO-8601). 화면에서 이 값으로 매 틱 남은 초를 계산한다
    val endedAt: String? = null,
    val status: String? = null,
    val viewerCount: Int? = null,
    val commandId: String? = null,
    val bidAccepted: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val minAllowedAmount: Int? = null,
    val occurredAt: String? = null,
    // 낙찰자 정보는 백엔드가 아직 안 줄 수 있어 항상 null 허용 — 없으면 낙찰자 연출을 하지 않는다
    val winnerId: String? = null,
    val auctionResult: String? = null
)

class LiveSocketEventParser(
    private val now: () -> Instant = Instant::now,
    private val auctionParser: AuctionSocketEventParser = AuctionSocketEventParser(now)
) {
    fun parse(envelope: SocketEnvelope): LiveRealtimeUpdate? {
        val payload = envelope.payload
        auctionParser.parse(envelope)?.let { update ->
            return LiveRealtimeUpdate(
                eventType = update.eventType,
                auctionId = update.auctionId,
                currentPrice = update.currentPrice,
                bidCount = update.bidCount,
                isHighestBidder = update.isHighestBidder,
                remainingSeconds = update.remainingSeconds,
                // 절대 종료 시각. passthrough 이벤트도 payload 에 실려오면 그대로 넘긴다
                endedAt = payload.string("endedAt") ?: payload.string("scheduledEndAt"),
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
                    endedAt = auction?.string("endedAt") ?: auction?.string("scheduledEndAt"),
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
                endedAt = payload.string("endedAt") ?: run {
                    // 서버가 종료 시각을 안 주면 시작 시각 + 경매 시간으로 계산한다
                    val startedAt = payload.string("startedAt")
                    val auctionTime = payload.int("auctionTime")
                    if (startedAt != null && auctionTime != null) {
                        runCatching { Instant.parse(startedAt).plusSeconds(auctionTime.toLong()).toString() }.getOrNull()
                    } else null
                },
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
                endedAt = payload.string("endedAt"),
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
                    occurredAt = occurredAt,
                    auctionResult = result,
                    // winnerId 는 최상위 필드 또는 winner 객체 안에 올 수 있고, 필드 자체가 아직 없을 수도 있다
                    winnerId = payload.string("winnerId") ?: payload.obj("winner")?.string("memberId")
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
        return remainingWholeSeconds(reference, end)
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
