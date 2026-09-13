package com.ssafy.dib.feature.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.feature.auction.BidParticipationFields
import com.ssafy.dib.feature.auction.BidSubmission
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.auction.sampleBidAddresses
import com.ssafy.dib.feature.auction.samplePaymentMethods
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFeedScreen(
    remoteItems: List<LiveFeedItem>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    activeLiveBroadcastId: String?,
    liveComments: List<LiveChatMessage>,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onLiveVisible: (String) -> Unit,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    paidBidAmount: Int,
    depositPaidAuctionIds: Set<String>,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onPaymentConsumed: () -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    onDepositPayment: (String, BidSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> Box(modifier.fillMaxSize().background(Color(0xFF17212D)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Mint) }
        errorMessage != null -> Column(modifier.fillMaxSize().background(Color(0xFF17212D)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(errorMessage, color = Color.White)
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기", color = Color.White) }
            TextButton(onClick = onClose) { Text("닫기", color = Color.White) }
        }
        remoteItems?.isEmpty() == true -> Column(modifier.fillMaxSize().background(Color(0xFF17212D)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("현재 방송 중인 Live가 없어요.", color = Color.White)
            TextButton(onClick = onClose) { Text("홈으로", color = Colors.Mint) }
        }
        else -> {
            val items = remoteItems ?: listOf(null)
            val pagerState = rememberPagerState(pageCount = items::size)
            LaunchedEffect(pagerState.currentPage, remoteItems) {
                items[pagerState.currentPage]?.liveBroadcastId?.let(onLiveVisible)
            }
            VerticalPager(state = pagerState, modifier = modifier.fillMaxSize(), key = { page -> items[page]?.liveBroadcastId ?: "sample" }) { page ->
                LiveFeedPage(
                    liveItem = items[page],
                    liveComments = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) liveComments else emptyList(),
                    chatError = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatError else null,
                    chatConnectionState = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatConnectionState else null,
                    onSendComment = onSendComment,
                    isAuthenticated = isAuthenticated,
                    paidBidAmount = if (page == pagerState.currentPage) paidBidAmount else 0,
                    depositPaidAuctionIds = depositPaidAuctionIds,
                    realtimeBidFeedback = if (page == pagerState.currentPage) realtimeBidFeedback else null,
                    onRealtimeBid = onRealtimeBid,
                    onPaymentConsumed = onPaymentConsumed,
                    onClose = onClose,
                    onProductClick = onProductClick,
                    onLoginRequired = onLoginRequired,
                    onDepositPayment = onDepositPayment
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedPage(
    liveItem: LiveFeedItem?,
    liveComments: List<LiveChatMessage>,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    paidBidAmount: Int,
    depositPaidAuctionIds: Set<String>,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onPaymentConsumed: () -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    onDepositPayment: (String, BidSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAuction = liveItem?.currentAuction
    val auctionKey = activeAuction?.auctionId ?: if (liveItem == null) "camera" else null
    val depositPaid = auctionKey in depositPaidAuctionIds
    var following by rememberSaveable { mutableStateOf(true) }
    var favorite by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(activeAuction?.bookmarked == true) }
    var showProducts by rememberSaveable { mutableStateOf(false) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var currentPrice by rememberSaveable(liveItem?.liveBroadcastId) { mutableIntStateOf(activeAuction?.currentPrice?.takeIf { it > 0 } ?: activeAuction?.startPrice ?: 34_500) }
    var remaining by rememberSaveable(liveItem?.liveBroadcastId) { mutableIntStateOf(activeAuction?.remainingSeconds ?: 42) }
    var comment by rememberSaveable { mutableStateOf("") }
    var showBidFeedback by remember { mutableStateOf(false) }
    var bidFeedbackAccepted by remember { mutableStateOf(true) }
    var bidFeedbackMessage by remember { mutableStateOf("") }
    val livePulse = rememberInfiniteTransition(label = "livePulse")
    val liveDotAlpha by livePulse.animateFloat(
        initialValue = .4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(Unit) { while (remaining > 0) { delay(1_000); remaining-- } }
    LaunchedEffect(paidBidAmount) {
        if (paidBidAmount > 0) {
            if (liveItem == null) {
                currentPrice = paidBidAmount
                if (remaining in 1..15) remaining = 15
                bidFeedbackAccepted = true
                bidFeedbackMessage = "입찰이 접수됐어요."
                showBidFeedback = true
            } else if (auctionKey == null || !onRealtimeBid(auctionKey, paidBidAmount)) {
                bidFeedbackAccepted = false
                bidFeedbackMessage = "실시간 연결을 확인한 뒤 다시 입찰해주세요."
                showBidFeedback = true
            }
            onPaymentConsumed()
        }
    }
    LaunchedEffect(realtimeBidFeedback?.eventKey) {
        realtimeBidFeedback ?: return@LaunchedEffect
        bidFeedbackAccepted = realtimeBidFeedback.accepted
        bidFeedbackMessage = realtimeBidFeedback.message.ifBlank {
            if (realtimeBidFeedback.accepted) "입찰이 접수됐어요." else "입찰이 반영되지 않았어요."
        }
        if (realtimeBidFeedback.accepted) {
            realtimeBidFeedback.currentPrice?.let { currentPrice = it }
        }
        showBidFeedback = true
    }
    LaunchedEffect(activeAuction?.currentPrice, activeAuction?.remainingSeconds) {
        activeAuction?.currentPrice?.let { currentPrice = it }
        activeAuction?.remainingSeconds?.let { remaining = it }
    }
    LaunchedEffect(showBidFeedback) {
        if (showBidFeedback) {
            delay(1_500)
            showBidFeedback = false
        }
    }

    Box(modifier.fillMaxSize().safeDrawingPadding().background(Color(0xFF17212D))) {
        Image(painterResource(R.drawable.live_video), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.12f), Color.Transparent, Color(0xFF07101D).copy(.72f)))))
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Colors.Live, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("●", Modifier.graphicsLayer(alpha = liveDotAlpha), color = Color.White, fontSize = 9.sp)
                        Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("시청 ${"%,d".format(liveItem?.viewCount ?: 1_248)}", Modifier.padding(start = 10.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("×", Modifier.size(44.dp).clickable(onClick = onClose).wrapContentSize(), color = Color.White, fontSize = 27.sp)
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Color(0xFFBDEEDF), CircleShape), contentAlignment = Alignment.Center) { Text("d", color = Color(0xFF13284B), fontWeight = FontWeight.Bold) }
                Text(liveItem?.title ?: "하루공방", Modifier.padding(horizontal = 8.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(onClick = { if (isAuthenticated) following = !following else onLoginRequired() }, color = if (following) Color.White else Color(0xFF102342), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) {
                    Text(if (following) "팔로잉" else "팔로우", Modifier.padding(horizontal = 17.dp, vertical = 8.dp), color = if (following) Color(0xFF102342) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedVisibility(!imeVisible, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 76.dp, bottom = 230.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val displayedComments = if (liveItem == null) listOf("도윤  포장 상태 궁금해요", "nana***  다음 상품도 기대돼요", "haeun9***  가격 실화인가요?") else liveComments.takeLast(3).map { message -> "${message.nickname ?: message.memberId}  ${message.content}" }
                displayedComments.forEach { message ->
                    Surface(color = Color.Black.copy(alpha = .18f), shape = RoundedCornerShape(9.dp)) {
                        Text(message, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
        AnimatedVisibility(!imeVisible, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 218.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LiveFavoriteAction(favorite) { if (isAuthenticated) favorite = !favorite else onLoginRequired() }
                LiveAction("···", Color.White) { showProducts = true }
            }
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).imePadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().height(116.dp).animateContentSize()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                    Text(if (activeAuction != null) "현재 경매 상품 · 상세  ↑" else "상품 2 / 5 · 전체 목록  ↑", Modifier.clickable { showProducts = true }, color = Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(activeAuction?.title ?: "달빛 유약 머그컵", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("현재가 ${"%,d".format(currentPrice)}원", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("⚡ ${formatClock(remaining)} 남음", color = if (remaining <= 15) Colors.Live else Colors.Urgent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { if (isAuthenticated) showBidSheet = true else onLoginRequired() }, enabled = remaining > 0, modifier = Modifier.size(68.dp, 58.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (remaining <= 15) Colors.Live else Colors.Navy), contentPadding = PaddingValues(0.dp)) { Text("입찰", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(44.dp).background(Color.Black.copy(.42f), RoundedCornerShape(22.dp)).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = comment, onValueChange = { comment = it.take(500) }, Modifier.weight(1f), enabled = isAuthenticated, singleLine = true, textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp), decorationBox = { inner -> if (comment.isBlank()) Text(if (isAuthenticated) "댓글을 입력하세요" else "로그인 후 댓글을 작성할 수 있어요", color = Color.White.copy(.75f), fontSize = 12.sp); inner() })
                Text("↑", Modifier.size(32.dp).background(Color.White, CircleShape).clickable { if (!isAuthenticated) onLoginRequired() else if (comment.isNotBlank() && onSendComment(comment)) comment = "" }.wrapContentSize(), color = Colors.Navy, fontWeight = FontWeight.Bold)
            }
            if (isAuthenticated && chatConnectionState != RealtimeConnectionState.Connected) Text(chatError ?: "Live 채팅 연결 중", color = Color.White.copy(.75f), fontSize = 9.sp)
        }
        AnimatedVisibility(showBidFeedback, Modifier.align(Alignment.Center), enter = fadeIn() + scaleIn(initialScale = .7f), exit = fadeOut() + scaleOut(targetScale = .82f)) {
            Surface(color = Colors.Mint, shape = RoundedCornerShape(20.dp), shadowElevation = 8.dp) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (bidFeedbackAccepted) "⚡" else "!", fontSize = 30.sp)
                    Text(bidFeedbackMessage, color = Colors.MintInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (bidFeedbackAccepted) Text("현재 최고가 ${"%,d".format(currentPrice)}원", color = Colors.MintInk, fontSize = 11.sp)
                }
            }
        }
    }

    if (showProducts) ModalBottomSheet(onDismissRequest = { showProducts = false }, containerColor = Color.White) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).navigationBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val count = if (activeAuction == null) 5 else 1
            item { Text("라이브 상품 ${count}개", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(count) { index ->
                Row(Modifier.fillMaxWidth().height(72.dp).clickable {
                    showProducts = false
                    if (isAuthenticated) onProductClick(activeAuction?.auctionId ?: if (index == 1) "camera" else "headphones") else onLoginRequired()
                }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).background(Color(0xFFECECEC), RoundedCornerShape(10.dp)))
                    Column(Modifier.padding(start = 12.dp)) { Text(activeAuction?.title ?: listOf("푸른 유약 접시", "달빛 유약 머그컵", "수제 화병", "도자기 찻잔", "우드 트레이")[index], fontWeight = FontWeight.Bold); Text(if (activeAuction != null || index == 1) "● 현재 경매 중" else "대기", color = if (activeAuction != null || index == 1) Colors.Live else Colors.Muted, fontSize = 11.sp) }
                }
            }
        }
    }
    if (showBidSheet) LiveBidSheet(currentPrice, depositPaid, { showBidSheet = false }) { submission ->
        showBidSheet = false
        onDepositPayment(auctionKey ?: "camera", submission)
    }
}

@Composable private fun LiveAction(text: String, color: Color, onClick: () -> Unit) { Box(Modifier.size(44.dp).background(Color.Black.copy(.42f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(text, color = color, fontSize = 25.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun LiveFavoriteAction(selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.size(48.dp, 58.dp).background(Color.Black.copy(.28f), RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(if (selected) R.drawable.favorite_selected else R.drawable.favorite_outline),
            contentDescription = if (selected) "찜 해제" else "찜하기",
            Modifier.size(25.dp),
            colorFilter = ColorFilter.tint(if (selected) Colors.Favorite else Color.White)
        )
        Text("찜", color = Color.White, fontSize = 9.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LiveBidSheet(currentPrice: Int, depositPaid: Boolean, onDismiss: () -> Unit, onConfirm: (BidSubmission) -> Unit) {
    val minimum = currentPrice + 1
    var amount by rememberSaveable(currentPrice) { mutableStateOf(minimum.toString()) }
    var paymentMethodId by rememberSaveable { mutableStateOf(samplePaymentMethods.first().id) }
    var addressId by rememberSaveable { mutableStateOf(sampleBidAddresses.first().id) }
    val parsed = amount.toIntOrNull() ?: 0
    val depositAmount = maxOf(1_000, parsed / 10)
    val valid = parsed >= minimum && paymentMethodId.isNotBlank() && addressId.isNotBlank()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("라이브 입찰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("현재가 ${"%,d".format(currentPrice)}원 · ${"%,d".format(minimum)}원 이상", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth(), suffix = { Text("원") }, isError = amount.isNotBlank() && !valid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            BidParticipationFields(
                selectedPaymentMethodId = paymentMethodId,
                onPaymentMethodSelected = { paymentMethodId = it },
                selectedAddressId = addressId,
                onAddressSelected = { addressId = it }
            )
            Text(
                if (depositPaid) "보증금 결제 완료 · 추가 결제 없이 재입찰할 수 있어요." else "첫 입찰 보증금은 ${"%,d".format(depositAmount)}원이에요(입찰가의 10%).",
                color = Colors.MintInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text("종료 15초 이내 입찰 시 남은 시간이 15초로 갱신돼요.", color = Colors.Muted, fontSize = 11.sp)
            Button({ onConfirm(BidSubmission(parsed, paymentMethodId, addressId)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if (depositPaid) "${"%,d".format(parsed)}원 입찰하기" else "보증금 결제로 계속", fontWeight = FontWeight.Bold) }
        }
    }
}
