package com.ssafy.dib.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.ssafy.dib.R
import com.ssafy.dib.core.time.formatRemainingTime

@Composable
fun AuctionUrgencyBadge(
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val safeSeconds = remainingSeconds.coerceAtLeast(0)
    val critical = safeSeconds in 1..15
    val urgent = safeSeconds in 1..60
    val endingSoon = safeSeconds in 1..300
    val background = when {
        critical -> Color(0xFFF04452)
        urgent -> Color(0xFFFFE7E9)
        endingSoon -> Color(0xFFFFF0EA)
        else -> Color(0xFFEFF3F8)
    }
    val foreground = when {
        critical -> Color.White
        endingSoon -> Color(0xFFD74233)
        else -> Color(0xFF13284B)
    }
    // 5분보다 많이 남았는데 "마감 임박"이라고 쓰면 시간 숫자보다 문구가 먼저 읽혀 헷갈렸다
    val label = when {
        safeSeconds == 0 -> "종료"
        critical -> "지금 입찰"
        urgent -> "곧 종료"
        endingSoon -> "마감 임박"
        else -> "남은 시간"
    }
    Row(
        modifier = modifier
            .auctionUrgencyPulse(safeSeconds)
            .background(background, RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 4.dp else 7.dp)
            .semantics { contentDescription = if (safeSeconds == 0) "경매 종료" else "$label, ${formatRemainingTime(safeSeconds)} 남음" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.timer_outline),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 15.dp else 18.dp),
            colorFilter = ColorFilter.tint(foreground)
        )
        Text(
            text = "$label  ${formatRemainingTime(safeSeconds)}",
            color = foreground,
            fontSize = if (compact) 13.sp else 15.sp,
            lineHeight = if (compact) 17.sp else 20.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * 마감이 가까울수록 빠르고 크게 뛰는 강조 애니메이션. 라이브 배지·홈 마감 임박 카드·경매 상세 남은 시간이 함께 쓴다.
 *
 * 예전에는 15초 이하에서만 4.5% 커졌다 작아져 눈에 거의 띄지 않았다.
 * 5분 이하부터 천천히 뛰기 시작해 1분·15초 이하에서 점점 빨라지고, 마지막 15초는 깜빡임도 더한다.
 * 5분보다 많이 남았으면 애니메이션을 만들지 않는다 — 목록 카드마다 무한 애니메이션이 돌면 프레임을 낭비한다.
 */
fun Modifier.auctionUrgencyPulse(remainingSeconds: Int): Modifier = composed {
    val (periodMillis, maxScale, minAlpha) = when (remainingSeconds) {
        in 1..15 -> Triple(320, 1.14f, .55f)
        in 16..60 -> Triple(520, 1.08f, 1f)
        in 61..300 -> Triple(900, 1.04f, 1f)
        else -> return@composed Modifier
    }
    val transition = rememberInfiniteTransition(label = "auctionUrgency")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(tween(periodMillis), RepeatMode.Reverse),
        label = "auctionUrgencyScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = minAlpha,
        animationSpec = infiniteRepeatable(tween(periodMillis), RepeatMode.Reverse),
        label = "auctionUrgencyAlpha"
    )
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
