package com.ssafy.dib.core.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatServerTime(
    value: String?,
    pattern: String = "yyyy.MM.dd HH:mm",
    zoneId: ZoneId = ZoneId.systemDefault()
): String? {
    val serverValue = value?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        val dateTime = runCatching { Instant.parse(serverValue).atZone(zoneId) }
            .recoverCatching { OffsetDateTime.parse(serverValue).atZoneSameInstant(zoneId) }
            .recoverCatching { LocalDateTime.parse(serverValue).atZone(zoneId) }
            .getOrThrow()
        DateTimeFormatter.ofPattern(pattern).format(dateTime)
    }.getOrNull()
}
