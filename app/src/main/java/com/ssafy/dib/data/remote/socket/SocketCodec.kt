package com.ssafy.dib.data.remote.socket

import com.ssafy.dib.core.network.DibJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

class SocketCodec(private val json: Json = DibJson.instance) {
    fun encode(envelope: SocketEnvelope): String =
        json.encodeToString(SocketEnvelope.serializer(), envelope)

    fun decode(message: String): SocketEnvelope =
        json.decodeFromString(SocketEnvelope.serializer(), message)

    fun <T> decodePayload(
        envelope: SocketEnvelope,
        serializer: DeserializationStrategy<T>
    ): T = json.decodeFromJsonElement(serializer, envelope.payload)
}
