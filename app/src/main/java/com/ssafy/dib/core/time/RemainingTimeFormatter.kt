package com.ssafy.dib.core.time

import java.time.Duration
import java.time.Instant

// 종료 시각까지 남은 초. 내림(floor)으로 세면 서버가 마감을 15초로 되돌린 직후 전송 지연 때문에 14.7초 → "14" 로 보여
// 15초 리셋이 아닌 것처럼 보였다. 올림(ceil)이면 그 순간 15 가 나오고, 0 은 정말 끝났을 때만 나온다
fun remainingWholeSeconds(from: Instant, to: Instant): Int {
    val millis = Duration.between(from, to).toMillis()
    if (millis <= 0L) return 0
    return ((millis + 999L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

// 긴 경매는 일·시, 짧은 경매는 시·분으로 읽고 마지막 한 시간은 초를 보여준다.
fun formatRemainingTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val days = safe / 86_400
    val hours = safe % 86_400 / 3_600
    val minutes = safe % 3_600 / 60
    return when {
        days > 0 -> "${days}일 ${hours}시간"
        hours > 0 -> "${hours}시간 %02d분".format(minutes)
        else -> "%02d:%02d".format(minutes, safe % 60)
    }
}
