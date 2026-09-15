package com.ssafy.dib.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatServerTime(
    value: String?,
    pattern: String = "yyyy.MM.dd HH:mm",
    zoneId: ZoneId = ZoneId.systemDefault()
): String? {
    val instantValue = value?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        DateTimeFormatter.ofPattern(pattern).format(Instant.parse(instantValue).atZone(zoneId))
    }.getOrNull()
}
