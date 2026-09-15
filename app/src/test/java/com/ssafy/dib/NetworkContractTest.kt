package com.ssafy.dib

import com.ssafy.dib.core.network.ApiErrorParser
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import com.ssafy.dib.core.network.RetryPolicy
import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.core.network.responsePayload
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.socket.SocketCodec
import com.ssafy.dib.data.remote.socket.SocketCommands
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventGate
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import com.ssafy.dib.data.remote.socket.SocketUpdateFreshnessGate
import com.ssafy.dib.data.remote.socket.LiveSocketEventParser
import java.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkContractTest {
    @Test
    fun successfulResponseUnwrapsBackendDataEnvelope() {
        val payload = DibJson.instance.responsePayload(
            """{"message":"조회 성공","data":{"items":[{"orderId":7}]}}"""
        )

        assertEquals(7, payload.jsonObject.getValue("items").jsonArray.single().jsonObject.getValue("orderId").jsonPrimitive.content.toInt())
    }

    @Test
    fun successfulResponseKeepsLegacyDirectPayload() {
        val payload = DibJson.instance.responsePayload("""{"items":[{"orderId":8}]}""")

        assertEquals(8, payload.jsonObject.getValue("items").jsonArray.single().jsonObject.getValue("orderId").jsonPrimitive.content.toInt())
    }

    @Test
    fun socketFreshnessRejectsOlderStateAndKeepsRecoveryCursor() {
        val gate = SocketUpdateFreshnessGate()

        assertTrue(gate.shouldHandle("auction:3", "2026-09-15T05:00:00Z"))
        assertFalse(gate.shouldHandle("auction:3", "2026-09-15T04:59:59Z"))
        assertTrue(gate.shouldHandle("auction:3", "2026-09-15T05:00:01Z"))
        assertEquals("2026-09-15T05:00:01Z", gate.lastKnown("auction:3"))
    }

    @Test
    fun commonErrorParserSupportsTargetContract() {
        val error = ApiErrorParser().parse(
            httpStatus = 409,
            body = """{"timestamp":"2026-09-11T00:00:00Z","status":409,"code":"ACTIVE_ORDER_EXISTS","message":"진행 중인 주문이 있습니다.","path":"/api/v1/members/me/withdrawal","traceId":"trace-1","fieldErrors":{"reason":"invalid"}}""",
            requestPath = "/fallback"
        )

        assertEquals(409, error.status)
        assertEquals("ACTIVE_ORDER_EXISTS", error.code)
        assertEquals("trace-1", error.traceId)
        assertEquals("/api/v1/members/me/withdrawal", error.path)
        assertEquals("invalid", error.fieldErrors?.jsonObject?.get("reason")?.jsonPrimitive?.content)
    }

    @Test
    fun commonErrorParserSupportsCurrentCompactContract() {
        val error = ApiErrorParser().parse(
            httpStatus = 401,
            body = """{"code":"UNAUTHORIZED","message":"인증이 필요합니다."}""",
            requestPath = "/api/v1/orders"
        )

        assertEquals(401, error.status)
        assertEquals("UNAUTHORIZED", error.code)
        assertEquals("/api/v1/orders", error.path)
        assertNull(error.traceId)
        assertTrue(error.requiresLogin)
    }

    @Test
    fun commandRetryRequiresIdempotencyKey() {
        assertTrue(RetryPolicy.canAutomaticallyRetry("GET", hasIdempotencyKey = false))
        assertFalse(RetryPolicy.canAutomaticallyRetry("POST", hasIdempotencyKey = false))
        assertTrue(RetryPolicy.canAutomaticallyRetry("POST", hasIdempotencyKey = true))
    }

    @Test
    fun missingServerUrlReturnsConfigurationFailure() {
        val client = DibHttpClient(
            config = NetworkConfig(apiBaseUrl = "", webSocketUrl = ""),
            accessTokenProvider = AccessTokenProvider { null },
            guestSessionProvider = GuestSessionProvider { null }
        )

        val result = AuthRemoteDataSource(client).login(
            LoginRequest("user@example.com", "password", "device-1")
        )

        assertTrue(result is ApiResult.Failure)
        assertEquals(
            ApiErrorCodes.CLIENT_NOT_CONFIGURED,
            (result as ApiResult.Failure).error.code
        )
    }

    @Test
    fun bidCommandUsesDocumentedEnvelopeAndDeduplicatesEvents() {
        val codec = SocketCodec()
        val command = SocketCommands.placeBid(
            auctionId = "auction-1",
            amount = 52_000,
            commandId = "command-1"
        )
        val decoded = codec.decode(codec.encode(command))

        assertEquals(SocketEventTypes.PLACE_BID, decoded.eventType)
        assertEquals("command-1", decoded.commandId)
        assertEquals("auction-1", decoded.payload.getValue("auctionId").jsonPrimitive.content)
        assertEquals(52_000, decoded.payload.getValue("amount").jsonPrimitive.content.toInt())

        val gate = SocketEventGate()
        val event = SocketEnvelope(eventType = SocketEventTypes.HIGHEST_BID_UPDATED, eventId = "event-1")
        assertTrue(gate.shouldHandle(event))
        assertFalse(gate.shouldHandle(event))
    }

    @Test
    fun pingUsesTheSameEventIdInEnvelopeAndPayload() {
        val command = SocketCommands.ping(eventId = "heartbeat-1")

        assertEquals(SocketEventTypes.PING, command.eventType)
        assertEquals("heartbeat-1", command.eventId)
        assertEquals("heartbeat-1", command.payload.getValue("eventId").jsonPrimitive.content)
        assertEquals(command.occurredAt, command.payload.getValue("clientTime").jsonPrimitive.content)
    }

    @Test
    fun liveChatRejectsOversizedMessageInsteadOfTruncatingIt() {
        assertThrows(IllegalArgumentException::class.java) {
            SocketCommands.sendLiveChat("live-1", "가".repeat(501))
        }
    }

    @Test
    fun orderChatCommandIncludesOrderAndClientTimestamp() {
        val command = SocketCommands.sendChatMessage("order-7", "안녕하세요", "command-1")

        assertEquals(SocketEventTypes.SEND_CHAT_MESSAGE, command.eventType)
        assertEquals("command-1", command.payload.getValue("commandId").jsonPrimitive.content)
        assertEquals("order-7", command.payload.getValue("orderId").jsonPrimitive.content)
        assertEquals("안녕하세요", command.payload.getValue("content").jsonPrimitive.content)
        assertTrue(command.payload.containsKey("clientSentAt"))
    }

    @Test
    fun liveAuctionStatusUpdatesPriceCountAndRemainingTime() {
        val update = LiveSocketEventParser(now = { Instant.parse("2026-09-13T08:00:00Z") }).parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_AUCTION_STATUS_UPDATED,
                occurredAt = "2026-09-13T08:00:01Z",
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-1")
                    put("auctionId", "auction-3")
                    put("currentPrice", 57_000)
                    put("bidCount", 8)
                    put("endedAt", "2026-09-13T08:00:30Z")
                    put("status", "ACTIVE")
                }
            )
        )

        requireNotNull(update)
        assertEquals("live-1", update.liveBroadcastId)
        assertEquals("auction-3", update.auctionId)
        assertEquals(57_000, update.currentPrice)
        assertEquals(8, update.bidCount)
        assertEquals(30, update.remainingSeconds)
    }

    @Test
    fun liveViewerEventDoesNotRequireAnAuction() {
        val update = LiveSocketEventParser().parse(
            SocketEnvelope(
                eventType = SocketEventTypes.LIVE_VIEWER_COUNT_UPDATED,
                payload = buildJsonObject {
                    put("liveBroadcastId", "live-2")
                    put("viewerCount", 1_321)
                }
            )
        )

        requireNotNull(update)
        assertEquals("live-2", update.liveBroadcastId)
        assertEquals(1_321, update.viewerCount)
        assertNull(update.auctionId)
    }
}
