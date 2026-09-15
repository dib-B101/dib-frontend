package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.domain.order.OrderMessage
import com.ssafy.dib.domain.order.isOrderChatWritable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class OrderChatConnection(
    private val socket: DibWebSocketClient,
    private val codec: SocketCodec = SocketCodec()
) {
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-order-chat-reconnect").apply { isDaemon = true }
    }
    private var orderId = ""
    @Volatile private var active = false
    private var lastChattingId: String? = null
    private var reconnectAttempt = 0
    private var reconnectTask: ScheduledFuture<*>? = null
    @Volatile private var chatWritable = true
    private val pendingMessages = linkedMapOf<String, SocketEnvelope>()
    private var onMessage: (OrderMessage) -> Unit = {}
    private var onHistoryGap: () -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var onWritableChanged: (Boolean) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        orderId: String,
        lastChattingId: String?,
        onMessage: (OrderMessage) -> Unit,
        onHistoryGap: () -> Unit,
        onError: (String) -> Unit,
        onWritableChanged: (Boolean) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        stopSession()
        this.orderId = orderId
        this.lastChattingId = lastChattingId
        this.onMessage = onMessage
        this.onHistoryGap = onHistoryGap
        this.onError = onError
        this.onWritableChanged = onWritableChanged
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
                socket.send(SocketCommands.subscribeOrder(orderId, lastChattingId))
                synchronized(this@OrderChatConnection) {
                    pendingMessages.values.forEach(socket::send)
                }
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                when (envelope.eventType) {
                    SocketEventTypes.CHAT_MESSAGE_CREATED -> runCatching {
                        codec.decodePayload(envelope, ChatMessageCreatedPayload.serializer())
                    }.getOrNull()?.takeIf { it.orderId.idValue() == orderId }?.let { payload ->
                        val message = OrderMessage(payload.chattingId.idValue(), payload.memberId.idValue(), payload.content, payload.time)
                        lastChattingId = message.chattingId
                        onMessage(message)
                    }
                    SocketEventTypes.CHAT_MESSAGE_ACCEPTED -> runCatching {
                        codec.decodePayload(envelope, ChatMessageAcceptedPayload.serializer())
                    }.getOrNull()?.takeIf { it.orderId.idValue() == orderId }?.let { payload ->
                        synchronized(this@OrderChatConnection) { pendingMessages.remove(payload.commandId) }
                        lastChattingId = payload.chattingId.idValue()
                    }
                    SocketEventTypes.ORDER_SNAPSHOT -> {
                        val snapshotOrderId = envelope.payload["orderId"].idValueOrNull()
                        val serverLastChattingId = envelope.payload["lastChattingId"].idValueOrNull()
                        if (snapshotOrderId == orderId) {
                            chatWritable = isOrderChatWritable(envelope.payload["status"].idValueOrNull())
                            if (!chatWritable) synchronized(this@OrderChatConnection) { pendingMessages.clear() }
                            onWritableChanged(chatWritable)
                            if (serverLastChattingId != null && serverLastChattingId != lastChattingId) {
                                onHistoryGap()
                            }
                        }
                    }
                    SocketEventTypes.ERROR -> runCatching {
                        codec.decodePayload(envelope, SocketErrorPayload.serializer())
                    }.getOrNull()?.let { error ->
                        if (error.code == "CHAT_CLOSED") {
                            chatWritable = false
                            synchronized(this@OrderChatConnection) { pendingMessages.clear() }
                            onWritableChanged(false)
                        } else {
                            (error.commandId ?: envelope.commandId)?.let { commandId ->
                                synchronized(this@OrderChatConnection) { pendingMessages.remove(commandId) }
                            }
                        }
                        onError(error.message)
                    }
                    SocketEventTypes.SERVER_DRAINING -> scheduleReconnect()
                }
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
    fun send(content: String): Boolean {
        val value = content.trim()
        if (!active || !chatWritable || value.isBlank() || value.length > 500) return false
        val command = SocketCommands.sendChatMessage(orderId, value)
        command.commandId?.let { pendingMessages[it] = command }
        if (!socket.send(command)) scheduleReconnect()
        return true
    }

    fun updateLastChattingId(chattingId: String?) {
        lastChattingId = chattingId?.takeIf(String::isNotBlank)
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
        chatWritable = true
        pendingMessages.clear()
        socket.disconnect()
    }

    fun close() {
        stopSession()
        reconnectExecutor.shutdownNow()
    }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')

private fun kotlinx.serialization.json.JsonElement?.idValueOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
