package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.SocketCommands
import com.ssafy.dib.data.remote.socket.StompAction
import com.ssafy.dib.data.remote.socket.StompFrame
import com.ssafy.dib.data.remote.socket.StompRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// STOMP 전환 후 소켓 프로토콜 테스트 (기존 HeartbeatMonitor / WebSocketHandshakeGate 는 STOMP heart-beat · CONNECTED 프레임으로 대체됨)
class StompProtocolTest {
    @Test
    fun `encodes a SEND frame with destination and null terminator`() {
        val frame = StompFrame(StompFrame.SEND, linkedMapOf("destination" to "/app/auctions/1/bids"), """{"amount":1000}""")
        val encoded = frame.encode()

        assertTrue(encoded.startsWith("SEND\ndestination:/app/auctions/1/bids\n"))
        assertTrue(encoded.contains("content-length:15\n"))
        assertTrue(encoded.endsWith("\n\n{\"amount\":1000}" + StompFrame.NULL))
    }

    @Test
    fun `decodes a MESSAGE frame headers and body`() {
        val raw = "MESSAGE\ndestination:/topic/auctions/1\nsubscription:sub-1\ncontent-type:application/json\n\n{\"eventType\":\"HIGHEST_BID_UPDATED\"}" + StompFrame.NULL
        val frame = StompFrame.decode(raw)

        assertEquals(StompFrame.MESSAGE, frame.command)
        assertEquals("/topic/auctions/1", frame.header("destination"))
        assertEquals("{\"eventType\":\"HIGHEST_BID_UPDATED\"}", frame.body)
    }

    @Test
    fun `heart-beat frames are recognised`() {
        assertTrue(StompFrame.isHeartbeat("\n"))
        assertTrue(StompFrame.isHeartbeat("\r\n"))
    }

    @Test
    fun `legacy auction subscribe command maps to topic, personal queue and snapshot`() {
        val action = StompRouting.route(SocketCommands.subscribeAuction("17"))

        assertTrue(action is StompAction.Subscribe)
        assertEquals(
            listOf("/topic/auctions/17", "/user/queue/auction", "/app/auctions/17/snapshot"),
            (action as StompAction.Subscribe).destinations
        )
    }

    @Test
    fun `legacy place bid command maps to SEND with commandId and amount only`() {
        val command = SocketCommands.placeBid("17", 81000, "cmd-1")
        val action = StompRouting.route(command) as StompAction.Send

        assertEquals("/app/auctions/17/bids", action.destination)
        assertEquals(setOf("commandId", "amount"), action.body.keys)
    }

    @Test
    fun `ping is absorbed by STOMP heart-beat`() {
        assertEquals(StompAction.None, StompRouting.route(SocketCommands.ping()))
    }
}
