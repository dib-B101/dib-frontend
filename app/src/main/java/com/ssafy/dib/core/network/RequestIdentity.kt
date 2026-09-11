package com.ssafy.dib.core.network

import java.util.UUID

fun interface AccessTokenProvider {
    fun accessToken(): String?
}

fun interface GuestSessionProvider {
    fun guestSessionId(): String?
}

fun interface IdempotencyKeyProvider {
    fun newKey(): String
}

object UuidIdempotencyKeyProvider : IdempotencyKeyProvider {
    override fun newKey(): String = UUID.randomUUID().toString()
}

object RetryPolicy {
    private val safeMethods = setOf("GET", "HEAD", "OPTIONS")

    fun canAutomaticallyRetry(method: String, hasIdempotencyKey: Boolean): Boolean =
        method.uppercase() in safeMethods || hasIdempotencyKey
}
