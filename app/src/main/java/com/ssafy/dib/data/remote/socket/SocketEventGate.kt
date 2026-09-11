package com.ssafy.dib.data.remote.socket

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
