package com.ssafy.dib.core.time

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
