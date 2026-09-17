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
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.feature.auction.BidSubmission
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.AuctionUrgencyBadge
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
    favoriteError: String?,
    favoriteUpdatingAuctionIds: Set<String>,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onLiveVisible: (String) -> Unit,
    onLoadEarlierComments: () -> Unit,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    currentMemberId: String?,
    realtimeBiddingEnabled: Boolean,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onDismissFavoriteError: () -> Unit,
    onLoginRequired: () -> Unit,
    onReportAuction: (String, String) -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
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
                    favoriteError = favoriteError,
                    favoriteUpdatingAuctionIds = favoriteUpdatingAuctionIds,
                    chatError = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatError else null,
                    chatConnectionState = if (items[page]?.liveBroadcastId == activeLiveBroadcastId) chatConnectionState else null,
                    onLoadEarlierComments = onLoadEarlierComments,
                    onSendComment = onSendComment,
                    isAuthenticated = isAuthenticated,
                    currentMemberId = currentMemberId,
                    realtimeBiddingEnabled = realtimeBiddingEnabled,
                    realtimeBidFeedback = if (page == pagerState.currentPage) realtimeBidFeedback else null,
                    onRealtimeBid = onRealtimeBid,
                    onStreamRetry = onRetry,
                    onClose = onClose,
                    onProductClick = onProductClick,
                    onFavoriteChange = onFavoriteChange,
                    onDismissFavoriteError = onDismissFavoriteError,
                    onLoginRequired = onLoginRequired,
                    onReportAuction = onReportAuction,
                    onReportParticipant = onReportParticipant,
                    onDismissReport = onDismissReport
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
    favoriteError: String?,
    favoriteUpdatingAuctionIds: Set<String>,
    chatError: String?,
    chatConnectionState: RealtimeConnectionState?,
    onLoadEarlierComments: () -> Unit,
    onSendComment: (String) -> Boolean,
    isAuthenticated: Boolean,
    currentMemberId: String?,
    realtimeBiddingEnabled: Boolean,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (String, Int) -> Boolean,
    onStreamRetry: () -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onDismissFavoriteError: () -> Unit,
    onLoginRequired: () -> Unit,
    onReportAuction: (String, String) -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAuction = liveItem?.currentAuction
    val productAuctions = liveAuctions ?: listOfNotNull(activeAuction)
    val isSampleContent = liveItem == null
    val auctionKey = activeAuction?.auctionId ?: if (liveItem == null) "camera" else null
    val hasActiveAuction = auctionKey != null && (
        liveItem == null || activeAuction?.status.equals("ACTIVE", ignoreCase = true)
    )
    val isOwnAuction = currentMemberId != null && (
        activeAuction?.sellerMemberId == currentMemberId || liveItem?.memberId == currentMemberId
    )
    val isHighestBidder = activeAuction?.isHighestBidder == true
    var favorite by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(activeAuction?.bookmarked == true) }
    var showProducts by rememberSaveable { mutableStateOf(false) }
    var showComments by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var showReportTypes by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var reportAuctionId by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf<String?>(null) }
    var productReportReason by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf("상품 정보가 실제와 달라요") }
    var showProductReportReasons by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var currentPrice by rememberSaveable(liveItem?.liveBroadcastId) {
        mutableIntStateOf(activeAuction?.currentPrice?.takeIf { it > 0 } ?: activeAuction?.startPrice ?: if (isSampleContent) 34_500 else 0)
    }
    var remaining by rememberSaveable(liveItem?.liveBroadcastId) {
        mutableIntStateOf(activeAuction?.remainingSeconds ?: if (isSampleContent) 42 else 0)
    }
    var comment by rememberSaveable { mutableStateOf("") }
    var showBidFeedback by remember { mutableStateOf(false) }
    var bidFeedbackAccepted by remember { mutableStateOf(true) }
    var bidFeedbackMessage by remember { mutableStateOf("") }
    var bidSubmitting by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<LiveChatMessage?>(null) }
    var reportContent by rememberSaveable { mutableStateOf("") }
    var memberReportReason by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf("욕설·사기 유도") }
    var showMemberReportReasons by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    val livePulse = rememberInfiniteTransition(label = "livePulse")
    val liveDotAlpha by livePulse.animateFloat(
        initialValue = .4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(Unit) { while (remaining > 0) { delay(1_000); remaining-- } }
    LaunchedEffect(realtimeBidFeedback?.eventKey) {
        realtimeBidFeedback ?: return@LaunchedEffect
        bidSubmitting = false
        bidFeedbackAccepted = realtimeBidFeedback.accepted
        bidFeedbackMessage = realtimeBidFeedback.message.ifBlank {
            if (realtimeBidFeedback.accepted) "입찰이 접수됐어요." else "입찰이 반영되지 않았어요."
        }
        if (realtimeBidFeedback.accepted) {
            realtimeBidFeedback.currentPrice?.let { currentPrice = it }
        }
        showBidFeedback = true
    }
    LaunchedEffect(bidSubmitting) {
        if (bidSubmitting) {
            delay(10_000L)
            if (bidSubmitting) {
                bidSubmitting = false
                bidFeedbackAccepted = false
                bidFeedbackMessage = "입찰 응답이 늦어지고 있어요. 현재가를 확인한 뒤 다시 시도해주세요."
                showBidFeedback = true
            }
        }
    }
    LaunchedEffect(activeAuction?.auctionId, activeAuction?.currentPrice, activeAuction?.remainingSeconds, isSampleContent) {
        currentPrice = activeAuction?.currentPrice?.takeIf { it > 0 }
            ?: activeAuction?.startPrice
            ?: if (isSampleContent) 34_500 else 0
        remaining = activeAuction?.remainingSeconds ?: if (isSampleContent) 42 else 0
    }
    LaunchedEffect(activeAuction?.auctionId, activeAuction?.bookmarked) {
        favorite = activeAuction?.bookmarked == true
    }
    LaunchedEffect(showBidFeedback) {
        if (showBidFeedback) {
            delay(1_500)
            showBidFeedback = false
        }
    }

    Box(modifier.fillMaxSize().safeDrawingPadding().background(Color(0xFF17212D))) {
        LiveVideoBackground(
            streamUrl = liveItem?.streamUrl,
            fallbackImageUrl = activeAuction?.imageUrls?.firstOrNull(),
            fallbackTitle = activeAuction?.title.orEmpty(),
            streamExpected = liveItem != null,
            isActivePage = isActivePage,
            onRetry = onStreamRetry,
            onExit = onClose
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.12f), Color.Transparent, Color(0xFF07101D).copy(.72f)))))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Colors.Live, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(7.dp).graphicsLayer(alpha = liveDotAlpha).background(Color.White, CircleShape))
                        Text("LIVE", color = Color.White, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("${"%,d".format(liveItem?.viewCount ?: 1_248)}명 시청 중", Modifier.padding(start = 9.dp), color = Color.White.copy(alpha = .9f), fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Image(painterResource(R.drawable.close), "Live 닫기", Modifier.size(44.dp).clickable(onClick = onClose).padding(10.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(Color(0xFFBDEEDF), CircleShape), contentAlignment = Alignment.Center) { Text("d", color = Color(0xFF13284B), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Text(liveItem?.title ?: "하루공방", Modifier.padding(horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        AnimatedVisibility(!imeVisible, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 76.dp, bottom = 230.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (liveItem == null) {
                    listOf("도윤  포장 상태 궁금해요", "nana***  다음 상품도 기대돼요", "haeun9***  가격 실화인가요?").forEach { message ->
                        Surface(color = Color.Black.copy(alpha = .24f), shape = RoundedCornerShape(10.dp)) {
                            Text(message, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
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
                            color = Color.Black.copy(alpha = .24f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable(enabled = message.memberId != currentMemberId) {
                                if (isAuthenticated) {
                                    reportTarget = message
                                    reportContent = ""
                                    onDismissReport()
                                } else onLoginRequired()
                            }
                        ) {
                            Text("${message.nickname ?: message.memberId}  ${message.content}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(!imeVisible, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 218.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LiveFavoriteAction(favorite, enabled = auctionKey != null && auctionKey !in favoriteUpdatingAuctionIds) {
                    when {
                        !isAuthenticated -> onLoginRequired()
                        auctionKey != null -> {
                            val selected = !favorite
                            favorite = selected
                            onFavoriteChange(auctionKey, selected)
                        }
                    }
                }
                LiveAction(R.drawable.product_outline, "상품") { showProducts = true }
                LiveAction(R.drawable.report_outline, "신고") {
                    if (isAuthenticated) showReportTypes = true else onLoginRequired()
                }
            }
        }
        AnimatedVisibility(
            visible = favoriteError != null,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 62.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color.Black.copy(alpha = .72f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.clickable(onClick = onDismissFavoriteError)
            ) {
                Text(
                    favoriteError.orEmpty(),
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).imePadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedVisibility(!imeVisible, enter = fadeIn(), exit = fadeOut()) {
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().heightIn(min = 126.dp).animateContentSize()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showProducts = true }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (hasActiveAuction) "진행 중인 경매" else "경매 준비 중", color = Colors.Navy, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("상품 ${productAuctions.size.coerceAtLeast(if (hasActiveAuction) 1 else 0)}개", color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        DibNetworkImage(
                            activeAuction?.imageUrls?.firstOrNull(),
                            activeAuction?.title,
                            Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                activeAuction?.title ?: if (isSampleContent) "달빛 유약 머그컵" else "다음 경매를 준비하고 있어요",
                                color = Colors.Navy,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (hasActiveAuction) "현재가 ${"%,d".format(currentPrice)}원" else "판매자가 경매를 시작하면 참여할 수 있어요",
                                color = if (hasActiveAuction) Colors.Navy else Colors.Muted,
                                fontSize = if (hasActiveAuction) 15.sp else 11.sp,
                                lineHeight = if (hasActiveAuction) 20.sp else 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasActiveAuction) AuctionUrgencyBadge(remaining, compact = true)
                        }
                        Button(
                            onClick = { if (isAuthenticated) showBidSheet = true else onLoginRequired() },
                            enabled = hasActiveAuction && remaining > 0 && !isOwnAuction && !isHighestBidder && !bidSubmitting && realtimeBiddingEnabled,
                            modifier = Modifier.width(76.dp).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (remaining <= 15) Colors.Live else Colors.Navy),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                when {
                                    isOwnAuction -> "내 경매"
                                    isHighestBidder -> "최고가"
                                    !hasActiveAuction -> "대기 중"
                                    !realtimeBiddingEnabled -> "연결 필요"
                                    bidSubmitting -> "요청 중"
                                    remaining <= 15 -> "지금\n입찰"
                                    else -> "입찰"
                                },
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            }
            if (isAuthenticated && chatConnectionState != RealtimeConnectionState.Connected) Text(chatError ?: "Live 채팅 연결 중", color = Color.White.copy(.75f), fontSize = 9.sp)
            Row(Modifier.fillMaxWidth().height(46.dp).background(Color.Black.copy(.48f), RoundedCornerShape(23.dp)).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = comment, onValueChange = { comment = it.take(500) }, Modifier.weight(1f), enabled = isAuthenticated, singleLine = true, textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp), decorationBox = { inner -> if (comment.isBlank()) Text(if (isAuthenticated) "댓글을 입력하세요" else "로그인 후 댓글을 작성할 수 있어요", color = Color.White.copy(.75f), fontSize = 12.sp); inner() })
                Box(Modifier.size(34.dp).background(if (comment.isNotBlank()) Colors.Mint else Color.White, CircleShape).clickable { if (!isAuthenticated) onLoginRequired() else if (comment.isNotBlank() && onSendComment(comment)) comment = "" }, contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.send), "댓글 전송", Modifier.size(17.dp), colorFilter = ColorFilter.tint(Colors.Navy))
                }
            }
        }
        AnimatedVisibility(showBidFeedback, Modifier.align(Alignment.Center), enter = fadeIn() + scaleIn(initialScale = .7f), exit = fadeOut() + scaleOut(targetScale = .82f)) {
            Surface(color = if (bidFeedbackAccepted) Colors.Mint else Colors.UrgentBackground, shape = RoundedCornerShape(22.dp), shadowElevation = 10.dp) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (bidFeedbackAccepted) "최고가 갱신!" else "입찰을 확인해주세요", color = if (bidFeedbackAccepted) Colors.MintInk else Colors.Urgent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(bidFeedbackMessage, color = if (bidFeedbackAccepted) Colors.MintInk else Colors.Urgent, fontSize = 12.sp)
                    if (bidFeedbackAccepted) Text("현재 최고가 ${"%,d".format(currentPrice)}원", color = Colors.MintInk, fontSize = 11.sp)
                }
            }
        }
    }

    if (showReportTypes) ModalBottomSheet(onDismissRequest = { showReportTypes = false }, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("무엇을 신고할까요?", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                auctionKey?.let { "경매 ID $it" } ?: "현재 진행 중인 경매가 없어요.",
                color = Colors.Muted,
                fontSize = 10.sp
            )
            LiveReportTypeAction(
                title = "상품 신고",
                description = "현재 경매 상품의 정보·설명을 신고",
                enabled = auctionKey != null
            ) {
                showReportTypes = false
                reportAuctionId = auctionKey
                productReportReason = "상품 정보가 실제와 달라요"
                showProductReportReasons = false
                reportContent = ""
                onDismissReport()
            }
            LiveReportTypeAction(
                title = "회원·채팅 신고",
                description = "신고할 댓글의 작성자와 메시지를 선택",
                enabled = true
            ) {
                showReportTypes = false
                showComments = true
            }
        }
    }
    reportAuctionId?.let { auctionId ->
        val detailLimit = reportDetailLimit(productReportReason, null)
        ModalBottomSheet(
            onDismissRequest = {
                if (!reportSubmitting) {
                    reportAuctionId = null
                    showProductReportReasons = false
                    onDismissReport()
                }
            },
            containerColor = Color(0xFFF8F9FB)
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("상품 신고", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("신고 대상", color = Colors.Muted, fontSize = 10.sp)
                        Text("현재 경매 상품", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("경매 ID  $auctionId", color = Colors.Muted, fontSize = 10.sp)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !reportSubmitting && !reportCompleted) {
                        showProductReportReasons = !showProductReportReasons
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("신고 사유", color = Colors.Muted, fontSize = 10.sp)
                        Text("$productReportReason  ›", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showProductReportReasons && !reportCompleted) {
                    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            productReportReasons.forEach { reason ->
                                Text(
                                    reason,
                                    Modifier.fillMaxWidth().clickable {
                                        productReportReason = reason
                                        showProductReportReasons = false
                                        reportContent = reportContent.take(reportDetailLimit(reason, null))
                                    }.padding(horizontal = 14.dp, vertical = 11.dp),
                                    color = if (reason == productReportReason) Colors.Navy else Colors.Text,
                                    fontSize = 13.sp,
                                    fontWeight = if (reason == productReportReason) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                if (reportCompleted) {
                    Surface(Modifier.fillMaxWidth(), color = Colors.Mint.copy(alpha = .22f), shape = RoundedCornerShape(14.dp)) {
                        Text("신고가 접수됐어요.", Modifier.padding(16.dp), color = Colors.MintInk, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = reportContent,
                        onValueChange = { reportContent = it.take(detailLimit) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        enabled = !reportSubmitting,
                        label = { Text("상세 내용") },
                        placeholder = { Text("신고 내용을 구체적으로 입력해주세요.") },
                        supportingText = { Text("${reportContent.length}/$detailLimit") }
                    )
                    reportError?.let { Text(it, color = Colors.Live, fontSize = 11.sp) }
                }
                Button(
                    onClick = {
                        if (reportCompleted) {
                            reportAuctionId = null
                            onDismissReport()
                        } else {
                            onReportAuction(auctionId, buildReportContent(productReportReason, null, reportContent))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = reportCompleted || (reportContent.isNotBlank() && !reportSubmitting),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (reportCompleted) "확인" else if (reportSubmitting) "접수 중" else "신고 접수", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showProducts) ModalBottomSheet(onDismissRequest = { showProducts = false }, containerColor = Color.White) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).navigationBarsPadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val displayedAuctions = if (liveItem == null && productAuctions.isEmpty()) emptyList() else productAuctions
            item { Text("라이브 상품 ${if (liveItem == null) 5 else displayedAuctions.size}개", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            if (productListLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Mint) }
            productListError?.let { message -> item { Text(message, color = Colors.Live, fontSize = 12.sp) } }
            if (liveItem != null && !productListLoading && displayedAuctions.isEmpty()) item { Text("편성된 상품이 없어요.", color = Colors.Muted, fontSize = 13.sp) }
            items(if (liveItem == null) 5 else displayedAuctions.size) { index ->
                val auction = displayedAuctions.getOrNull(index)
                Row(Modifier.fillMaxWidth().height(76.dp).background(if (auction?.auctionId == activeAuction?.auctionId || (auction == null && index == 1)) Colors.MintSoft else Color.Transparent, RoundedCornerShape(14.dp)).clickable {
                    showProducts = false
                    val targetAuctionId = auction?.auctionId ?: if (liveItem == null) {
                        if (index == 1) "camera" else "headphones"
                    } else null
                    if (!isAuthenticated) onLoginRequired()
                    else targetAuctionId?.let(onProductClick)
                }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.fillMaxWidth().clickable(enabled = message.memberId != currentMemberId) {
                        if (isAuthenticated) {
                            showComments = false
                            reportTarget = message
                            memberReportReason = "욕설·사기 유도"
                            showMemberReportReasons = false
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
    if (showBidSheet) LiveBidSheet(currentPrice, { showBidSheet = false }) { submission ->
        when {
            !realtimeBiddingEnabled -> {
                bidFeedbackAccepted = false
                bidFeedbackMessage = "실시간 입찰 연결을 사용할 수 없어요."
                showBidFeedback = true
            }
            !isSampleContent && chatConnectionState != RealtimeConnectionState.Connected -> {
                bidFeedbackAccepted = false
                bidFeedbackMessage = "실시간 연결 중이에요. 연결된 뒤 다시 시도해주세요."
                showBidFeedback = true
            }
            auctionKey != null && onRealtimeBid(auctionKey, submission.amount) -> {
                showBidSheet = false
                bidSubmitting = true
            }
            else -> {
                bidFeedbackAccepted = false
                bidFeedbackMessage = "입찰 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요."
                showBidFeedback = true
            }
        }
    }
    reportTarget?.let { target ->
        val detailLimit = reportDetailLimit(memberReportReason, target.content)
        ModalBottomSheet(
            onDismissRequest = {
                if (!reportSubmitting) {
                    reportTarget = null
                    showMemberReportReasons = false
                    onDismissReport()
                }
            },
            containerColor = Color(0xFFF8F9FB)
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("회원·채팅 신고", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("신고 대상 회원", color = Colors.Muted, fontSize = 10.sp)
                        Text("@${target.nickname ?: target.memberId}", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("라이브 채팅 참여자", color = Colors.Muted, fontSize = 10.sp)
                    }
                }
                Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("신고할 메시지", color = Colors.Muted, fontSize = 10.sp)
                        Text("“${target.content}”", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !reportSubmitting && !reportCompleted) {
                        showMemberReportReasons = !showMemberReportReasons
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("신고 사유", color = Colors.Muted, fontSize = 10.sp)
                        Text("$memberReportReason  ›", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showMemberReportReasons && !reportCompleted) {
                    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            memberReportReasons.forEach { reason ->
                                Text(
                                    reason,
                                    Modifier.fillMaxWidth().clickable {
                                        memberReportReason = reason
                                        showMemberReportReasons = false
                                        reportContent = reportContent.take(reportDetailLimit(reason, target.content))
                                    }.padding(horizontal = 14.dp, vertical = 11.dp),
                                    color = if (reason == memberReportReason) Colors.Navy else Colors.Text,
                                    fontSize = 13.sp,
                                    fontWeight = if (reason == memberReportReason) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                if (reportCompleted) {
                    Surface(Modifier.fillMaxWidth(), color = Colors.Mint.copy(alpha = .22f), shape = RoundedCornerShape(14.dp)) {
                        Text("신고가 접수됐어요.", Modifier.padding(16.dp), color = Colors.MintInk, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = reportContent,
                        onValueChange = { reportContent = it.take(detailLimit) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        enabled = !reportSubmitting,
                        label = { Text("상세 내용") },
                        placeholder = { Text("신고 내용을 구체적으로 입력해주세요.") },
                        supportingText = { Text("${reportContent.length}/$detailLimit") }
                    )
                    reportError?.let { Text(it, color = Colors.Live, fontSize = 11.sp) }
                }
                Button(
                    onClick = {
                        if (reportCompleted) {
                            reportTarget = null
                            onDismissReport()
                        } else {
                            liveItem?.liveBroadcastId?.let {
                                onReportParticipant(it, target.memberId, buildReportContent(memberReportReason, target.content, reportContent))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = reportCompleted || (reportContent.isNotBlank() && !reportSubmitting),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (reportCompleted) "확인" else if (reportSubmitting) "접수 중" else "회원 신고 접수", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val productReportReasons = listOf(
    "상품 정보가 실제와 달라요",
    "위조품이 의심돼요",
    "판매가 금지된 상품이에요",
    "부적절한 이미지나 설명이 있어요",
    "기타"
)

private val memberReportReasons = listOf(
    "욕설·사기 유도",
    "괴롭힘·혐오 표현",
    "개인정보 노출",
    "스팸·광고",
    "기타"
)

private fun reportDetailLimit(reason: String, evidence: String?): Int =
    (500 - reportContentPrefix(reason, evidence).length).coerceAtLeast(0)

private fun buildReportContent(reason: String, evidence: String?, detail: String): String =
    (reportContentPrefix(reason, evidence) + detail.trim()).take(500)

private fun reportContentPrefix(reason: String, evidence: String?): String = buildString {
    append("[신고 사유] ").append(reason).append('\n')
    evidence?.let {
        append("[신고할 메시지] ").append(it.take(220))
        if (it.length > 220) append('…')
        append('\n')
    }
    append("[상세 내용] ")
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LiveVideoBackground(
    streamUrl: String?,
    fallbackImageUrl: String?,
    fallbackTitle: String,
    streamExpected: Boolean,
    isActivePage: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var playbackFailed by remember(streamUrl) { mutableStateOf(false) }
    var retryCount by remember(streamUrl) { mutableIntStateOf(0) }
    var retryKey by remember(streamUrl) { mutableIntStateOf(0) }
    val activePage by rememberUpdatedState(isActivePage)
    val player = remember(streamUrl, retryKey) {
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
                retryCount++
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
    LaunchedEffect(playbackFailed, retryCount, isActivePage) {
        if (playbackFailed && isActivePage && retryCount < LIVE_STREAM_MAX_RETRIES) {
            delay(LIVE_STREAM_RETRY_DELAY_MILLIS)
            playbackFailed = false
            retryKey++
        }
    }

    val streamMissing = streamExpected && streamUrl.isNullOrBlank()
    val recoveryFailed = streamMissing || (playbackFailed && retryCount >= LIVE_STREAM_MAX_RETRIES)
    val reconnecting = playbackFailed && !recoveryFailed

    if (player != null && !playbackFailed) {
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW
        )
    } else {
        if (!fallbackImageUrl.isNullOrBlank()) {
            DibNetworkImage(fallbackImageUrl, fallbackTitle, Modifier.fillMaxSize())
        } else {
            Image(
                painterResource(R.drawable.live_video),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
    if (reconnecting) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.padding(horizontal = 48.dp),
                color = Color(0xFF101C2C).copy(alpha = .92f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("라이브 연결이 불안정해요", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("대표 이미지로 경매를 계속 보여드려요", color = Color.White.copy(alpha = .78f), fontSize = 10.sp)
                    Text("재연결 중 · $retryCount/$LIVE_STREAM_MAX_RETRIES", color = Colors.Mint, fontSize = 10.sp)
                }
            }
        }
    }
    if (recoveryFailed && isActivePage) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("라이브 연결을 복구하지 못했어요") },
            text = { Text("네트워크를 확인한 뒤 다시 시도해 주세요.") },
            confirmButton = {
                Button(
                    onClick = {
                        retryCount = 0
                        playbackFailed = false
                        retryKey++
                        onRetry()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) { Text("다시 연결") }
            },
            dismissButton = { TextButton(onClick = onExit) { Text("라이브 나가기") } }
        )
    }
}

private const val LIVE_STREAM_MAX_RETRIES = 3
private const val LIVE_STREAM_RETRY_DELAY_MILLIS = 2_000L

@Composable
private fun LiveReportTypeAction(title: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFF8F9FB),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(62.dp).graphicsLayer(alpha = if (enabled) 1f else .5f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Colors.Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LiveAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(48.dp, 58.dp).background(Color.Black.copy(.32f), RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), label, Modifier.size(21.dp), colorFilter = ColorFilter.tint(Color.White))
        Text(label, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
private fun LiveFavoriteAction(selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    DibWishlistButton(
        selected = selected,
        onSelectedChange = { onClick() },
        productName = "라이브 상품",
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LiveBidSheet(currentPrice: Int, onDismiss: () -> Unit, onConfirm: (BidSubmission) -> Unit) {
    val minimum = currentPrice + 1
    var amount by rememberSaveable(currentPrice) { mutableStateOf(minimum.toString()) }
    val parsed = amount.toIntOrNull() ?: 0
    val valid = parsed >= minimum
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("라이브 입찰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("현재가 ${"%,d".format(currentPrice)}원 · ${"%,d".format(minimum)}원 이상", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth(), suffix = { Text("원") }, isError = amount.isNotBlank() && !valid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            Text("입찰 후에는 취소할 수 없어요. 낙찰되면 등록된 카드로 낙찰가 전액을 자동결제해요.\n종료 30초 이내 입찰 시 종료 시간이 15초 연장돼요.", color = Colors.Muted, fontSize = 11.sp, lineHeight = 17.sp)
            Button({ onConfirm(BidSubmission(parsed)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("${"%,d".format(parsed)}원 입찰하기", fontWeight = FontWeight.Bold) }
        }
    }
}
