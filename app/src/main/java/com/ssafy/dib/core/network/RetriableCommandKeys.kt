package com.ssafy.dib.core.network

import java.util.UUID

/** Keeps one idempotency key while the same command is retried. */
class RetriableCommandKeys(
    private val createKey: () -> String = { UUID.randomUUID().toString() }
) {
    private val keys = mutableMapOf<String, String>()

    fun keyFor(command: String): String = keys.getOrPut(command, createKey)

    fun complete(command: String) {
        keys.remove(command)
    }
}
