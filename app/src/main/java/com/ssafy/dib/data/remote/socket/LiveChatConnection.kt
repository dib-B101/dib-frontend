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
    private val eventParser: LiveSocketEventParser = LiveSocketEventParser(),
    private val sessionMemberId: () -> String?
) {
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-live-reconnect").apply { isDaemon = true }
    }
    private var liveBroadcastId = ""
    @Volatile private var connectedMemberId: String? = null
    private var subscribedAuctionId: String? = null
    @Volatile private var active = false
    private var reconnectAttempt = 0
    private var reconnectTask: ScheduledFuture<*>? = null
    @Volatile private var pendingBidCommand: SocketEnvelope? = null
    private val pendingChatMessages = linkedMapOf<String, SocketEnvelope>()
    private val updateFreshness = SocketUpdateFreshnessGate()
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
        updateFreshness.clear()
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
        connectedMemberId = sessionMemberId()
        onState(state)
        socket.connect(object : DibSocketListener {
            override fun onConnected() {
                if (!active) return
                if (connectedMemberId != sessionMemberId()) {
                    reconnectForSessionChange()
                    return
                }
                reconnectTask?.cancel(false)
                reconnectTask = null
                reconnectAttempt = 0
                onState(RealtimeConnectionState.Connected)
                socket.send(SocketCommands.subscribeLive(liveBroadcastId, updateFreshness.lastKnown(liveStreamKey())))
                subscribedAuctionId?.let { auctionId ->
                    socket.send(
                        SocketCommands.subscribeAuction(
                            auctionId,
                            updateFreshness.lastKnown(auctionStreamKey(auctionId))
                        )
                    )
                }
                pendingBidCommand?.let(socket::send)
                synchronized(this@LiveChatConnection) {
                    pendingChatMessages.values.forEach(socket::send)
                }
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                if (connectedMemberId != sessionMemberId()) {
                    reconnectForSessionChange()
                    return
                }
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
                        val pending = synchronized(this@LiveChatConnection) {
                            pendingChatMessages.remove(payload.commandId)
                        }
                        val acceptedMemberId = payload.memberId.idValueOrNull()
                        if (acceptedMemberId != null && acceptedMemberId != connectedMemberId) {
                            onError("로그인 계정과 Live 연결이 일치하지 않아요. 다시 연결합니다.")
                            reconnectForSessionChange()
                        } else {
                            acceptedLiveMessage(payload, pending, envelope.occurredAt)?.let(onMessage)
                        }
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
                eventParser.parse(envelope)?.takeIf(::shouldHandleUpdate)?.let { update ->
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
        if (connectedMemberId == null || connectedMemberId != sessionMemberId()) {
            reconnectForSessionChange()
            return false
        }
        val command = SocketCommands.sendLiveChat(liveBroadcastId, value)
        command.commandId?.let { pendingChatMessages[it] = command }
        if (!socket.send(command)) scheduleReconnect()
        return true
    }

    @Synchronized
    fun placeBid(auctionId: String, amount: Int): String? {
        if (!active || auctionId.isBlank() || amount <= 0 || pendingBidCommand != null) return null
        if (connectedMemberId == null || connectedMemberId != sessionMemberId()) {
            reconnectForSessionChange()
            return null
        }
        if (subscribedAuctionId != auctionId) subscribeAuction(auctionId)
        return SocketCommands.placeBid(auctionId, amount.toLong()).also { command ->
            pendingBidCommand = command
            if (!socket.send(command)) scheduleReconnect()
        }.commandId
    }

    /** Live 소켓은 유지한 채 현재 경매 토픽만 교체한다. */
    @Synchronized
    fun updateActiveAuction(auctionId: String?) {
        val next = auctionId?.takeIf(String::isNotBlank)
        if (next == subscribedAuctionId) return
        if (next == null) {
            subscribedAuctionId?.let(::unsubscribeAuction)
        } else {
            subscribeAuction(next)
        }
    }

    @Synchronized
    private fun subscribeAuction(auctionId: String) {
        if (subscribedAuctionId == auctionId) return
        subscribedAuctionId?.takeIf { it != auctionId }?.let { socket.send(SocketCommands.unsubscribeAuction(it)) }
        subscribedAuctionId = auctionId
        socket.send(
            SocketCommands.subscribeAuction(
                auctionId,
                updateFreshness.lastKnown(auctionStreamKey(auctionId))
            )
        )
    }

    @Synchronized
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
        connectedMemberId = null
        subscribedAuctionId = null
        pendingBidCommand = null
        pendingChatMessages.clear()
        socket.disconnect()
    }

    fun close() {
        stopSession()
        socket.close()
        reconnectExecutor.shutdownNow()
    }

    @Synchronized
    private fun reconnectForSessionChange() {
        pendingBidCommand = null
        pendingChatMessages.clear()
        socket.disconnect()
        scheduleReconnect()
    }

    private fun shouldHandleUpdate(update: LiveRealtimeUpdate): Boolean {
        if (update.bidAccepted != null) return true
        val auctionEvent = update.eventType in auctionStateEventTypes
        val streamKey = if (auctionEvent) {
            update.auctionId?.let(::auctionStreamKey)
        } else {
            (update.liveBroadcastId ?: liveBroadcastId.takeIf(String::isNotBlank))?.let { "live:$it" }
        } ?: return true
        return updateFreshness.shouldHandle(streamKey, update.occurredAt)
    }

    private fun liveStreamKey() = "live:$liveBroadcastId"
    private fun auctionStreamKey(auctionId: String) = "auction:$auctionId"
}

internal fun acceptedLiveMessage(
    accepted: LiveChatAcceptedPayload,
    pending: SocketEnvelope?,
    envelopeOccurredAt: String?
): LiveChatMessage? {
    if (pending?.commandId != accepted.commandId) return null
    // 구버전 서버 응답에는 작성자 ID가 없다. 이 경우 앱 프로필로 추측하지 않고
    // 서버의 LIVE_CHAT_MESSAGE_CREATED 방송을 기다린다.
    val memberId = accepted.memberId.idValueOrNull() ?: return null
    val pendingLiveId = pending.payload["liveBroadcastId"].idValueOrNull() ?: return null
    if (accepted.liveBroadcastId?.idValue()?.let { it != pendingLiveId } == true) return null
    val chattingId = accepted.liveChattingId?.idValue() ?: return null
    val content = pending.payload["content"].idValueOrNull() ?: return null
    val time = accepted.time ?: envelopeOccurredAt ?: return null
    return LiveChatMessage(chattingId, memberId, accepted.nickname, content, time)
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')

private fun kotlinx.serialization.json.JsonElement?.idValueOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

private val auctionStateEventTypes = setOf(
    SocketEventTypes.AUCTION_SNAPSHOT,
    SocketEventTypes.HIGHEST_BID_UPDATED,
    SocketEventTypes.AUCTION_EXTENDED,
    SocketEventTypes.AUCTION_ENDED,
    SocketEventTypes.LIVE_AUCTION_OPENED,
    SocketEventTypes.LIVE_AUCTION_STATUS_UPDATED,
    SocketEventTypes.LIVE_AUCTION_CLOSED
)
