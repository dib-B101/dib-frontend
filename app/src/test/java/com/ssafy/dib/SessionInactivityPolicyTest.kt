package com.ssafy.dib

import com.ssafy.dib.core.session.SessionInactivityPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionInactivityPolicyTest {
    @Test
    fun `timeout boundary expires the session`() {
        assertTrue(SessionInactivityPolicy.hasExpired(1_000L, 31_000L, 30_000L))
    }

    @Test
    fun `recent interaction keeps the session active`() {
        assertFalse(SessionInactivityPolicy.hasExpired(1_000L, 30_999L, 30_000L))
    }

    @Test
    fun `clock rollback does not expire the session`() {
        assertFalse(SessionInactivityPolicy.hasExpired(31_000L, 1_000L, 30_000L))
    }
}
