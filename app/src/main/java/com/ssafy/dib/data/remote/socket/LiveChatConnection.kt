package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.domain.live.LiveChatMessage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class LiveChatConnection(
    private val socket: DibWebSocketClient,
    private val codec: SocketCodec = SocketCodec(),
    private val eventParser: LiveSocketEventParser = LiveSocketEventParser()
) {
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-live-reconnect").apply { isDaemon = true }
    }
    private var liveBroadcastId = ""
    private var subscribedAuctionId: String? = null
    @Volatile private var active = false
    private var reconnectAttempt = 0
    private var reconnectTask: ScheduledFuture<*>? = null
    @Volatile private var pendingBidCommand: SocketEnvelope? = null
    private val pendingChatMessages = linkedMapOf<String, SocketEnvelope>()
    private var onMessage: (LiveChatMessage) -> Unit = {}
    private var onUpdate: (LiveRealtimeUpdate) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        liveBroadcastId: String,
        activeAuctionId: String?,
        onMessage: (LiveChatMessage) -> Unit,
        onUpdate: (LiveRealtimeUpdate) -> Unit,
        onError: (String) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        stopSession()
        this.liveBroadcastId = liveBroadcastId
        this.subscribedAuctionId = activeAuctionId
        this.onMessage = onMessage
        this.onUpdate = onUpdate
        this.onError = onError
        this.onState = onState
        active = true
        connect(RealtimeConnectionState.Connecting)
    }

    private fun connect(state: RealtimeConnectionState) {
        if (!active) return
        onState(state)
        socket.connect(object : DibSocketListener {
            override fun onConnected() {
                if (!active) return
                reconnectTask?.cancel(false)
                reconnectTask = null
                reconnectAttempt = 0
                onState(RealtimeConnectionState.Connected)
                socket.send(SocketCommands.subscribeLive(liveBroadcastId))
                subscribedAuctionId?.let { socket.send(SocketCommands.subscribeAuction(it)) }
                pendingBidCommand?.let(socket::send)
                synchronized(this@LiveChatConnection) {
                    pendingChatMessages.values.forEach(socket::send)
                }
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                when (envelope.eventType) {
                    SocketEventTypes.LIVE_CHAT_MESSAGE_CREATED -> runCatching {
                        codec.decodePayload(envelope, LiveChatMessageCreatedPayload.serializer())
                    }.getOrNull()?.takeIf { it.liveBroadcastId.idValue() == liveBroadcastId }?.let { payload ->
                        onMessage(LiveChatMessage(payload.liveChattingId.idValue(), payload.memberId.idValue(), payload.nickname, payload.content, payload.time))
                    }
                    SocketEventTypes.CHAT_ACCEPTED -> runCatching {
                        codec.decodePayload(envelope, LiveChatAcceptedPayload.serializer())
                    }.getOrNull()?.takeIf {
                        it.liveBroadcastId?.idValue()?.let { id -> id == liveBroadcastId } != false
                    }?.let { payload ->
                        synchronized(this@LiveChatConnection) { pendingChatMessages.remove(payload.commandId) }
                    }
                    SocketEventTypes.CHAT_REJECTED -> runCatching {
                        codec.decodePayload(envelope, LiveChatRejectedPayload.serializer())
                    }.getOrNull()?.takeIf {
                        it.liveBroadcastId?.idValue()?.let { id -> id == liveBroadcastId } != false
                    }?.let { payload ->
                        synchronized(this@LiveChatConnection) { pendingChatMessages.remove(payload.commandId) }
                        onError(payload.message)
                    }
                    SocketEventTypes.ERROR -> runCatching {
                        codec.decodePayload(envelope, SocketErrorPayload.serializer())
                    }.getOrNull()?.let { error ->
                        val commandId = error.commandId ?: envelope.commandId
                        val isPendingBidError = commandId != null && commandId == pendingBidCommand?.commandId
                        if (isPendingBidError) {
                            pendingBidCommand = null
                            onUpdate(
                                LiveRealtimeUpdate(
                                    eventType = SocketEventTypes.BID_REJECTED,
                                    auctionId = subscribedAuctionId,
                                    commandId = commandId,
                                    bidAccepted = false,
                                    message = error.message,
                                    errorCode = error.code,
                                    occurredAt = envelope.occurredAt
                                )
                            )
                        } else {
                            commandId?.let {
                                synchronized(this@LiveChatConnection) { pendingChatMessages.remove(it) }
                            }
                            onError(error.message)
                        }
                    }
                }
                eventParser.parse(envelope)?.let { update ->
                    when (update.eventType) {
                        SocketEventTypes.LIVE_AUCTION_OPENED -> update.auctionId?.let(::subscribeAuction)
                        SocketEventTypes.LIVE_AUCTION_CLOSED -> update.auctionId?.let(::unsubscribeAuction)
                    }
                    if (update.bidAccepted != null && update.commandId == pendingBidCommand?.commandId) {
                        pendingBidCommand = null
                    }
                    onUpdate(update)
                }
                if (envelope.eventType == SocketEventTypes.SERVER_DRAINING) scheduleReconnect()
            }

            override fun onFailure(cause: Throwable) {
                if (active) {
                    scheduleReconnect()
                }
            }

            override fun onClosed(code: Int, reason: String) {
                if (active) scheduleReconnect()
            }
        })
    }

    @Synchronized
    fun send(content: String): Boolean {
        val value = content.trim()
        if (!active || value.isBlank() || value.length > 500) return false
        val command = SocketCommands.sendLiveChat(liveBroadcastId, value)
        command.commandId?.let { pendingChatMessages[it] = command }
        if (!socket.send(command)) scheduleReconnect()
        return true
    }

    @Synchronized
    fun placeBid(auctionId: String, amount: Int): String? {
        if (!active || auctionId.isBlank() || amount <= 0 || pendingBidCommand != null) return null
        if (subscribedAuctionId != auctionId) subscribeAuction(auctionId)
        return SocketCommands.placeBid(auctionId, amount.toLong()).also { command ->
            pendingBidCommand = command
            if (!socket.send(command)) scheduleReconnect()
        }.commandId
    }

    private fun subscribeAuction(auctionId: String) {
        if (subscribedAuctionId == auctionId) return
        subscribedAuctionId?.takeIf { it != auctionId }?.let { socket.send(SocketCommands.unsubscribeAuction(it)) }
        subscribedAuctionId = auctionId
        socket.send(SocketCommands.subscribeAuction(auctionId))
    }

    private fun unsubscribeAuction(auctionId: String) {
        if (subscribedAuctionId == auctionId) {
            socket.send(SocketCommands.unsubscribeAuction(auctionId))
            subscribedAuctionId = null
        }
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (!active || reconnectTask?.isDone == false) return
        onState(RealtimeConnectionState.Reconnecting)
        val delaySeconds = (1L shl reconnectAttempt.coerceAtMost(5)).coerceAtMost(30)
        reconnectAttempt++
        reconnectTask = reconnectExecutor.schedule(
            { connect(RealtimeConnectionState.Reconnecting) },
            delaySeconds,
            TimeUnit.SECONDS
        )
    }

    private fun stopSession() {
        reconnectTask?.cancel(false)
        reconnectTask = null
        subscribedAuctionId?.let { socket.send(SocketCommands.unsubscribeAuction(it)) }
        if (active && liveBroadcastId.isNotBlank()) socket.send(SocketCommands.unsubscribeLive(liveBroadcastId))
        active = false
        subscribedAuctionId = null
        pendingBidCommand = null
        pendingChatMessages.clear()
        socket.disconnect()
    }

    fun close() {
        stopSession()
        reconnectExecutor.shutdownNow()
    }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
