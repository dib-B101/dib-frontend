package com.ssafy.dib.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.domain.live.LiveStreamSession
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

/** 시청자가 한 방송에 머물면서 진행 중인 경매에 입찰하고 채팅하는 화면. */
@Composable
fun LiveWatchScreen(
    liveTitle: String,
    sellerNickname: String?,
    viewerCount: Int,
    streamTokenProvider: (suspend () -> Result<LiveStreamSession>)?,
    activeAuction: AuctionSummary?,
    chatMessages: List<LiveChatMessage>,
    bidNotices: List<LiveBidNotice>,
    connectionState: RealtimeConnectionState?,
    isLoading: Boolean,
    errorMessage: String?,
    chatError: String?,
    bidFeedback: RealtimeBidFeedback?,
    bidEnabled: Boolean,
    isAuthenticated: Boolean,
    currentMemberId: String?,
    sellerMemberId: String?,
    liveEnded: Boolean,
    lastResult: LiveAuctionResult? = null,
    onRetry: () -> Unit,
    onBid: (String, Int) -> Boolean,
    onSendChat: (String) -> Boolean,
    onLoginRequired: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBidSheet by rememberSaveable(activeAuction?.auctionId) { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bidFeedback?.eventKey) {
        val feedback = bidFeedback ?: return@LaunchedEffect
        showBidSheet = false
        feedbackMessage = feedback.message.ifBlank {
            if (feedback.accepted) "입찰이 접수됐어요." else "입찰이 반영되지 않았어요."
        }
        delay(2_500)
        feedbackMessage = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Surface
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Colors.Navy)
            }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
                TextButton(onClick = onBack) { Text("돌아가기") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                LiveWatchHeader(
                    liveTitle = liveTitle,
                    sellerNickname = sellerNickname,
                    viewerCount = viewerCount,
                    connectionState = connectionState,
                    liveEnded = liveEnded,
                    onBack = onBack
                )
                HorizontalDivider(color = Colors.Border)
                LiveWatchVideoPanel(
                    liveEnded = liveEnded,
                    streamTokenProvider = streamTokenProvider,
                    modifier = Modifier.padding(16.dp)
                )
                if (liveEnded) Text(
                    "방송이 종료됐어요",
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp),
                    color = Colors.Urgent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ) else LiveWatchAuctionCard(
                    auction = activeAuction,
                    bidEnabled = bidEnabled,
                    lastResult = lastResult,
                    currentMemberId = currentMemberId,
                    onBidRequest = { if (isAuthenticated) showBidSheet = true else onLoginRequired() }
                )
                feedbackMessage?.let { message ->
                    Text(
                        message,
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(if (bidFeedback?.accepted == true) Colors.MintSoft else Color(0xFFFFE9E9), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        color = if (bidFeedback?.accepted == true) Colors.MintInk else Colors.Urgent,
                        fontSize = 12.sp
                    )
                }
                HorizontalDivider(color = Colors.Border, modifier = Modifier.padding(top = 8.dp))
                LiveChatPanel(
                    chatMessages = chatMessages,
                    bidNotices = bidNotices,
                    currentMemberId = currentMemberId,
                    sellerMemberId = sellerMemberId,
                    chatError = chatError,
                    inputEnabled = !liveEnded && isAuthenticated,
                    onSendChat = { content -> if (isAuthenticated) onSendChat(content) else { onLoginRequired(); false } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val auction = activeAuction
    if (showBidSheet && auction != null && !liveEnded) LiveWatchBidDialog(
        auction = auction,
        onDismiss = { showBidSheet = false },
        onSubmit = { amount -> if (onBid(auction.auctionId, amount)) showBidSheet = false }
    )
}

@Composable
private fun LiveWatchHeader(
    liveTitle: String,
    sellerNickname: String?,
    viewerCount: Int,
    connectionState: RealtimeConnectionState?,
    liveEnded: Boolean,
    onBack: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "watchLivePulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = .4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "watchLiveDot"
    )
    Row(
        Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
        Column(Modifier.weight(1f)) {
            Text(liveTitle.ifBlank { "Live 방송" }, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    sellerNickname?.takeIf(String::isNotBlank),
                    "%,d명 시청 중".format(viewerCount.coerceAtLeast(0)),
                    liveConnectionLabel(connectionState)
                ).joinToString(" · "),
                color = Colors.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(color = if (liveEnded) Colors.Muted else Colors.Live, shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!liveEnded) Box(Modifier.size(6.dp).graphicsLayer(alpha = dotAlpha).background(Color.White, CircleShape))
                Text(if (liveEnded) "종료" else "LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun LiveWatchVideoPanel(
    liveEnded: Boolean,
    streamTokenProvider: (suspend () -> Result<LiveStreamSession>)?,
    modifier: Modifier = Modifier
) {
    // 시청자는 구독만 하므로 카메라·마이크 권한을 요청하지 않는다
    val session = rememberLiveVideoSession(
        role = LiveVideoRole.VIEWER,
        enabled = streamTokenProvider != null && !liveEnded,
        tokenProvider = streamTokenProvider
    )
    Box(
        modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF17212D)),
        contentAlignment = Alignment.Center
    ) {
        when {
            liveEnded -> LiveVideoNotice("방송이 종료됐어요", "다시보기는 아직 준비 중이에요")
            streamTokenProvider == null -> LiveVideoNotice("영상을 불러올 수 없어요", "채팅과 입찰로 참여할 수 있어요")
            else -> when (val state = session.state) {
                is LiveVideoState.Failed -> LiveVideoNotice(
                    "영상을 불러오지 못했어요",
                    state.message,
                    action = "다시 시도" to session::retry
                )
                LiveVideoState.Disconnected -> LiveVideoNotice(
                    "영상 연결이 끊겼어요",
                    "채팅과 입찰은 그대로 동작해요",
                    action = "다시 연결" to session::retry
                )
                LiveVideoState.Connected, LiveVideoState.Reconnecting ->
                    if (session.videoTrack != null) LiveVideoSurface(
                        room = session.room,
                        videoTrack = session.videoTrack,
                        mirror = false,
                        modifier = Modifier.fillMaxSize()
                    ) else LiveVideoNotice("방송 화면을 기다리는 중이에요", "판매자가 카메라를 켜면 바로 보여요")
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Colors.Mint, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    Text("영상을 불러오는 중이에요", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveWatchAuctionCard(
    auction: AuctionSummary?,
    bidEnabled: Boolean,
    lastResult: LiveAuctionResult?,
    currentMemberId: String?,
    onBidRequest: () -> Unit
) {
    if (auction == null || !auction.status.equals("ACTIVE", ignoreCase = true)) {
        // 낙찰자만 축하 연출을 본다 — 나머지 시청자에게 "당신이 낙찰됐다"고 착각하게 하면 안 된다
        val isWinner = lastResult?.winnerId != null && lastResult.winnerId == currentMemberId
        when {
            lastResult != null && isWinner -> AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = .85f)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .background(Colors.MintSoft, RoundedCornerShape(14.dp))
                        .border(1.dp, Colors.Mint, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text("🎉 낙찰을 축하해요!", color = Colors.MintInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "‘${lastResult.title}’ · 최종가 %,d원".format(lastResult.finalPrice ?: 0),
                        Modifier.padding(top = 4.dp),
                        color = Colors.Navy,
                        fontSize = 13.sp
                    )
                    Text(
                        "거래 상세에서 결제와 배송을 확인해주세요",
                        Modifier.padding(top = 4.dp),
                        color = Colors.Muted,
                        fontSize = 11.sp
                    )
                }
            }
            lastResult != null -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("‘${lastResult.title}’ 경매가 종료됐어요", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (lastResult.sold) "최종가 %,d원에 낙찰됐어요".format(lastResult.finalPrice ?: 0) else "입찰이 없어 유찰됐어요",
                    Modifier.padding(top = 4.dp),
                    color = Colors.Muted,
                    fontSize = 11.sp
                )
                Text(
                    "다음 상품은 판매자가 시작하면 바로 보여요",
                    Modifier.padding(top = 4.dp),
                    color = Colors.Muted,
                    fontSize = 11.sp
                )
            }
            else -> Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("다음 경매를 준비하고 있어요", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("판매자가 경매를 시작하면 바로 입찰할 수 있어요", Modifier.padding(top = 4.dp), color = Colors.Muted, fontSize = 11.sp)
            }
        }
        return
    }
    val remaining = rememberLiveCountdown(auction.auctionId, auction.remainingSeconds, auction.endedAt, true)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Colors.Live, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(auction.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("현재가 %,d원".format(auction.currentPrice), color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("입찰 ${auction.bidCount}회", color = Colors.Muted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(painterResource(R.drawable.timer_outline), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Colors.Live))
                Text(formatClock(remaining), color = Colors.Live, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (auction.isHighestBidder == true) Text(
            "내가 최고 입찰자예요",
            Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            color = Colors.MintInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Button(
            onClick = onBidRequest,
            modifier = Modifier.fillMaxWidth(),
            enabled = bidEnabled && remaining > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
        ) { Text("입찰하기", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun LiveWatchBidDialog(
    auction: AuctionSummary,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    val minimum = remember(auction.auctionId, auction.currentPrice) {
        (auction.currentPrice.takeIf { it > 0 } ?: auction.startPrice) + 1_000
    }
    var amountText by rememberSaveable(auction.auctionId, minimum) { mutableStateOf(minimum.toString()) }
    val amount = amountText.toIntOrNull()
    val valid = amount != null && amount >= minimum
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("입찰하기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("현재가 %,d원 · 최소 %,d원부터 입찰할 수 있어요".format(auction.currentPrice, minimum), color = Colors.Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("입찰 금액") },
                    suffix = { Text("원") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (!valid) Text("최소 %,d원 이상 입력해주세요.".format(minimum), color = Colors.Urgent, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton({ amount?.let(onSubmit) }, enabled = valid) { Text("입찰", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } }
    )
}
