package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.domain.live.LiveChatMessage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class LiveChatConnection(
    private val socket: DibWebSocketClient,
    private val codec: SocketCodec = SocketCodec()
) {
    private var liveBroadcastId = ""
    private var active = false
    private var onMessage: (LiveChatMessage) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var onState: (RealtimeConnectionState) -> Unit = {}

    fun start(
        liveBroadcastId: String,
        onMessage: (LiveChatMessage) -> Unit,
        onError: (String) -> Unit,
        onState: (RealtimeConnectionState) -> Unit
    ) {
        close()
        this.liveBroadcastId = liveBroadcastId
        this.onMessage = onMessage
        this.onError = onError
        this.onState = onState
        active = true
        onState(RealtimeConnectionState.Connecting)
        socket.connect(object : DibSocketListener {
            override fun onConnected() {
                if (!active) return
                onState(RealtimeConnectionState.Connected)
                socket.send(SocketCommands.subscribeLive(liveBroadcastId))
            }

            override fun onEvent(envelope: SocketEnvelope) {
                if (!active) return
                when (envelope.eventType) {
                    SocketEventTypes.LIVE_CHAT_MESSAGE_CREATED -> runCatching {
                        codec.decodePayload(envelope, LiveChatMessageCreatedPayload.serializer())
                    }.getOrNull()?.takeIf { it.liveBroadcastId.idValue() == liveBroadcastId }?.let { payload ->
                        onMessage(LiveChatMessage(payload.liveChattingId.idValue(), payload.memberId.idValue(), payload.nickname, payload.content, payload.time))
                    }
                    SocketEventTypes.ERROR -> runCatching {
                        codec.decodePayload(envelope, SocketErrorPayload.serializer())
                    }.getOrNull()?.let { onError(it.message) }
                }
            }

            override fun onFailure(cause: Throwable) {
                if (active) {
                    onState(RealtimeConnectionState.Disconnected)
                    onError("Live 채팅 연결이 끊어졌어요. 다시 시도해주세요.")
                }
            }
        })
    }

    fun send(content: String): Boolean {
        val value = content.trim()
        if (!active || value.isBlank() || value.length > 500) return false
        return socket.send(SocketCommands.sendLiveChat(liveBroadcastId, value))
    }

    fun close() {
        if (active && liveBroadcastId.isNotBlank()) socket.send(SocketCommands.unsubscribeLive(liveBroadcastId))
        active = false
        socket.disconnect()
    }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
