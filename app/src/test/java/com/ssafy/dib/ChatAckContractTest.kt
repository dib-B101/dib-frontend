package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.ChatMessageAcceptedPayload
import com.ssafy.dib.data.remote.socket.LiveChatAcceptedPayload
import com.ssafy.dib.data.remote.socket.LiveChatRejectedPayload
import com.ssafy.dib.data.remote.socket.SocketCodec
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.data.remote.socket.SocketErrorPayload
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAckContractTest {
    private val codec = SocketCodec()

    @Test
    fun `decodes order chat accepted command`() {
        val payload = codec.decodePayload(
            SocketEnvelope(
                eventType = SocketEventTypes.CHAT_MESSAGE_ACCEPTED,
                commandId = "command-1",
                payload = buildJsonObject {
                    put("commandId", "command-1")
                    put("chattingId", 41)
                    put("orderId", 7)
                    put("time", "2026-09-14T10:00:00Z")
                }
            ),
            ChatMessageAcceptedPayload.serializer()
        )

        assertEquals("command-1", payload.commandId)
        assertEquals("41", payload.chattingId.toString())
        assertEquals("7", payload.orderId.toString())
    }

    @Test
    fun `decodes live chat accepted and rejected commands`() {
        val accepted = codec.decodePayload(
            SocketEnvelope(
                eventType = SocketEventTypes.CHAT_ACCEPTED,
                payload = buildJsonObject {
                    put("commandId", "command-2")
                    put("liveBroadcastId", "live-1")
                    put("liveChattingId", 19)
                }
            ),
            LiveChatAcceptedPayload.serializer()
        )
        val rejected = codec.decodePayload(
            SocketEnvelope(
                eventType = SocketEventTypes.CHAT_REJECTED,
                payload = buildJsonObject {
                    put("commandId", "command-3")
                    put("code", "CHAT_RATE_LIMITED")
                    put("message", "채팅이 너무 빠릅니다.")
                    put("retryable", true)
                }
            ),
            LiveChatRejectedPayload.serializer()
        )

        assertEquals("command-2", accepted.commandId)
        assertEquals("CHAT_RATE_LIMITED", rejected.code)
        assertEquals(true, rejected.retryable)
    }

    @Test
    fun `decodes command error used to release pending requests`() {
        val error = codec.decodePayload(
            SocketEnvelope(
                eventType = SocketEventTypes.ERROR,
                commandId = "command-4",
                payload = buildJsonObject {
                    put("code", "AUCTION_NOT_ACTIVE")
                    put("message", "진행 중인 경매가 아닙니다.")
                    put("retryable", false)
                }
            ),
            SocketErrorPayload.serializer()
        )

        assertEquals(null, error.commandId)
        assertEquals("AUCTION_NOT_ACTIVE", error.code)
        assertEquals(false, error.retryable)
    }
}
