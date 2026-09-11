package com.ssafy.dib

import com.ssafy.dib.domain.auth.AuthSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {
    private val session = AuthSession(
        memberId = "member-1",
        email = "user@example.com",
        nickname = "dibber",
        accessToken = "access",
        refreshToken = "refresh",
        accessExpiresAtEpochMillis = 1_000_000L
    )

    @Test
    fun refreshesBeforeAccessTokenActuallyExpires() {
        assertTrue(session.needsRefresh(nowEpochMillis = 950_000L, bufferMillis = 60_000L))
    }

    @Test
    fun keepsSessionWhenEnoughAccessTimeRemains() {
        assertFalse(session.needsRefresh(nowEpochMillis = 900_000L, bufferMillis = 60_000L))
    }
}
