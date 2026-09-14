package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.domain.notification.DomainNotification
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class DomainNotificationParser {
    fun parse(envelope: SocketEnvelope): DomainNotification? {
        if (envelope.eventType != SocketEventTypes.DOMAIN_NOTIFICATION) return null
        val payload = envelope.payload
        fun value(name: String): String? =
            (payload[name] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

        val eventId = envelope.eventId ?: value("eventId") ?: return null
        return DomainNotification(
            eventId = eventId,
            type = value("type") ?: return null,
            resourceType = value("resourceType") ?: return null,
            resourceId = value("resourceId") ?: return null,
            title = value("title") ?: return null,
            body = value("body") ?: return null,
            occurredAt = envelope.occurredAt ?: value("occurredAt") ?: return null
        )
    }
}

class DomainNotificationConnection(
    private val socket: DibWebSocketClient,
    private val parser: DomainNotificationParser = DomainNotificationParser()
) {
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-notification-reconnect").apply { isDaemon = true }
    }
    @Volatile private var active = false
    private var reconnectAttempt = 0
    private var reconnectTask: ScheduledFuture<*>? = null
    private var onNotification: (DomainNotification) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        onNotification: (DomainNotification) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        stopSession()
        this.onNotification = onNotification
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
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                parser.parse(envelope)?.let(onNotification)
                if (envelope.eventType == SocketEventTypes.SERVER_DRAINING) scheduleReconnect()
            }

            override fun onFailure(cause: Throwable) {
                if (active) scheduleReconnect()
            }

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

    private fun stopSession() {
        reconnectTask?.cancel(false)
        reconnectTask = null
        active = false
        socket.disconnect()
    }

    fun close() {
        stopSession()
        reconnectExecutor.shutdownNow()
    }
}
