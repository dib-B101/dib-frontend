package com.ssafy.dib.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

class ApiErrorParser(private val json: Json = DibJson.instance) {
    fun parse(httpStatus: Int, body: String?, requestPath: String): ApiFailure {
        val payload = body?.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        }
        return ApiFailure(
            status = (payload?.get("status") as? JsonPrimitive)?.intOrNull ?: httpStatus,
            code = payload.string("code") ?: "HTTP_$httpStatus",
            message = payload.string("message") ?: "요청을 처리하지 못했습니다.",
            path = payload.string("path") ?: requestPath,
            traceId = payload.string("traceId"),
            timestamp = payload.string("timestamp"),
            fieldErrors = payload?.get("fieldErrors")
        )
    }

    private fun JsonObject?.string(key: String): String? =
        (this?.get(key) as? JsonPrimitive)?.contentOrNull
}
