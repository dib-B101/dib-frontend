package com.ssafy.dib.data.remote.socket

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SocketCommands {
    fun ping(eventId: String = UUID.randomUUID().toString()) = SocketEnvelope(
        eventType = SocketEventTypes.PING,
        eventId = eventId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject { put("clientTime", Instant.now().toString()) }
    )

    fun subscribeAuction(
        auctionId: String,
        lastKnownOccurredAt: String? = null,
        guestSessionId: String? = null,
        commandId: String = UUID.randomUUID().toString()
    ) = SocketEnvelope(
        eventType = SocketEventTypes.SUBSCRIBE_AUCTION,
        commandId = commandId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject {
            put("commandId", commandId)
            put("auctionId", auctionId)
            lastKnownOccurredAt?.let { put("lastKnownOccurredAt", it) }
            guestSessionId?.let { put("guestSessionId", it) }
        }
    )

    fun syncAuction(
        auctionId: String,
        lastKnownOccurredAt: String?,
        guestSessionId: String? = null,
        commandId: String = UUID.randomUUID().toString()
    ) = subscribeAuction(auctionId, lastKnownOccurredAt, guestSessionId, commandId)
        .copy(eventType = SocketEventTypes.SYNC_AUCTION)

    fun unsubscribeAuction(
        auctionId: String,
        commandId: String = UUID.randomUUID().toString()
    ) = SocketEnvelope(
        eventType = SocketEventTypes.UNSUBSCRIBE_AUCTION,
        commandId = commandId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject {
            put("commandId", commandId)
            put("auctionId", auctionId)
        }
    )

    fun placeBid(
        auctionId: String,
        amount: Long,
        commandId: String = UUID.randomUUID().toString()
    ): SocketEnvelope {
        require(amount > 0) { "Bid amount must be positive." }
        return SocketEnvelope(
            eventType = SocketEventTypes.PLACE_BID,
            commandId = commandId,
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("commandId", commandId)
                put("auctionId", auctionId)
                put("amount", amount)
                put("sentAt", Instant.now().toString())
            }
        )
    }

    fun subscribeOrder(
        orderId: String,
        lastChattingId: String? = null,
        commandId: String = UUID.randomUUID().toString()
    ) = SocketEnvelope(
        eventType = SocketEventTypes.SUBSCRIBE_ORDER,
        commandId = commandId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject {
            put("commandId", commandId)
            put("orderId", orderId)
            lastChattingId?.let { put("lastChattingId", it) }
        }
    )

    fun sendChatMessage(
        orderId: String,
        content: String,
        commandId: String = UUID.randomUUID().toString()
    ): SocketEnvelope {
        require(content.isNotBlank()) { "Chat message must not be blank." }
        return SocketEnvelope(
            eventType = SocketEventTypes.SEND_CHAT_MESSAGE,
            commandId = commandId,
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("commandId", commandId)
                put("orderId", orderId)
                put("content", content)
                put("clientSentAt", Instant.now().toString())
            }
        )
    }

    fun subscribeLive(
        liveBroadcastId: String,
        lastKnownOccurredAt: String? = null,
        guestSessionId: String? = null,
        commandId: String = UUID.randomUUID().toString()
    ) = SocketEnvelope(
        eventType = SocketEventTypes.SUBSCRIBE_LIVE,
        commandId = commandId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject {
            put("commandId", commandId)
            put("liveBroadcastId", liveBroadcastId)
            lastKnownOccurredAt?.let { put("lastKnownOccurredAt", it) }
            guestSessionId?.let { put("guestSessionId", it) }
        }
    )

    fun unsubscribeLive(
        liveBroadcastId: String,
        commandId: String = UUID.randomUUID().toString()
    ) = SocketEnvelope(
        eventType = SocketEventTypes.UNSUBSCRIBE_LIVE,
        commandId = commandId,
        occurredAt = Instant.now().toString(),
        payload = buildJsonObject {
            put("commandId", commandId)
            put("liveBroadcastId", liveBroadcastId)
        }
    )

    fun sendLiveChat(
        liveBroadcastId: String,
        content: String,
        commandId: String = UUID.randomUUID().toString()
    ): SocketEnvelope {
        require(content.isNotBlank()) { "Live chat message must not be blank." }
        require(content.length <= 500) { "Live chat message must be 500 characters or fewer." }
        return SocketEnvelope(
            eventType = SocketEventTypes.SEND_LIVE_CHAT,
            commandId = commandId,
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("commandId", commandId)
                put("liveBroadcastId", liveBroadcastId)
                put("content", content)
            }
        )
    }
}
