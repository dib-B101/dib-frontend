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
    val background = when {
        critical -> Color(0xFFF04452)
        urgent -> Color(0xFFFFE7E9)
        else -> Color(0xFFFFF0EA)
    }
    val foreground = if (critical) Color.White else Color(0xFFD74233)
    val label = when {
        safeSeconds == 0 -> "종료"
        critical -> "지금 입찰"
        urgent -> "곧 종료"
        else -> "마감 임박"
    }
    val transition = rememberInfiniteTransition(label = "auctionUrgency")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (critical) 1.045f else 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "auctionUrgencyPulse"
    )

    Row(
        modifier = modifier
            .graphicsLayer(scaleX = pulse, scaleY = pulse)
            .background(background, RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 4.dp else 7.dp)
            .semantics { contentDescription = if (safeSeconds == 0) "경매 종료" else "$label, ${formatRemainingTime(safeSeconds)} 남음" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.timer_outline),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 14.dp else 17.dp),
            colorFilter = ColorFilter.tint(foreground)
        )
        Text(
            text = "$label  ${formatRemainingTime(safeSeconds)}",
            color = foreground,
            fontSize = if (compact) 11.sp else 13.sp,
            lineHeight = if (compact) 15.sp else 18.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
