package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
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

// STOMP over WebSocket 클라이언트 (백엔드 Spring STOMP /ws).
//  - 소켓이 열리면 CONNECT(Authorization: Bearer …) → CONNECTED 를 받아야 onConnected
//  - send(envelope) 는 StompRouting 으로 SUBSCRIBE / UNSUBSCRIBE / SEND 프레임으로 바뀐다 (기존 Connection 클래스 무수정)
//  - MESSAGE 프레임 body 가 SocketEnvelope JSON → eventGate(eventId 중복 제거) → onEvent
//  - heart-beat: CONNECTED 의 heart-beat 헤더와 협상. 서버 비트가 3번 빠지면 실패로 간주해 재연결
class DibWebSocketClient(
    private val config: NetworkConfig,
    private val accessTokenProvider: AccessTokenProvider,
    private val guestSessionProvider: GuestSessionProvider,
    private val codec: SocketCodec = SocketCodec(),
    private val eventGate: SocketEventGate = SocketEventGate(),
    private val client: OkHttpClient = OkHttpClient()
) {
    private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "dib-stomp-heartbeat").apply { isDaemon = true }
    }
    private var socket: WebSocket? = null
    private var stompConnected = false
    private var outgoingBeat: ScheduledFuture<*>? = null
    private var incomingWatch: ScheduledFuture<*>? = null
    private val lastInboundAt = AtomicLong(0)
    private val subscriptionSeq = AtomicInteger(0)
    private val subscriptions = LinkedHashMap<String, String>()   // destination → subscription id

    @Synchronized
    fun connect(listener: DibSocketListener) {
        disconnect(1000, "reconnect")
        val webSocketUrl = try {
            config.requireWebSocketUrl()
        } catch (error: RuntimeException) {
            listener.onFailure(error)
            return
        }
        val token = accessTokenProvider.accessToken()?.takeIf(String::isNotBlank)
        val guest = guestSessionProvider.guestSessionId()?.takeIf(String::isNotBlank)
        val request = Request.Builder().url(webSocketUrl).apply {
            token?.let { header("Authorization", "Bearer $it") } ?: guest?.let { header("X-Guest-Session-Id", it) }
        }.build()
        stompConnected = false
        subscriptions.clear()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (socket !== webSocket) return
                val headers = linkedMapOf(
                    "accept-version" to "1.2",
                    "heart-beat" to "$CLIENT_BEAT_MS,$CLIENT_BEAT_MS",
                    "host" to (request.url.host)
                )
                token?.let { headers["Authorization"] = "Bearer $it" }
                guest?.let { headers["X-Guest-Session-Id"] = it }
                webSocket.send(StompFrame(StompFrame.CONNECT, headers).encode())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (socket !== webSocket) return
                lastInboundAt.set(System.currentTimeMillis())
                if (StompFrame.isHeartbeat(text)) return
                val frame = runCatching { StompFrame.decode(text) }.getOrElse {
                    listener.onMalformedMessage(text, it); return
                }
                when (frame.command) {
                    StompFrame.CONNECTED -> {
                        val accepted = synchronized(this@DibWebSocketClient) {
                            if (stompConnected || socket !== webSocket) false else { stompConnected = true; true }
                        }
                        if (accepted) {
                            scheduleHeartbeat(frame.header("heart-beat"), webSocket, listener)
                            listener.onConnected()
                        }
                    }
                    StompFrame.MESSAGE -> runCatching { codec.decode(frame.body) }
                        .onSuccess { envelope -> if (eventGate.shouldHandle(envelope)) listener.onEvent(envelope) }
                        .onFailure { listener.onMalformedMessage(frame.body, it) }
                    StompFrame.ERROR -> {
                        // 서버가 CONNECT 거절(토큰 오류 등) 또는 프로토콜 오류 → 소켓도 곧 닫힌다
                        val message = frame.header("message") ?: frame.body.ifBlank { "STOMP ERROR" }
                        synchronized(this@DibWebSocketClient) {
                            if (socket === webSocket) { socket = null; stopHeartbeat() }
                        }
                        webSocket.cancel()
                        listener.onFailure(IllegalStateException(message))
                    }
                    else -> Unit   // RECEIPT 등
                }
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

    // 기존 봉투 명령을 STOMP 프레임으로. 연결(CONNECTED) 전이면 false → 호출 측이 재시도/재연결
    @Synchronized
    fun send(envelope: SocketEnvelope): Boolean {
        val ws = socket ?: return false
        if (!stompConnected) return false
        return when (val action = StompRouting.route(envelope)) {
            is StompAction.Subscribe -> action.destinations.all { subscribe(ws, it) }
            is StompAction.Unsubscribe -> { action.destinations.forEach { unsubscribe(ws, it) }; true }
            is StompAction.Send -> ws.send(
                StompFrame(
                    StompFrame.SEND,
                    linkedMapOf("destination" to action.destination, "content-type" to "application/json"),
                    action.body.toString()
                ).encode()
            )
            StompAction.None -> true
        }
    }

    private fun subscribe(ws: WebSocket, destination: String): Boolean {
        // 스냅샷(/app/**)은 구독마다 1회 응답이라 매번 새 id 로 다시 보낸다. 나머지는 이미 구독 중이면 생략
        if (!destination.startsWith("/app/") && subscriptions.containsKey(destination)) return true
        val id = "sub-${subscriptionSeq.incrementAndGet()}"
        val ok = ws.send(StompFrame(StompFrame.SUBSCRIBE, linkedMapOf("id" to id, "destination" to destination)).encode())
        if (ok) subscriptions[destination] = id
        return ok
    }

    private fun unsubscribe(ws: WebSocket, destination: String) {
        val id = subscriptions.remove(destination) ?: return
        ws.send(StompFrame(StompFrame.UNSUBSCRIBE, linkedMapOf("id" to id)).encode())
    }

    @Synchronized
    fun disconnect(code: Int = 1000, reason: String = "client closing") {
        stopHeartbeat()
        val current = socket
        socket = null
        stompConnected = false
        subscriptions.clear()
        if (current != null) {
            runCatching { current.send(StompFrame(StompFrame.DISCONNECT).encode()) }
            current.close(code, reason)
        }
    }

    @Synchronized
    fun close() {
        disconnect()
        eventGate.clear()
        heartbeatExecutor.shutdownNow()
    }

    // heart-beat 협상: 우리가 보내는 주기 = max(우리 cx, 서버 sy), 서버에서 기대하는 주기 = max(우리 cy, 서버 sx). 한쪽이 0이면 그 방향은 없음
    @Synchronized
    private fun scheduleHeartbeat(serverHeader: String?, expectedSocket: WebSocket, listener: DibSocketListener) {
        stopHeartbeat()
        val parts = serverHeader?.split(',')?.map { it.trim().toLongOrNull() ?: 0L } ?: listOf(0L, 0L)
        val serverSends = parts.getOrElse(0) { 0L }
        val serverWants = parts.getOrElse(1) { 0L }
        lastInboundAt.set(System.currentTimeMillis())

        if (serverWants > 0) {
            val interval = maxOf(CLIENT_BEAT_MS, serverWants)
            outgoingBeat = heartbeatExecutor.scheduleWithFixedDelay(
                {
                    if (socket !== expectedSocket) return@scheduleWithFixedDelay
                    if (!expectedSocket.send(StompFrame.HEARTBEAT)) fail(expectedSocket, listener, "STOMP heart-beat could not be sent.")
                },
                interval, interval, TimeUnit.MILLISECONDS
            )
        }
        if (serverSends > 0) {
            val interval = maxOf(CLIENT_BEAT_MS, serverSends)
            incomingWatch = heartbeatExecutor.scheduleWithFixedDelay(
                {
                    if (socket !== expectedSocket) return@scheduleWithFixedDelay
                    if (System.currentTimeMillis() - lastInboundAt.get() > interval * MAX_MISSED_BEATS) {
                        fail(expectedSocket, listener, "STOMP heart-beat from server was missed $MAX_MISSED_BEATS times.")
                    }
                },
                interval, interval, TimeUnit.MILLISECONDS
            )
        }
    }

    private fun fail(expectedSocket: WebSocket, listener: DibSocketListener, message: String) {
        synchronized(this) {
            if (socket !== expectedSocket) return
            socket = null
            stompConnected = false
            subscriptions.clear()
            stopHeartbeat()
        }
        expectedSocket.cancel()
        listener.onFailure(SocketTimeoutException(message))
    }

    @Synchronized
    private fun stopHeartbeat() {
        outgoingBeat?.cancel(false)
        outgoingBeat = null
        incomingWatch?.cancel(false)
        incomingWatch = null
    }

    private companion object {
        const val CLIENT_BEAT_MS = 10_000L
        const val MAX_MISSED_BEATS = 3
    }
}
