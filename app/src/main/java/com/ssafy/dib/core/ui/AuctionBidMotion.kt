package com.ssafy.dib.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class BidMotionTone { Neutral, Success, Outbid }

fun auctionUrgencyColor(remainingSeconds: Int): Color {
    val progress = (15 - remainingSeconds).coerceIn(0, 15) / 15f
    return lerp(Color(0xFFEE9151), Color(0xFFEF3C43), progress)
}

fun auctionUrgencySurface(remainingSeconds: Int): Color {
    val progress = (15 - remainingSeconds).coerceIn(0, 15) / 15f
    return lerp(Color(0xFFFFF9F4), Color(0xFFFFE4E1), progress)
}

@Composable
fun AnimatedAuctionPrice(
    price: Int,
    identity: String,
    motionSequence: Int,
    tone: BidMotionTone,
    urgent: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedPrice = remember(identity) { Animatable(price.toFloat()) }
    val scale = remember(identity) { Animatable(1f) }
    LaunchedEffect(identity, price) {
        animatedPrice.animateTo(price.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    // 성공·최고가 상실에는 한 번, 마감 15초부터는 같은 가격 바운스를 쉬어 가며 반복한다.
    // 초가 바뀌어도 effect key가 그대로라 진행 중인 바운스가 다시 시작되지 않는다.
    LaunchedEffect(identity, motionSequence, urgent) {
        suspend fun bounceOnce() {
            scale.animateTo(1.085f, tween(280, easing = FastOutSlowInEasing))
            scale.animateTo(.995f, tween(360, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
        scale.snapTo(1f)
        if (motionSequence > 0) bounceOnce()
        if (urgent) {
            if (motionSequence > 0) delay(1_300)
            while (true) {
                bounceOnce()
                delay(1_300)
            }
        }
    }
    val targetColor = when (tone) {
        BidMotionTone.Success -> Colors.MintInk
        BidMotionTone.Outbid -> Colors.Urgent
        BidMotionTone.Neutral -> baseColor
    }
    val color by animateColorAsState(targetColor, tween(250), label = "bidPriceColor")
    Text(
        "%,d원".format(animatedPrice.value.roundToInt().coerceAtLeast(0)),
        modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }.semantics { contentDescription = "%,d원".format(price) },
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

@Composable
fun AuctionUrgencyProgress(remainingSeconds: Int, modifier: Modifier = Modifier) {
    if (remainingSeconds !in 1..15) return
    val fraction by animateFloatAsState(
        targetValue = remainingSeconds / 15f,
        animationSpec = tween(1_000, easing = LinearEasing),
        label = "auctionUrgencyProgress"
    )
    val color = auctionUrgencyColor(remainingSeconds)
    Box(modifier.height(3.dp).background(color.copy(alpha = .17f), RoundedCornerShape(3.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color, RoundedCornerShape(3.dp)))
    }
}
