package com.ssafy.dib

import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auth.RefreshTokenResponse
import com.ssafy.dib.data.repository.SessionTokenRefresher
import com.ssafy.dib.domain.auth.AuthSession
import com.ssafy.dib.domain.auth.AuthSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokenRefresherTest {
    @Test
    fun `rotates tokens and preserves member identity`() {
        val store = FakeSessionStore(session())
        val refresher = SessionTokenRefresher(store, "device-1", { request ->
            assertEquals("refresh-old", request.refreshToken)
            assertEquals("device-1", request.deviceId)
            ApiResult.Success(RefreshTokenResponse("access-new", "refresh-new", 1_800L), 200)
        }, now = { 1_000L })

        assertEquals("access-new", refresher.refresh("access-old"))
        assertEquals("member-1", store.value?.memberId)
        assertEquals("refresh-new", store.value?.refreshToken)
        assertEquals(1_801_000L, store.value?.accessExpiresAtEpochMillis)
    }

    @Test
    fun `reuses token already rotated by another request`() {
        val store = FakeSessionStore(session().copy(accessToken = "access-new"))
        var calls = 0
        val refresher = SessionTokenRefresher(store, "device-1", {
            calls++
            ApiResult.Success(RefreshTokenResponse("unused", "unused", 1L), 200)
        })

        assertEquals("access-new", refresher.refresh("access-old"))
        assertEquals(0, calls)
    }

    @Test
    fun `terminal refresh failure clears the session`() {
        val store = FakeSessionStore(session())
        val refresher = SessionTokenRefresher(store, "device-1", {
            ApiResult.Failure(ApiFailure(401, "REFRESH_TOKEN_EXPIRED", "expired"))
        })

        assertNull(refresher.refresh("access-old"))
        assertNull(store.value)
    }

    private fun session() = AuthSession(
        memberId = "member-1",
        email = "member@example.com",
        nickname = "dibber",
        accessToken = "access-old",
        refreshToken = "refresh-old",
        accessExpiresAtEpochMillis = 900L
    )

    private class FakeSessionStore(var value: AuthSession?) : AuthSessionStore {
        override fun read(): AuthSession? = value
        override fun save(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
}
