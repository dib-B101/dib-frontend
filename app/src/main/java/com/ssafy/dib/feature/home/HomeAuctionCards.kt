package com.ssafy.dib.feature.home

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.time.remainingWholeSeconds
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * 상품 카드는 앱 어디서나 홈 카드와 같은 모양이어야 한다.
 * 홈 추천·마감 임박, 찜한 경매, 검색 결과, 카테고리 목록이 모두 이 두 카드를 쓴다.
 * onFavorite 가 null 이면 하트를 그리지 않는다(찜 상태를 모르는 화면). showStatus 는 예정·종료 배지를 붙인다.
 */
@Composable
fun HomeAuctionCard(
    auction: HomeAuction,
    favorite: Boolean,
    onFavorite: ((Boolean) -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = false,
    meta: String? = null
) {
    val remainingSeconds = rememberHomeAuctionRemaining(auction)
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Colors.Background,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Colors.Border),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.05f).background(Colors.Image, RoundedCornerShape(13.dp))) {
                ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
                if (showStatus) HomeAuctionStatusBadge(auction.status, Modifier.align(Alignment.TopStart).padding(6.dp))
                if (onFavorite != null) DibWishlistButton(
                    selected = favorite,
                    onSelectedChange = onFavorite,
                    productName = auction.name,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(auction.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                Text(auction.priceText, color = Colors.Navy, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                Text(meta ?: homeAuctionMeta(auction, remainingSeconds), maxLines = 1, color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun HomeAuctionListCard(
    auction: HomeAuction,
    favorite: Boolean,
    onFavorite: ((Boolean) -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = false,
    meta: String? = null
) {
    val remainingSeconds = rememberHomeAuctionRemaining(auction)
    Row(
        modifier.fillMaxWidth().height(116.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(92.dp)) {
            ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
            if (showStatus) HomeAuctionStatusBadge(auction.status, Modifier.align(Alignment.TopStart).padding(5.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(auction.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(auction.priceText, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(meta ?: homeAuctionMeta(auction, remainingSeconds), color = Colors.Muted, fontSize = 10.sp)
        }
        if (onFavorite != null) DibWishlistButton(favorite, onFavorite, auction.name)
    }
}

/** 예정·종료 경매만 배지를 단다. 진행 중은 카드 자체가 기본 상태라 표시하지 않는다. */
@Composable
private fun HomeAuctionStatusBadge(status: String, modifier: Modifier = Modifier) {
    val label = when (status.uppercase()) {
        "SCHEDULED" -> "예정"
        "ENDED" -> "종료"
        "CANCELED", "CANCELLED" -> "취소"
        else -> return
    }
    Surface(modifier, color = if (status.equals("SCHEDULED", true)) Colors.NavySoft else Colors.Surface, shape = RoundedCornerShape(9.dp)) {
        Text(label, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = if (status.equals("SCHEDULED", true)) Colors.Navy else Colors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** 카드 아랫줄. 진행 중이면 남은 시간, 예정·종료면 상태 글자. */
internal fun homeAuctionMeta(auction: HomeAuction, remainingSeconds: Int = auction.remainingSeconds): String = when (auction.status.uppercase()) {
    "SCHEDULED" -> "경매 예정"
    "ENDED" -> "경매 종료"
    "CANCELED", "CANCELLED" -> "경매 취소"
    else -> if (remainingSeconds <= 0) "마감" else "${remainingTimeLabel(remainingSeconds)} 남음"
}

/** 서버 종료 시각을 우선 사용하고, 없는 응답은 단조 시계로 받은 초부터 센다. */
@Composable
internal fun rememberHomeAuctionRemaining(auction: HomeAuction): Int {
    val endInstant = remember(auction.endedAt) {
        auction.endedAt?.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { Instant.parse(raw) }
                .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
                .recoverCatching { LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant() }
                .getOrNull()
        }
    }
    var remaining by remember(auction.id, auction.endedAt, auction.remainingSeconds) {
        mutableIntStateOf(endInstant?.let { remainingWholeSeconds(Instant.now(), it) } ?: auction.remainingSeconds)
    }
    LaunchedEffect(auction.id, auction.status, auction.endedAt, auction.remainingSeconds) {
        if (!auction.status.equals("ACTIVE", ignoreCase = true)) return@LaunchedEffect
        val deadlineMillis = SystemClock.elapsedRealtime() + auction.remainingSeconds.coerceAtLeast(0).toLong() * 1_000L
        while (true) {
            remaining = endInstant?.let { remainingWholeSeconds(Instant.now(), it) }
                ?: ((deadlineMillis - SystemClock.elapsedRealtime() + 999L) / 1_000L).coerceAtLeast(0L).toInt()
            if (remaining <= 0) break
            delay(1_000)
        }
    }
    return remaining
}
