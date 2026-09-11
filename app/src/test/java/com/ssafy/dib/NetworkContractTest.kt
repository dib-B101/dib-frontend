package com.ssafy.dib

import com.ssafy.dib.core.network.ApiErrorParser
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.AccessTokenProvider
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.GuestSessionProvider
import com.ssafy.dib.core.network.NetworkConfig
import com.ssafy.dib.core.network.RetryPolicy
import com.ssafy.dib.data.remote.auth.AuthRemoteDataSource
import com.ssafy.dib.data.remote.auth.LoginRequest
import com.ssafy.dib.data.remote.socket.SocketCodec
import com.ssafy.dib.data.remote.socket.SocketCommands
import com.ssafy.dib.data.remote.socket.SocketEnvelope
import com.ssafy.dib.data.remote.socket.SocketEventGate
import com.ssafy.dib.data.remote.socket.SocketEventTypes
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkContractTest {
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
    fun liveChatRejectsOversizedMessageInsteadOfTruncatingIt() {
        assertThrows(IllegalArgumentException::class.java) {
            SocketCommands.sendLiveChat("live-1", "가".repeat(501))
        }
    }
}
