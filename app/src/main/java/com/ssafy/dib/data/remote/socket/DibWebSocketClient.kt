package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface DibSocketListener {
    fun onConnected() = Unit
    fun onEvent(envelope: SocketEnvelope)
    fun onMalformedMessage(rawMessage: String, cause: Throwable) = Unit
    fun onFailure(cause: Throwable) = Unit
    fun onClosed(code: Int, reason: String) = Unit
}

class DibWebSocketClient(
    private val config: NetworkConfig,
    private val accessTokenProvider: AccessTokenProvider,
    private val guestSessionProvider: GuestSessionProvider,
    private val codec: SocketCodec = SocketCodec(),
    private val eventGate: SocketEventGate = SocketEventGate(),
    private val client: OkHttpClient = OkHttpClient()
) {
    private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-websocket-heartbeat").apply { isDaemon = true }
    }
    private var socket: WebSocket? = null
    private var heartbeat: ScheduledFuture<*>? = null
    private val heartbeatMonitor = HeartbeatMonitor()

    @Synchronized
    fun connect(listener: DibSocketListener) {
        disconnect(1000, "reconnect")
        val webSocketUrl = try {
            config.requireWebSocketUrl()
        } catch (error: RuntimeException) {
            listener.onFailure(error)
            return
        }
        val request = Request.Builder().url(webSocketUrl).apply {
            accessTokenProvider.accessToken()?.takeIf(String::isNotBlank)?.let {
                header("Authorization", "Bearer $it")
            } ?: guestSessionProvider.guestSessionId()?.takeIf(String::isNotBlank)?.let {
                header("X-Guest-Session-Id", it)
            }
        }.build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (socket !== webSocket) return
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (socket !== webSocket) return
                runCatching { codec.decode(text) }
                    .onSuccess { envelope ->
                        if (envelope.eventType == SocketEventTypes.CONNECTED) {
                            val connected = runCatching {
                                codec.decodePayload(envelope, ConnectedPayload.serializer())
                            }.getOrNull()
                            scheduleHeartbeat(connected?.heartbeatIntervalSeconds ?: 20, webSocket, listener)
                        }
                        if (envelope.eventType == SocketEventTypes.PONG) heartbeatMonitor.onPong()
                        if (eventGate.shouldHandle(envelope)) listener.onEvent(envelope)
                    }
                    .onFailure { listener.onMalformedMessage(text, it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (socket !== webSocket) return
                socket = null
                stopHeartbeat()
                listener.onFailure(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (socket !== webSocket) return
                socket = null
                stopHeartbeat()
                listener.onClosed(code, reason)
            }
        })
    }

    fun send(envelope: SocketEnvelope): Boolean = socket?.send(codec.encode(envelope)) == true

    @Synchronized
    fun disconnect(code: Int = 1000, reason: String = "client closing") {
        stopHeartbeat()
        val current = socket
        socket = null
        current?.close(code, reason)
        eventGate.clear()
    }

    @Synchronized
    private fun scheduleHeartbeat(intervalSeconds: Long, expectedSocket: WebSocket, listener: DibSocketListener) {
        stopHeartbeat()
        heartbeatMonitor.reset()
        heartbeat = heartbeatExecutor.scheduleWithFixedDelay(
            {
                if (socket !== expectedSocket) return@scheduleWithFixedDelay
                if (!heartbeatMonitor.onPingDue()) {
                    synchronized(this) {
                        if (socket === expectedSocket) {
                            socket = null
                            stopHeartbeat()
                            expectedSocket.cancel()
                            listener.onFailure(SocketTimeoutException("WebSocket PONG was missed 3 times."))
                        }
                    }
                } else if (!expectedSocket.send(codec.encode(SocketCommands.ping()))) {
                    synchronized(this) {
                        if (socket === expectedSocket) {
                            socket = null
                            stopHeartbeat()
                            expectedSocket.cancel()
                            listener.onFailure(SocketTimeoutException("WebSocket heartbeat could not be sent."))
                        }
                    }
                }
            },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        )
    }

    @Synchronized
    private fun stopHeartbeat() {
        heartbeat?.cancel(false)
        heartbeat = null
        heartbeatMonitor.reset()
    }
}

internal class HeartbeatMonitor(private val maxMissedPongs: Int = 3) {
    private var missedPongs = 0

    @Synchronized
    fun onPingDue(): Boolean {
        if (missedPongs >= maxMissedPongs) return false
        missedPongs++
        return true
    }

    @Synchronized
    fun onPong() {
        missedPongs = 0
    }

    @Synchronized
    fun reset() {
        missedPongs = 0
    }
}
