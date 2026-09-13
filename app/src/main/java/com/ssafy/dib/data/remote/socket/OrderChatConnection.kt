package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.domain.order.OrderMessage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class OrderChatConnection(
    private val socket: DibWebSocketClient,
    private val codec: SocketCodec = SocketCodec()
) {
    private var orderId = ""
    private var active = false
    private var onMessage: (OrderMessage) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        orderId: String,
        lastChattingId: String?,
        onMessage: (OrderMessage) -> Unit,
        onError: (String) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        close()
        this.orderId = orderId
        this.onMessage = onMessage
        this.onError = onError
        this.onState = onState
        active = true
        onState(RealtimeConnectionState.Connecting)
        socket.connect(object : DibSocketListener {
            override fun onConnected() {
                if (!active) return
                onState(RealtimeConnectionState.Connected)
                socket.send(SocketCommands.subscribeOrder(orderId, lastChattingId))
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                when (envelope.eventType) {
                    SocketEventTypes.CHAT_MESSAGE_CREATED -> runCatching {
                        codec.decodePayload(envelope, ChatMessageCreatedPayload.serializer())
                    }.getOrNull()?.takeIf { it.orderId.idValue() == orderId }?.let { payload ->
                        onMessage(OrderMessage(payload.chattingId.idValue(), payload.memberId.idValue(), payload.content, payload.time))
                    }
                    SocketEventTypes.ERROR -> runCatching {
                        codec.decodePayload(envelope, SocketErrorPayload.serializer())
                    }.getOrNull()?.let { onError(it.message) }
                }
            }

            override fun onFailure(cause: Throwable) {
                if (active) {
                    onState(RealtimeConnectionState.Disconnected)
                    onError("채팅 연결이 끊어졌어요. 화면을 다시 열어주세요.")
                }
            }
        })
    }

    fun send(content: String): Boolean {
        val value = content.trim()
        if (!active || value.isBlank()) return false
        return socket.send(SocketCommands.sendChatMessage(orderId, value))
    }

    fun close() {
        active = false
        socket.disconnect()
    }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
