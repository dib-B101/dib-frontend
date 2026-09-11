package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
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
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { codec.decode(text) }
                    .onSuccess { envelope ->
                        if (envelope.eventType == SocketEventTypes.CONNECTED) {
                            val connected = runCatching {
                                codec.decodePayload(envelope, ConnectedPayload.serializer())
                            }.getOrNull()
                            scheduleHeartbeat(connected?.heartbeatIntervalSeconds ?: 20)
                        }
                        if (eventGate.shouldHandle(envelope)) listener.onEvent(envelope)
                    }
                    .onFailure { listener.onMalformedMessage(text, it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                stopHeartbeat()
                listener.onFailure(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stopHeartbeat()
                listener.onClosed(code, reason)
            }
        })
    }

    fun send(envelope: SocketEnvelope): Boolean = socket?.send(codec.encode(envelope)) == true

    @Synchronized
    fun disconnect(code: Int = 1000, reason: String = "client closing") {
        stopHeartbeat()
        socket?.close(code, reason)
        socket = null
        eventGate.clear()
    }

    @Synchronized
    private fun scheduleHeartbeat(intervalSeconds: Long) {
        stopHeartbeat()
        heartbeat = heartbeatExecutor.scheduleWithFixedDelay(
            { send(SocketCommands.ping()) },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        )
    }

    @Synchronized
    private fun stopHeartbeat() {
        heartbeat?.cancel(false)
        heartbeat = null
    }
}
