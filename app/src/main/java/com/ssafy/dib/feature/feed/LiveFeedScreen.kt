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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.feature.auction.BidDepositStatusNotice
import com.ssafy.dib.feature.auction.BidSubmission
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFeedScreen(
    remoteItems: List<LiveFeedItem>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    hasNextPage: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    activeLiveBroadcastId: String?,
    liveComments: List<LiveChatMessage>,
    chatHasMore: Boolean,
    chatLoadingEarlier: Boolean,
    chatLoadEarlierError: String?,
    liveAuctionsByBroadcast: Map<String, List<AuctionSummary>>,
    productListLoading: Boolean,
    productListError: String?,
    reportSubmitting: Boolean,
    reportError: String?,
    reportCompleted: Boolean,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onLiveVisible: (String) -> Unit,
    onLoadEarlierComments: () -> Unit,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    currentMemberId: String?,
    paidBidAmount: Int,
    depositPaidAuctionIds: Set<String>,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onPaymentConsumed: () -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
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
            LaunchedEffect(pagerState.currentPage, remoteItems, hasNextPage, isLoadingMore, loadMoreError) {
                items[pagerState.currentPage]?.liveBroadcastId?.let(onLiveVisible)
                if (remoteItems != null && hasNextPage && !isLoadingMore && loadMoreError == null && pagerState.currentPage >= items.lastIndex - 1) {
                    onLoadMore()
                }
            }
            Box(modifier.fillMaxSize()) {
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { page -> items[page]?.liveBroadcastId ?: "sample" }) { page ->
                    LiveFeedPage(
                    liveItem = items[page],
                    isActivePage = page == pagerState.currentPage,
                    liveComments = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) liveComments else emptyList(),
                    chatHasMore = items[page]?.liveBroadcastId == activeLiveBroadcastId && chatHasMore,
                    chatLoadingEarlier = items[page]?.liveBroadcastId == activeLiveBroadcastId && chatLoadingEarlier,
                    chatLoadEarlierError = chatLoadEarlierError.takeIf { items[page]?.liveBroadcastId == activeLiveBroadcastId },
                    liveAuctions = items[page]?.liveBroadcastId?.let(liveAuctionsByBroadcast::get),
                    productListLoading = productListLoading && items[page]?.liveBroadcastId == activeLiveBroadcastId,
                    productListError = productListError.takeIf { items[page]?.liveBroadcastId == activeLiveBroadcastId },
                    reportSubmitting = reportSubmitting,
                    reportError = reportError,
                    reportCompleted = reportCompleted,
                    chatError = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatError else null,
                    chatConnectionState = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatConnectionState else null,
                    onLoadEarlierComments = onLoadEarlierComments,
                    onSendComment = onSendComment,
                    isAuthenticated = isAuthenticated,
                    currentMemberId = currentMemberId,
                    paidBidAmount = if (page == pagerState.currentPage) paidBidAmount else 0,
                    depositPaidAuctionIds = depositPaidAuctionIds,
                    realtimeBidFeedback = if (page == pagerState.currentPage) realtimeBidFeedback else null,
                    onRealtimeBid = onRealtimeBid,
                    onPaymentConsumed = onPaymentConsumed,
                    onClose = onClose,
                    onProductClick = onProductClick,
                    onLoginRequired = onLoginRequired,
                    onReportParticipant = onReportParticipant,
                    onDismissReport = onDismissReport,
                        onDepositPayment = onDepositPayment
                    )
                }
                if (isLoadingMore) {
                    CircularProgressIndicator(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp).size(24.dp), color = Colors.Mint, strokeWidth = 2.dp)
                } else if (loadMoreError != null) {
                    Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp).clickable(onClick = onLoadMore), color = Color.Black.copy(alpha = .65f), shape = RoundedCornerShape(16.dp)) {
                        Text("다음 Live를 불러오지 못했어요 · 다시 시도", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedPage(
    liveItem: LiveFeedItem?,
    isActivePage: Boolean,
    liveComments: List<LiveChatMessage>,
    chatHasMore: Boolean,
    chatLoadingEarlier: Boolean,
    chatLoadEarlierError: String?,
    liveAuctions: List<AuctionSummary>?,
    productListLoading: Boolean,
    productListError: String?,
    reportSubmitting: Boolean,
    reportError: String?,
    reportCompleted: Boolean,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onLoadEarlierComments: () -> Unit,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    currentMemberId: String?,
    paidBidAmount: Int,
    depositPaidAuctionIds: Set<String>,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onPaymentConsumed: () -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
    onDepositPayment: (String, BidSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAuction = liveItem?.currentAuction
    val productAuctions = liveAuctions ?: listOfNotNull(activeAuction)
    val auctionKey = activeAuction?.auctionId ?: if (liveItem == null) "camera" else null
    val hasActiveAuction = auctionKey != null
    val depositPaid = auctionKey in depositPaidAuctionIds
    val isOwnAuction = currentMemberId != null && (
        activeAuction?.sellerMemberId == currentMemberId || liveItem?.memberId == currentMemberId
    )
    val isHighestBidder = activeAuction?.isHighestBidder == true
    var following by rememberSaveable { mutableStateOf(true) }
    var favorite by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(activeAuction?.bookmarked == true) }
    var showProducts by rememberSaveable { mutableStateOf(false) }
    var showComments by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var currentPrice by rememberSaveable(liveItem?.liveBroadcastId) { mutableIntStateOf(activeAuction?.currentPrice?.takeIf { it > 0 } ?: activeAuction?.startPrice ?: 34_500) }
    var remaining by rememberSaveable(liveItem?.liveBroadcastId) { mutableIntStateOf(activeAuction?.remainingSeconds ?: 42) }
    var comment by rememberSaveable { mutableStateOf("") }
    var showBidFeedback by remember { mutableStateOf(false) }
    var bidFeedbackAccepted by remember { mutableStateOf(true) }
    var bidFeedbackMessage by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<LiveChatMessage?>(null) }
    var reportContent by rememberSaveable { mutableStateOf("") }
    val livePulse = rememberInfiniteTransition(label = "livePulse")
    val liveDotAlpha by livePulse.animateFloat(
        initialValue = .4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(Unit) { while (remaining > 0) { delay(1_000); remaining-- } }
    LaunchedEffect(paidBidAmount, chatConnectionState, auctionKey) {
        if (paidBidAmount > 0) {
            if (liveItem == null) {
                currentPrice = paidBidAmount
                if (remaining in 1..15) remaining = 15
                bidFeedbackAccepted = true
                bidFeedbackMessage = "입찰이 접수됐어요."
                showBidFeedback = true
                onPaymentConsumed()
            } else if (auctionKey == null || chatConnectionState != RealtimeConnectionState.Connected) {
                bidFeedbackAccepted = false
                bidFeedbackMessage = "실시간 연결 후 입찰을 자동으로 요청할게요."
                showBidFeedback = true
            } else {
                while (true) {
                    if (onRealtimeBid(auctionKey, paidBidAmount)) {
                        onPaymentConsumed()
                        break
                    }
                    delay(1_000L)
                }
            }
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
        LiveVideoBackground(liveItem?.streamUrl, isActivePage)
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
                if (liveItem == null) {
                    listOf("도윤  포장 상태 궁금해요", "nana***  다음 상품도 기대돼요", "haeun9***  가격 실화인가요?").forEach { message ->
                        Surface(color = Color.Black.copy(alpha = .18f), shape = RoundedCornerShape(9.dp)) {
                            Text(message, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
                        }
                    }
                } else {
                    if (liveComments.isNotEmpty()) {
                        Text(
                            "댓글 ${liveComments.size} · 전체보기",
                            Modifier.clickable { showComments = true }.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = Color.White.copy(alpha = .82f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    liveComments.takeLast(3).forEach { message ->
                        Surface(
                            color = Color.Black.copy(alpha = .18f),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.clickable(enabled = message.memberId != currentMemberId) {
                                if (isAuthenticated) {
                                    reportTarget = message
                                    reportContent = ""
                                    onDismissReport()
                                } else onLoginRequired()
                            }
                        ) {
                            Text("${message.nickname ?: message.memberId}  ${message.content}", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
                        }
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
                    Text(
                        if (activeAuction != null) "현재 경매 상품 · 전체 ${productAuctions.size.coerceAtLeast(1)}개  ↑" else "전체 상품 ${productAuctions.size}개  ↑",
                        Modifier.clickable { showProducts = true },
                        color = Colors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(activeAuction?.title ?: "달빛 유약 머그컵", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("현재가 ${"%,d".format(currentPrice)}원", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("⚡ ${formatClock(remaining)} 남음", color = if (remaining <= 15) Colors.Live else Colors.Urgent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { if (isAuthenticated) showBidSheet = true else onLoginRequired() },
                            enabled = hasActiveAuction && remaining > 0 && !isOwnAuction && !isHighestBidder && paidBidAmount <= 0,
                            modifier = Modifier.size(68.dp, 58.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (remaining <= 15) Colors.Live else Colors.Navy),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                when {
                                    isOwnAuction -> "내 경매"
                                    isHighestBidder -> "최고가"
                                    !hasActiveAuction -> "대기 중"
                                    paidBidAmount > 0 -> "접속 중"
                                    else -> "입찰"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            val displayedAuctions = if (liveItem == null && productAuctions.isEmpty()) emptyList() else productAuctions
            item { Text("라이브 상품 ${if (liveItem == null) 5 else displayedAuctions.size}개", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            if (productListLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Mint) }
            productListError?.let { message -> item { Text(message, color = Colors.Live, fontSize = 12.sp) } }
            if (liveItem != null && !productListLoading && displayedAuctions.isEmpty()) item { Text("편성된 상품이 없어요.", color = Colors.Muted, fontSize = 13.sp) }
            items(if (liveItem == null) 5 else displayedAuctions.size) { index ->
                val auction = displayedAuctions.getOrNull(index)
                Row(Modifier.fillMaxWidth().height(72.dp).clickable {
                    showProducts = false
                    val targetAuctionId = auction?.auctionId ?: if (liveItem == null) {
                        if (index == 1) "camera" else "headphones"
                    } else null
                    if (!isAuthenticated) onLoginRequired()
                    else targetAuctionId?.let(onProductClick)
                }, verticalAlignment = Alignment.CenterVertically) {
                    if (auction != null) {
                        DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)))
                    } else {
                        Box(Modifier.size(64.dp).background(Color(0xFFECECEC), RoundedCornerShape(10.dp)))
                    }
                    val isCurrent = auction?.auctionId == activeAuction?.auctionId || (auction == null && index == 1)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(auction?.title ?: listOf("푸른 유약 접시", "달빛 유약 머그컵", "수제 화병", "도자기 찻잔", "우드 트레이")[index], fontWeight = FontWeight.Bold)
                        Text(if (isCurrent) "● 현재 경매 중" else if (auction?.status == "ENDED") "종료" else "대기", color = if (isCurrent) Colors.Live else Colors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
    if (showComments) ModalBottomSheet(onDismissRequest = { showComments = false }, containerColor = Color.White) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 520.dp).navigationBarsPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Live 댓글", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            if (chatHasMore || chatLoadingEarlier || chatLoadEarlierError != null) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            chatLoadingEarlier -> CircularProgressIndicator(Modifier.size(22.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            chatLoadEarlierError != null -> {
                                Text(chatLoadEarlierError, color = Colors.Live, fontSize = 11.sp)
                                TextButton(onClick = onLoadEarlierComments) { Text("이전 댓글 다시 불러오기") }
                            }
                            chatHasMore -> TextButton(onClick = onLoadEarlierComments) { Text("이전 댓글 불러오기") }
                        }
                    }
                }
            }
            if (liveComments.isEmpty()) item { Text("아직 작성된 댓글이 없어요.", color = Colors.Muted, fontSize = 13.sp) }
            items(liveComments.distinctBy(LiveChatMessage::liveChattingId).sortedBy(LiveChatMessage::time), key = LiveChatMessage::liveChattingId) { message ->
                Surface(
                    color = Color(0xFFF4F6F8),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (isAuthenticated) {
                            showComments = false
                            reportTarget = message
                            reportContent = ""
                            onDismissReport()
                        } else onLoginRequired()
                    }
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(message.nickname ?: message.memberId, color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(message.content, Modifier.padding(top = 3.dp), color = Colors.Text, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    if (showBidSheet) LiveBidSheet(currentPrice, depositPaid, { showBidSheet = false }) { submission ->
        showBidSheet = false
        auctionKey?.let { onDepositPayment(it, submission) }
    }
    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (!reportSubmitting) {
                    reportTarget = null
                    onDismissReport()
                }
            },
            title = { Text("${target.nickname ?: target.memberId} 신고") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (reportCompleted) {
                        Text("신고가 접수됐어요.", color = Colors.MintInk, fontWeight = FontWeight.Bold)
                    } else {
                        Text("신고 사유를 구체적으로 입력해주세요.", color = Colors.Muted, fontSize = 12.sp)
                        OutlinedTextField(
                            value = reportContent,
                            onValueChange = { reportContent = it.take(500) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            enabled = !reportSubmitting,
                            placeholder = { Text("욕설, 사기 유도 등") }
                        )
                        reportError?.let { Text(it, color = Colors.Live, fontSize = 11.sp) }
                    }
                }
            },
            confirmButton = {
                if (reportCompleted) {
                    TextButton(onClick = { reportTarget = null; onDismissReport() }) { Text("확인") }
                } else {
                    TextButton(
                        onClick = { liveItem?.liveBroadcastId?.let { onReportParticipant(it, target.memberId, reportContent.trim()) } },
                        enabled = reportContent.isNotBlank() && !reportSubmitting
                    ) { Text(if (reportSubmitting) "접수 중" else "신고하기") }
                }
            },
            dismissButton = {
                if (!reportCompleted) TextButton(onClick = { reportTarget = null; onDismissReport() }, enabled = !reportSubmitting) { Text("취소") }
            }
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LiveVideoBackground(streamUrl: String?, isActivePage: Boolean) {
    val context = LocalContext.current
    var playbackFailed by remember(streamUrl) { mutableStateOf(false) }
    val activePage by rememberUpdatedState(isActivePage)
    val player = remember(streamUrl) {
        streamUrl?.takeIf(String::isNotBlank)?.let { url ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isActivePage
                prepare()
            }
        }
    }
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(player, lifecycleOwner) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackFailed = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (activePage) player.play()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        player.addListener(listener)
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, isActivePage) {
        if (isActivePage) player?.play() else player?.pause()
    }

    if (player != null && !playbackFailed) {
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW
        )
    } else {
        Image(
            painterResource(R.drawable.live_video),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
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
    val parsed = amount.toIntOrNull() ?: 0
    val depositAmount = maxOf(1_000, parsed / 10)
    val valid = parsed >= minimum
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("라이브 입찰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("현재가 ${"%,d".format(currentPrice)}원 · ${"%,d".format(minimum)}원 이상", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth(), suffix = { Text("원") }, isError = amount.isNotBlank() && !valid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            BidDepositStatusNotice(depositPaid, depositAmount)
            Text("종료 30초 이내 입찰 시 종료 시간이 15초 연장돼요.", color = Colors.Muted, fontSize = 11.sp)
            Button({ onConfirm(BidSubmission(parsed)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if (depositPaid) "${"%,d".format(parsed)}원 입찰하기" else "보증금 결제로 계속", fontWeight = FontWeight.Bold) }
        }
    }
}
