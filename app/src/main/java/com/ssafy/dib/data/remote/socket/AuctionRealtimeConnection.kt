package com.ssafy.dib.data.remote.socket

import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

enum class RealtimeConnectionState { Connecting, Connected, Reconnecting, Disconnected }

class AuctionRealtimeConnection(
    private val socket: DibWebSocketClient,
    private val parser: AuctionSocketEventParser = AuctionSocketEventParser()
) {
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-auction-reconnect").apply { isDaemon = true }
    }
    @Volatile private var active = false
    private var auctionId = ""
    private var reconnectAttempt = 0
    private var reconnectTask: ScheduledFuture<*>? = null
    private var pendingBidRetryTask: ScheduledFuture<*>? = null
    private var pendingBidRetryAttempt = 0
    private var lastKnownOccurredAt: String? = null
    @Volatile private var pendingBidCommand: SocketEnvelope? = null
    private var onUpdate: (AuctionRealtimeUpdate) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        auctionId: String,
        onUpdate: (AuctionRealtimeUpdate) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        stop()
        this.auctionId = auctionId
        this.onUpdate = onUpdate
        this.onState = onState
        active = true
        connect(RealtimeConnectionState.Connecting)
    }

    fun stop() {
        active = false
        reconnectTask?.cancel(false)
        reconnectTask = null
        pendingBidRetryTask?.cancel(false)
        pendingBidRetryTask = null
        pendingBidRetryAttempt = 0
        pendingBidCommand = null
        if (auctionId.isNotBlank()) socket.send(SocketCommands.unsubscribeAuction(auctionId))
        socket.disconnect()
        onState(RealtimeConnectionState.Disconnected)
    }

    fun close() {
        stop()
        reconnectExecutor.shutdownNow()
    }

    @Synchronized
    fun placeBid(amount: Int): String? {
        if (!active || amount <= 0 || pendingBidCommand != null) return null
        val command = SocketCommands.placeBid(auctionId, amount.toLong())
        pendingBidCommand = command
        sendPendingBid()
        return command.commandId
    }

    private fun connect(state: RealtimeConnectionState) {
        if (!active) return
        onState(state)
        socket.connect(object : DibSocketListener {
            override fun onConnected() {
                if (!active) return
                clearReconnectBackoff()
                onState(RealtimeConnectionState.Connected)
                socket.send(SocketCommands.subscribeAuction(auctionId, lastKnownOccurredAt))
                sendPendingBid()
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                parser.parse(envelope)?.takeIf { it.auctionId == auctionId }?.let { update ->
                    val isBidResult = update.eventType in setOf(SocketEventTypes.BID_ACCEPTED, SocketEventTypes.BID_REJECTED)
                    if (isBidResult && update.commandId == pendingBidCommand?.commandId) {
                        pendingBidCommand = null
                        pendingBidRetryTask?.cancel(false)
                        pendingBidRetryTask = null
                        pendingBidRetryAttempt = 0
                    }
                    if (isBidResult || isNewer(update.occurredAt)) {
                        update.occurredAt?.let { lastKnownOccurredAt = it }
                        onUpdate(update)
                    }
                }
                if (envelope.eventType == SocketEventTypes.SERVER_DRAINING) scheduleReconnect()
            }

            override fun onFailure(cause: Throwable) = scheduleReconnect()

            override fun onClosed(code: Int, reason: String) {
                if (active) scheduleReconnect()
            }
        })
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

    @Synchronized
    private fun clearReconnectBackoff() {
        reconnectTask?.cancel(false)
        reconnectTask = null
        reconnectAttempt = 0
    }

    @Synchronized
    private fun sendPendingBid() {
        val command = pendingBidCommand ?: return
        if (!socket.send(command)) scheduleReconnect()
        pendingBidRetryTask?.cancel(false)
        val retryDelaySeconds = (5L shl pendingBidRetryAttempt.coerceAtMost(2)).coerceAtMost(20)
        pendingBidRetryAttempt++
        pendingBidRetryTask = reconnectExecutor.schedule(
            { if (active && pendingBidCommand?.commandId == command.commandId) sendPendingBid() },
            retryDelaySeconds,
            TimeUnit.SECONDS
        )
    }

    private fun isNewer(candidate: String?): Boolean {
        val previous = lastKnownOccurredAt ?: return true
        if (candidate == null) return true
        val previousInstant = runCatching { Instant.parse(previous) }.getOrNull() ?: return true
        val candidateInstant = runCatching { Instant.parse(candidate) }.getOrNull() ?: return true
        return !candidateInstant.isBefore(previousInstant)
    }
}
