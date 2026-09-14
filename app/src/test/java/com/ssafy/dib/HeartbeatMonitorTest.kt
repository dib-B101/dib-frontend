package com.ssafy.dib

import com.ssafy.dib.data.remote.socket.HeartbeatMonitor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatMonitorTest {
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
