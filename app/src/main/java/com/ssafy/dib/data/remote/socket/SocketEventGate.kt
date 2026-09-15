package com.ssafy.dib.data.remote.socket

import java.time.Instant

class SocketEventGate(private val capacity: Int = 512) {
    private val eventIds = LinkedHashSet<String>()

    @Synchronized
    fun shouldHandle(envelope: SocketEnvelope): Boolean {
        val eventId = envelope.eventId ?: return true
        if (!eventIds.add(eventId)) return false
        if (eventIds.size > capacity) eventIds.remove(eventIds.first())
        return true
    }

    @Synchronized
    fun clear() = eventIds.clear()
}

internal class SocketUpdateFreshnessGate {
    private val latestOccurredAt = mutableMapOf<String, String>()

    @Synchronized
    fun shouldHandle(streamKey: String, occurredAt: String?): Boolean {
        val occurredAtValue = occurredAt ?: return true
        val candidate = occurredAtValue.toInstantOrNull() ?: return true
        val previous = latestOccurredAt[streamKey].toInstantOrNull()
        if (previous != null && candidate.isBefore(previous)) return false
        latestOccurredAt[streamKey] = occurredAtValue
        return true
    }

    @Synchronized
    fun lastKnown(streamKey: String): String? = latestOccurredAt[streamKey]

    @Synchronized
    fun clear() = latestOccurredAt.clear()

    private fun String?.toInstantOrNull(): Instant? = this?.let {
        runCatching { Instant.parse(it) }.getOrNull()
    }
}
