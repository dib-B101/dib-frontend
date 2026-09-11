package com.ssafy.dib.data.remote.socket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SocketEnvelope(
    val eventType: String,
    val eventId: String? = null,
    val commandId: String? = null,
    val occurredAt: String? = null,
    val payload: JsonObject = JsonObject(emptyMap())
)

object SocketEventTypes {
    const val CONNECTED = "CONNECTED"
    const val PING = "PING"
    const val PONG = "PONG"
    const val SUBSCRIBE_AUCTION = "SUBSCRIBE_AUCTION"
    const val UNSUBSCRIBE_AUCTION = "UNSUBSCRIBE_AUCTION"
    const val SYNC_AUCTION = "SYNC_AUCTION"
    const val AUCTION_SNAPSHOT = "AUCTION_SNAPSHOT"
    const val PLACE_BID = "PLACE_BID"
    const val BID_ACCEPTED = "BID_ACCEPTED"
    const val BID_REJECTED = "BID_REJECTED"
    const val HIGHEST_BID_UPDATED = "HIGHEST_BID_UPDATED"
    const val AUCTION_EXTENDED = "AUCTION_EXTENDED"
    const val AUCTION_ENDED = "AUCTION_ENDED"
    const val SUBSCRIBE_ORDER = "SUBSCRIBE_ORDER"
    const val SEND_CHAT_MESSAGE = "SEND_CHAT_MESSAGE"
    const val CHAT_MESSAGE_CREATED = "CHAT_MESSAGE_CREATED"
    const val SUBSCRIBE_LIVE = "SUBSCRIBE_LIVE"
    const val UNSUBSCRIBE_LIVE = "UNSUBSCRIBE_LIVE"
    const val SEND_LIVE_CHAT = "SEND_LIVE_CHAT"
    const val DOMAIN_NOTIFICATION = "DOMAIN_NOTIFICATION"
    const val ERROR = "ERROR"
    const val SERVER_DRAINING = "SERVER_DRAINING"
}

@Serializable
data class ConnectedPayload(
    val connectionId: String,
    val memberId: String? = null,
    val guestSessionId: String? = null,
    val authenticated: Boolean,
    val heartbeatIntervalSeconds: Long = 20,
    val serverTime: String
)

@Serializable
data class BidAcceptedPayload(
    val commandId: String,
    val bidId: String,
    val auctionId: String,
    val amount: Long,
    val newCurrentPrice: Long,
    val isHighestBidder: Boolean,
    val endedAt: String,
    val acceptedAt: String
)

@Serializable
data class BidRejectedPayload(
    val commandId: String,
    val auctionId: String,
    val code: String,
    val message: String,
    val currentPrice: Long,
    val minAllowedAmount: Long,
    val endedAt: String,
    val rejectedAt: String
)
