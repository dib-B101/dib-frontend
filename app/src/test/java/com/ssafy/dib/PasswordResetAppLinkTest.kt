package com.ssafy.dib

import com.ssafy.dib.core.navigation.passwordResetTokenFromAppLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordResetAppLinkTest {
    private val redirect = "https://app.example.com/oauth/kakao/callback"
    private val token = "a".repeat(43)

    @Test
    fun acceptsResetLinkOnConfiguredAppLinkHost() {
        assertEquals(token, passwordResetTokenFromAppLink("https://app.example.com/password/reset?token=$token", redirect))
    }

    @Test
    fun rejectsUntrustedOrMalformedLinks() {
        assertNull(passwordResetTokenFromAppLink("https://other.example.com/password/reset?token=$token", redirect))
        assertNull(passwordResetTokenFromAppLink("http://app.example.com/password/reset?token=$token", redirect))
        assertNull(passwordResetTokenFromAppLink("https://app.example.com/password/reset?token=invalid", redirect))
    }
}
