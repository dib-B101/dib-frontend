package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.HeartbeatMonitor
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.data.remote.socket.WebSocketHandshakeGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatMonitorTest {
    @Test
    fun `application connection starts only after the first server handshake`() {
        val gate = WebSocketHandshakeGate()

        assertFalse(gate.accept(SocketEventTypes.PONG))
        assertTrue(gate.accept(SocketEventTypes.CONNECTED))
        assertFalse(gate.accept(SocketEventTypes.CONNECTED))
    }

    @Test
    fun `disconnects after three unanswered pings`() {
        val monitor = HeartbeatMonitor()

        assertTrue(monitor.onPingDue())
        assertTrue(monitor.onPingDue())
        assertTrue(monitor.onPingDue())
        assertFalse(monitor.onPingDue())
    }

    @Test
    fun `pong resets missed heartbeat count`() {
        val monitor = HeartbeatMonitor()

        assertTrue(monitor.onPingDue())
        assertTrue(monitor.onPingDue())
        monitor.onPong()

        assertTrue(monitor.onPingDue())
        assertTrue(monitor.onPingDue())
        assertTrue(monitor.onPingDue())
        assertFalse(monitor.onPingDue())
    }
}
