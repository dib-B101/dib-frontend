package com.ssafy.dib.feature.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.feature.auction.BidSubmission
import com.ssafy.dib.feature.auction.BID_NOTICES
import com.ssafy.dib.feature.auction.AutoPayConsentRow
import com.ssafy.dib.feature.auction.isValidBidAmount
import com.ssafy.dib.feature.auction.minimumBidAmount
import com.ssafy.dib.feature.auction.roundUpToBidUnit
import com.ssafy.dib.feature.auction.steppedBidAmount
import com.ssafy.dib.feature.auction.RealtimeBidFeedback
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.domain.live.LiveFeedItem
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.domain.live.LiveStreamSession
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.AnimatedAuctionPrice
import com.ssafy.dib.core.ui.AuctionUrgencyProgress
import com.ssafy.dib.core.ui.BidMotionTone
import com.ssafy.dib.core.ui.auctionUrgencyColor
import com.ssafy.dib.core.ui.auctionUrgencySurface
import com.ssafy.dib.core.time.formatRemainingTime
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.feature.live.LiveVideoRole
import com.ssafy.dib.feature.live.LiveVideoState
import com.ssafy.dib.feature.live.LiveVideoSurface
import com.ssafy.dib.feature.live.liveChatSpeakerLabel
import com.ssafy.dib.feature.live.currentLiveNotice
import com.ssafy.dib.feature.live.isLiveNoticeControl
import com.ssafy.dib.feature.live.rememberLiveVideoSession
import com.ssafy.dib.ui.theme.WireframeColors as Colors
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
    streamTokenProvider: (suspend (String) -> Result<LiveStreamSession>)?,
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
    onSellerClick: (String) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onDismissFavoriteError: () -> Unit,
    onLoginRequired: () -> Unit,
    onReportAuction: (String, String) -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    // 홈 "지금 LIVE" 에서 고른 방송. 목록에 있으면 그 페이지로 바로 넘긴다
    focusLiveBroadcastId: String? = null,
    modifier: Modifier = Modifier
) {
    // 라이브를 실제로 보고 있을 때만 몰입 모드(전체 화면)로 둔다.
    //
    // 예전에는 피드 탭 전체가 몰입 모드였다. 그래서 방송이 하나도 없을 때도 하단 내비가
    // 없어 **다른 탭으로 갈 방법이 '홈으로' 버튼 하나뿐**이었다 (QA #22).
    // 화면을 가릴 이유는 재생 중인 영상이 있을 때뿐이므로, 나머지 상태에서는 하단 내비를 둔다.
    //
    // `null` 은 "아직 서버를 안 붙였다"(프리뷰·데모)라서 예전처럼 표본 페이지를 보여준다.
    // 빈 목록(`emptyList`)과 뜻이 다르므로 함께 묶지 않는다.
    //
    // 첫 조회 중에는 아직 보여줄 것이 없으니 표본으로 넘어가지 않는다. 반대로 이미 목록을
    // 받아 둔 상태의 재조회는 당겨서 새로고침의 인디케이터로 보이면 되므로 화면을 가리지 않는다.
    val isFirstLoad = isLoading && remoteItems == null
    val immersive = !isFirstLoad && errorMessage == null &&
        (remoteItems == null || remoteItems.isNotEmpty())

    if (immersive) {
        LiveFeedPager(
            remoteItems = remoteItems,
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            hasNextPage = hasNextPage,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
            onLoadMore = onLoadMore,
            activeLiveBroadcastId = activeLiveBroadcastId,
            streamTokenProvider = streamTokenProvider,
            liveComments = liveComments,
            chatHasMore = chatHasMore,
            chatLoadingEarlier = chatLoadingEarlier,
            chatLoadEarlierError = chatLoadEarlierError,
            liveAuctionsByBroadcast = liveAuctionsByBroadcast,
            productListLoading = productListLoading,
            productListError = productListError,
            reportSubmitting = reportSubmitting,
            reportError = reportError,
            reportCompleted = reportCompleted,
            favoriteError = favoriteError,
            favoriteUpdatingAuctionIds = favoriteUpdatingAuctionIds,
            chatError = chatError,
            chatConnectionState = chatConnectionState,
            onLiveVisible = onLiveVisible,
            onLoadEarlierComments = onLoadEarlierComments,
            onSendComment = onSendComment,
            isAuthenticated = isAuthenticated,
            currentMemberId = currentMemberId,
            realtimeBiddingEnabled = realtimeBiddingEnabled,
            realtimeBidFeedback = realtimeBidFeedback,
            onRealtimeBid = onRealtimeBid,
            onClose = onClose,
            onProductClick = onProductClick,
            onSellerClick = onSellerClick,
            onFavoriteChange = onFavoriteChange,
            onDismissFavoriteError = onDismissFavoriteError,
            onLoginRequired = onLoginRequired,
            onReportAuction = onReportAuction,
            onReportParticipant = onReportParticipant,
            onDismissReport = onDismissReport,
            focusLiveBroadcastId = focusLiveBroadcastId,
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFF17212D),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { DibBottomNavigation(DibMainTab.Feed, onTabSelected) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isFirstLoad -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Mint) }
                errorMessage != null -> LiveFeedMessage(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh
                ) {
                    Text(errorMessage, color = Color.White)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기", color = Color.White) }
                }
                // 방송이 없는 것은 오류가 아니다. 당겨서 새로고침으로 **판매자가 방금 시작한
                // 방송을 직접 확인**할 수 있게 한다 (QA #19).
                else -> LiveFeedMessage(
                    isRefreshing = isLoading,
                    onRefresh = onRefresh
                ) {
                    Text("현재 방송 중인 Live가 없어요.", color = Color.White)
                    Text(
                        "아래로 당기면 새로고침돼요.",
                        color = Color.White.copy(alpha = .6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 피드의 안내 화면(오류 · 방송 없음) 공통 틀.
 *
 * 당겨서 새로고침이 동작하려면 안쪽에 스크롤 가능한 것이 있어야 한다. 화면을 가득 채우는
 * 항목 하나짜리 `LazyColumn` 을 쓰는 이유가 그것이다 — 내용이 짧아도 제스처가 잡힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedMessage(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DibPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillParentMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedPager(
    remoteItems: List<LiveFeedItem>?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    hasNextPage: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    activeLiveBroadcastId: String?,
    streamTokenProvider: (suspend (String) -> Result<LiveStreamSession>)?,
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
    onSellerClick: (String) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onDismissFavoriteError: () -> Unit,
    onLoginRequired: () -> Unit,
    onReportAuction: (String, String) -> Unit,
    onReportParticipant: (String, String, String) -> Unit,
    onDismissReport: () -> Unit,
    focusLiveBroadcastId: String? = null,
    modifier: Modifier = Modifier
) {
    // null 이면 표본 한 장. 기존 동작을 그대로 옮긴 것이다.
    val items = remoteItems ?: listOf(null)
    val pagerState = rememberPagerState(pageCount = items::size)
    // 홈에서 고른 방송으로 바로 넘긴다. 목록이 채워진 뒤에야 인덱스를 알 수 있어 items 도 키에 둔다
    LaunchedEffect(focusLiveBroadcastId, items) {
        val index = focusLiveBroadcastId?.let { id -> items.indexOfFirst { it?.liveBroadcastId == id } } ?: -1
        if (index >= 0 && index != pagerState.currentPage) pagerState.scrollToPage(index)
    }
    LaunchedEffect(pagerState.currentPage, remoteItems, hasNextPage, isLoadingMore, loadMoreError) {
        items[pagerState.currentPage]?.liveBroadcastId?.let(onLiveVisible)
        if (remoteItems != null && hasNextPage && !isLoadingMore && loadMoreError == null && pagerState.currentPage >= items.lastIndex - 1) {
            onLoadMore()
        }
    }
    DibPullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { page -> items[page]?.liveBroadcastId ?: "sample" }) { page ->
            LiveFeedPage(
            liveItem = items[page],
            isActivePage = page == pagerState.currentPage,
            streamTokenProvider = streamTokenProvider,
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
            onClose = onClose,
            onProductClick = onProductClick,
            onSellerClick = onSellerClick,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedPage(
    liveItem: LiveFeedItem?,
    isActivePage: Boolean,
    streamTokenProvider: (suspend (String) -> Result<LiveStreamSession>)?,
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
    onClose: () -> Unit,
    onProductClick: (String) -> Unit,
    onSellerClick: (String) -> Unit,
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
    val sellerMemberId = liveItem?.memberId?.takeIf(String::isNotBlank)
    // 판매자가 "공지로 설정"한 댓글만 공지로 띄운다 (규약은 currentLiveNotice 참고)
    val sellerNotice = currentLiveNotice(liveComments, sellerMemberId)
    val isOwnAuction = currentMemberId != null && (
        activeAuction?.sellerMemberId == currentMemberId || liveItem?.memberId == currentMemberId
    )
    var isHighestBidder by remember(auctionKey) { mutableStateOf(activeAuction?.isHighestBidder == true) }
    var bidMotionTone by remember(auctionKey) { mutableStateOf(BidMotionTone.Neutral) }
    var bidMotionSequence by remember(auctionKey) { mutableIntStateOf(0) }
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
    // 댓글 끄기. 영상만 보고 싶을 때 댓글 목록을 숨긴다(판매자 공지는 그대로 둔다). 방송을 넘겨도 설정을 유지한다
    var showChat by rememberSaveable { mutableStateOf(true) }
    // 찜 버튼을 누른 결과 안내. 하트가 "방송 좋아요"가 아니라 "현재 경매 상품 찜"이라는 걸 알려준다
    var favoriteHint by remember { mutableStateOf<String?>(null) }
    var showBidFeedback by remember { mutableStateOf(false) }
    var showFavoriteBurst by remember { mutableStateOf(false) }
    var bidFeedbackMessage by remember { mutableStateOf("") }
    var bidSubmitting by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf(false) }
    var autoPayAgreedAuctionId by rememberSaveable(liveItem?.liveBroadcastId) { mutableStateOf<String?>(null) }
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
    val auctionUrgent = hasActiveAuction && remaining in 1..15
    val cardSurface by animateColorAsState(
        when {
            bidMotionTone == BidMotionTone.Success -> Colors.MintSoft
            bidMotionTone == BidMotionTone.Outbid -> Colors.UrgentBackground
            auctionUrgent -> auctionUrgencySurface(remaining)
            else -> Color.White
        },
        tween(350), label = "liveAuctionCardSurface"
    )
    val cardBorder by animateColorAsState(
        when {
            bidMotionTone == BidMotionTone.Success -> Colors.Mint
            bidMotionTone == BidMotionTone.Outbid -> Colors.Urgent
            auctionUrgent -> auctionUrgencyColor(remaining)
            else -> Colors.Border
        },
        tween(350), label = "liveAuctionCardBorder"
    )

    // 0초에서 끝내지 않고 계속 돈다. 예전엔 방송에 들어올 때 진행 중인 경매가 없으면(0초) 루프가 바로 끝나,
    // 판매자가 방송 중에 경매를 시작해 남은 시간이 새로 들어와도 숫자가 멈춰 있었다
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            if (remaining > 0) remaining--
        }
    }
    LaunchedEffect(realtimeBidFeedback?.eventKey) {
        realtimeBidFeedback ?: return@LaunchedEffect
        bidSubmitting = false
        bidFeedbackMessage = realtimeBidFeedback.message.ifBlank {
            if (realtimeBidFeedback.accepted) "입찰이 접수됐어요." else "입찰이 반영되지 않았어요."
        }
        if (realtimeBidFeedback.accepted) {
            realtimeBidFeedback.currentPrice?.let { currentPrice = it }
            isHighestBidder = true
            bidMotionTone = BidMotionTone.Success
            bidMotionSequence++
        }
        showBidFeedback = !realtimeBidFeedback.accepted
    }
    LaunchedEffect(bidSubmitting) {
        if (bidSubmitting) {
            delay(10_000L)
            if (bidSubmitting) {
                bidSubmitting = false
                bidFeedbackMessage = "입찰 응답이 늦어지고 있어요. 현재가를 확인한 뒤 다시 시도해주세요."
                showBidFeedback = true
            }
        }
    }
    LaunchedEffect(activeAuction?.auctionId, activeAuction?.currentPrice, activeAuction?.remainingSeconds, isSampleContent) {
        val incomingPrice = activeAuction?.currentPrice?.takeIf { it > 0 }
            ?: activeAuction?.startPrice
            ?: if (isSampleContent) 34_500 else 0
        if (activeAuction != null) {
            val outbid = isHighestBidder && activeAuction.isHighestBidder == false && incomingPrice > currentPrice
            if (outbid) {
                bidMotionTone = BidMotionTone.Outbid
                bidMotionSequence++
            }
            if (activeAuction.isHighestBidder == true || outbid || !isHighestBidder) {
                isHighestBidder = activeAuction.isHighestBidder == true
            }
        }
        currentPrice = incomingPrice
        remaining = activeAuction?.remainingSeconds ?: if (isSampleContent) 42 else 0
    }
    LaunchedEffect(bidMotionSequence) {
        if (bidMotionSequence > 0) {
            delay(2_100)
            bidMotionTone = BidMotionTone.Neutral
        }
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
    LaunchedEffect(favoriteHint) {
        if (favoriteHint != null) {
            delay(2_200)
            favoriteHint = null
        }
    }
    LaunchedEffect(showFavoriteBurst) {
        if (showFavoriteBurst) {
            delay(700)
            showFavoriteBurst = false
        }
    }

    Box(modifier.fillMaxSize().safeDrawingPadding().background(Color(0xFF17212D))) {
        LiveVideoBackground(
            liveBroadcastId = liveItem?.liveBroadcastId,
            streamTokenProvider = streamTokenProvider,
            fallbackImageUrl = activeAuction?.imageUrls?.firstOrNull(),
            fallbackTitle = activeAuction?.title.orEmpty(),
            isActivePage = isActivePage
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.12f), Color.Transparent, Color(0xFF07101D).copy(.72f)))))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Colors.Live, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(7.dp).graphicsLayer(alpha = liveDotAlpha).background(Color.White, CircleShape))
                        Text("LIVE", color = Color.White, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("${"%,d".format(liveItem?.viewCount ?: 1_248)}명 시청 중", Modifier.padding(start = 9.dp), color = Color.White.copy(alpha = .9f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Image(painterResource(R.drawable.close), "Live 닫기", Modifier.size(44.dp).clickable(onClick = onClose).padding(10.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            // 방송 제목 줄을 누르면 판매자 프로필로 간다. 예전엔 모양만 프로필이고 눌러도 반응이 없었다
            Row(
                Modifier.padding(top = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = sellerMemberId != null) { sellerMemberId?.let(onSellerClick) }
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp).background(Color(0xFFBDEEDF), CircleShape), contentAlignment = Alignment.Center) { Text((liveItem?.title ?: "하루공방").take(1), color = Color(0xFF13284B), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Text(liveItem?.title ?: "하루공방", Modifier.padding(start = 8.dp).weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
                if (sellerMemberId != null) Image(painterResource(R.drawable.chevron_right), "판매자 프로필 보기", Modifier.padding(start = 2.dp).size(16.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = .85f)))
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart)
                .imePadding()
                .padding(start = 16.dp, end = 82.dp, bottom = if (imeVisible) 76.dp else 275.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                if (liveItem == null) {
                    listOf("도윤  포장 상태 궁금해요", "nana***  다음 상품도 기대돼요", "haeun9***  가격 실화인가요?")
                        .takeLast(if (imeVisible) 2 else 3)
                        .forEach { message ->
                        Surface(color = Color.Black.copy(alpha = .24f), shape = RoundedCornerShape(10.dp)) {
                            Text(message, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp)
                        }
                    }
                } else {
                    sellerNotice?.let { notice ->
                        Surface(
                            color = Colors.Navy.copy(alpha = .82f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            // 라벨과 본문의 세로 가운데를 맞춘다. 예전엔 위쪽 정렬이라 라벨이 글자보다 올라가 보였다
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "판매자 공지",
                                    Modifier.background(Color.White.copy(alpha = .22f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    notice.content,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // "전체보기" 시트 대신 이 자리에서 위로 올려 지난 댓글을 본다. 최신 댓글이 맨 아래에 붙도록 뒤집어 그린다
                    if (showChat) LiveCommentList(
                        comments = liveComments,
                        sellerMemberId = sellerMemberId,
                        currentMemberId = currentMemberId,
                        compact = imeVisible,
                        hasMore = chatHasMore,
                        loadingEarlier = chatLoadingEarlier,
                        loadEarlierError = chatLoadEarlierError,
                        onLoadEarlier = onLoadEarlierComments,
                        onMessageClick = { message ->
                            if (isAuthenticated) {
                                reportTarget = message
                                memberReportReason = "욕설·사기 유도"
                                showMemberReportReasons = false
                                reportContent = ""
                                onDismissReport()
                            } else onLoginRequired()
                        }
                    )
                }
        }
        AnimatedVisibility(!imeVisible, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 269.dp), enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 하트는 방송 좋아요가 아니라 "지금 경매 중인 상품 찜"이다. 라벨로 뜻을 밝힌다.
                // 내 방송의 내 상품은 찜할 이유가 없어 버튼을 뺀다. 편성 상품이 없을 때도 눌러서 이유를 알 수 있게
                // 버튼은 그대로 두고 안내만 띄운다 (예전엔 말없이 비활성이라 "어떤 라이브는 안 눌린다"로 보였다)
                if (!isOwnAuction) LiveFavoriteAction(favorite, updating = auctionKey != null && auctionKey in favoriteUpdatingAuctionIds) {
                    when {
                        !isAuthenticated -> onLoginRequired()
                        auctionKey == null -> favoriteHint = "찜할 경매 상품이 아직 없어요"
                        auctionKey in favoriteUpdatingAuctionIds -> Unit
                        else -> {
                            val selected = !favorite
                            favorite = selected
                            if (selected) showFavoriteBurst = true
                            favoriteHint = if (selected) "‘${activeAuction?.title ?: "현재 상품"}’을 찜했어요 · 마이 > 찜한 경매"
                            else "찜을 해제했어요"
                            onFavoriteChange(auctionKey, selected)
                        }
                    }
                }
                LiveAction(R.drawable.chat_outline, if (showChat) "댓글 끄기" else "댓글 켜기", label = if (showChat) "댓글" else "댓글 꺼짐", dimmed = !showChat) {
                    showChat = !showChat
                }
                LiveAction(R.drawable.report_outline, "신고", label = "신고") {
                    if (isAuthenticated) showReportTypes = true else onLoginRequired()
                }
            }
        }
        AnimatedVisibility(
            visible = showFavoriteBurst,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn() + scaleIn(initialScale = .45f),
            exit = fadeOut() + scaleOut(targetScale = 1.25f)
        ) {
            Image(
                painterResource(R.drawable.favorite_selected),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                colorFilter = ColorFilter.tint(Colors.Live)
            )
        }
        AnimatedVisibility(
            visible = favoriteHint != null && favoriteError == null,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 62.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(color = Color.Black.copy(alpha = .72f), shape = RoundedCornerShape(18.dp)) {
                Text(
                    favoriteHint.orEmpty(),
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
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
            Surface(
                color = cardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (hasActiveAuction) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val imageModifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { auctionKey?.let(onProductClick) }
                            if (isSampleContent) {
                                Image(painterResource(R.drawable.product_photo), activeAuction?.title ?: "달빛 유약 머그컵", imageModifier, contentScale = ContentScale.Crop)
                            } else {
                                DibNetworkImage(activeAuction?.imageUrls?.firstOrNull(), activeAuction?.title, imageModifier)
                            }
                            Column(Modifier.weight(1f).heightIn(min = 88.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    activeAuction?.title ?: "달빛 유약 머그컵",
                                    Modifier.clickable { auctionKey?.let(onProductClick) },
                                    color = Colors.Navy, fontSize = 15.sp, lineHeight = 19.sp,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold
                                )
                                AnimatedAuctionPrice(
                                    price = currentPrice,
                                    identity = auctionKey.orEmpty(),
                                    motionSequence = bidMotionSequence,
                                    tone = bidMotionTone,
                                    urgent = auctionUrgent,
                                    fontSize = 24.sp,
                                    lineHeight = 29.sp,
                                    baseColor = Colors.Navy
                                )
                                Text(
                                    "상품 전체보기 ›",
                                    Modifier.clickable { showProducts = true }.padding(vertical = 5.dp),
                                    color = Colors.Navy, fontSize = 11.sp, lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    } else {
                        Text("경매 준비 중", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("판매자가 경매를 시작하면 참여할 수 있어요", color = Colors.Muted, fontSize = 12.sp)
                    }
                    val bidLabel = when {
                        !hasActiveAuction -> "대기 중"
                        remaining <= 0 -> "종료"
                        isOwnAuction -> "내 경매"
                        isHighestBidder -> "최고가"
                        !realtimeBiddingEnabled -> "연결 필요"
                        bidSubmitting -> "요청 중"
                        else -> "입찰"
                    }
                    Button(
                        onClick = { if (isAuthenticated) showBidSheet = true else onLoginRequired() },
                        enabled = hasActiveAuction && remaining > 0 && !isOwnAuction && !isHighestBidder && !bidSubmitting && realtimeBiddingEnabled,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Colors.Navy,
                            disabledContainerColor = if (isHighestBidder) Colors.Surface else Colors.Navy,
                            disabledContentColor = if (isHighestBidder) Colors.Muted else Color.White
                        )
                    ) {
                        Text(
                            if (hasActiveAuction && remaining > 0) "$bidLabel: ${formatRemainingTime(remaining)}" else bidLabel,
                            fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (auctionUrgent) AuctionUrgencyProgress(remaining, Modifier.fillMaxWidth())
                }
            }
            }
            if (isAuthenticated && chatConnectionState != RealtimeConnectionState.Connected) Text(chatError ?: "Live 채팅 연결 중", color = Color.White.copy(.75f), fontSize = 9.sp)
            Row(Modifier.fillMaxWidth().height(46.dp).background(Color.Black.copy(.48f), RoundedCornerShape(23.dp)).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = comment, onValueChange = { comment = it.take(500) }, Modifier.weight(1f), enabled = isAuthenticated, singleLine = true, textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp), decorationBox = { inner -> if (comment.isBlank()) Text(if (isAuthenticated) "댓글을 입력하세요" else "로그인 후 댓글을 작성할 수 있어요", color = Color.White.copy(.75f), fontSize = 14.sp); inner() })
                Box(Modifier.size(34.dp).background(if (comment.isNotBlank()) Colors.Mint else Color.White, CircleShape).clickable { if (!isAuthenticated) onLoginRequired() else if (comment.isNotBlank() && onSendComment(comment)) comment = "" }, contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.send), "댓글 전송", Modifier.size(17.dp), colorFilter = ColorFilter.tint(Colors.Navy))
                }
            }
        }
        AnimatedVisibility(showBidFeedback, Modifier.align(Alignment.Center), enter = fadeIn() + scaleIn(initialScale = .7f), exit = fadeOut() + scaleOut(targetScale = .82f)) {
            Surface(color = Colors.UrgentBackground, shape = RoundedCornerShape(22.dp), shadowElevation = 10.dp) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("입찰을 확인해주세요", color = Colors.Urgent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(bidFeedbackMessage, color = Colors.Urgent, fontSize = 12.sp)
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
                if (auctionKey != null) activeAuction?.title ?: "현재 경매 상품" else "현재 진행 중인 경매가 없어요.",
                color = Colors.Muted,
                fontSize = 10.sp
            )
            LiveReportTypeAction(
                title = "상품 신고",
                // 내 방송의 내 상품은 신고 대상이 아니다 (서버도 SELF_REPORT_NOT_ALLOWED 로 막는다)
                description = if (isOwnAuction) "내 상품은 신고할 수 없어요" else "현재 경매 상품의 정보·설명을 신고",
                enabled = auctionKey != null && !isOwnAuction
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
                        Text(activeAuction?.title ?: "현재 경매 상품", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            // 댓글 전체보기는 없앴다. 이 시트는 신고 유형에서 "회원·채팅 신고"를 골랐을 때 대상 댓글을 고르는 용도다
            item { Text("신고할 댓글 선택", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
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
            // 공지·공지 해제 메시지는 판매자가 올린 규약 메시지라 신고할 댓글 목록에서 뺀다
            items(
                liveComments.distinctBy(LiveChatMessage::liveChattingId)
                    .filterNot { isLiveNoticeControl(it, sellerMemberId) }
                    .sortedBy(LiveChatMessage::time),
                key = LiveChatMessage::liveChattingId
            ) { message ->
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
                        val fromSeller = sellerMemberId != null && message.memberId == sellerMemberId
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (fromSeller) Text(
                                "판매자",
                                Modifier.background(Colors.Navy, RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                liveChatSpeakerLabel(message, currentMemberId, fromSeller),
                                color = Colors.Navy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(message.content, Modifier.padding(top = 3.dp), color = Colors.Text, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    // 라이브는 짧은 시간에 여러 번 입찰하므로 같은 경매에서는 자동 결제 동의를 한 번만 받는다.
    // 시트는 닫히면 사라지므로 동의한 경매 번호를 이 화면이 들고 있는다
    if (showBidSheet) LiveBidSheet(
        currentPrice,
        activeAuction?.bidCount ?: 0,
        autoPayAgreed = auctionKey != null && autoPayAgreedAuctionId == auctionKey,
        onAutoPayAgreedChange = { agreed -> autoPayAgreedAuctionId = if (agreed) auctionKey else null },
        onDismiss = { showBidSheet = false }
    ) { submission ->
        when {
            !realtimeBiddingEnabled -> {
                bidFeedbackMessage = "실시간 입찰 연결을 사용할 수 없어요."
                showBidFeedback = true
            }
            !isSampleContent && chatConnectionState != RealtimeConnectionState.Connected -> {
                bidFeedbackMessage = "실시간 연결 중이에요. 연결된 뒤 다시 시도해주세요."
                showBidFeedback = true
            }
            auctionKey != null && onRealtimeBid(auctionKey, submission.amount) -> {
                showBidSheet = false
                bidSubmitting = true
            }
            else -> {
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
                        Text(
                                "@" + liveChatSpeakerLabel(
                                    target,
                                    currentMemberId,
                                    sellerMemberId != null && target.memberId == sellerMemberId
                                ),
                                color = Colors.Navy,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
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

@Composable
private fun LiveVideoBackground(
    liveBroadcastId: String?,
    streamTokenProvider: (suspend (String) -> Result<LiveStreamSession>)?,
    fallbackImageUrl: String?,
    fallbackTitle: String,
    isActivePage: Boolean
) {
    val provider = liveBroadcastId?.let { id ->
        streamTokenProvider?.let { request -> suspend { request(id) } }
    }
    val session = rememberLiveVideoSession(
        role = LiveVideoRole.VIEWER,
        enabled = isActivePage && provider != null,
        tokenProvider = provider
    )

    when {
        session.videoTrack != null -> LiveVideoSurface(
            room = session.room,
            videoTrack = session.videoTrack,
            mirror = false,
            fill = true,
            modifier = Modifier.fillMaxSize()
        )
        !fallbackImageUrl.isNullOrBlank() -> DibNetworkImage(
            fallbackImageUrl,
            fallbackTitle,
            Modifier.fillMaxSize()
        )
        else -> Image(
            painterResource(if (liveBroadcastId == null) R.drawable.product_photo else R.drawable.live_video),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }

    if (!isActivePage || liveBroadcastId == null) return
    when (val state = session.state) {
        LiveVideoState.Preparing, LiveVideoState.Connecting -> LiveVideoConnectionNotice("방송 연결 중이에요")
        LiveVideoState.Reconnecting -> LiveVideoConnectionNotice("방송에 다시 연결 중이에요")
        LiveVideoState.Connected -> if (session.videoTrack == null) {
            LiveVideoConnectionNotice("판매자 영상을 기다리는 중이에요")
        }
        LiveVideoState.Disconnected -> LiveVideoConnectionNotice("영상 연결이 끊겼어요", "다시 연결", session::retry)
        is LiveVideoState.Failed -> LiveVideoConnectionNotice(state.message, "다시 연결", session::retry)
        LiveVideoState.Idle -> if (streamTokenProvider == null) {
            LiveVideoConnectionNotice("영상 연결 정보를 사용할 수 없어요")
        }
    }
}

@Composable
private fun LiveVideoConnectionNotice(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(horizontal = 48.dp),
            color = Color(0xFF101C2C).copy(alpha = .88f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(message, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) { Text(actionLabel, color = Colors.Mint) }
                }
            }
        }
    }
}

// 닉네임마다 고정된 색을 준다. 모두 흰 글씨라 누가 쓴 댓글인지 구분이 안 됐다.
// 어두운 반투명 배경 위에서 읽히는 밝은 색만 쓴다. 판매자는 민트, 나는 노랑으로 고정한다
private val liveCommentNameColors = listOf(
    Color(0xFF8FD3FF),
    Color(0xFFFFA8B6),
    Color(0xFFB9E68A),
    Color(0xFFCDB4FF),
    Color(0xFFFFC078),
    Color(0xFF7EE8E0),
    Color(0xFFFF9EE5)
)

private fun liveCommentNameColor(memberId: String, fromSeller: Boolean, mine: Boolean): Color = when {
    fromSeller -> Colors.Mint
    mine -> Color(0xFFFFE066)
    else -> liveCommentNameColors[Math.floorMod(memberId.hashCode(), liveCommentNameColors.size)]
}

@Composable
private fun LiveCommentList(
    comments: List<LiveChatMessage>,
    sellerMemberId: String?,
    currentMemberId: String?,
    compact: Boolean,
    hasMore: Boolean,
    loadingEarlier: Boolean,
    loadEarlierError: String?,
    onLoadEarlier: () -> Unit,
    onMessageClick: (LiveChatMessage) -> Unit
) {
    // 최신이 index 0. reverseLayout 이라 맨 아래에 붙고, 맨 아래를 보고 있으면 새 댓글을 그대로 따라간다
    val ordered = remember(comments, sellerMemberId) {
        comments.distinctBy(LiveChatMessage::liveChattingId)
            .filterNot { isLiveNoticeControl(it, sellerMemberId) }
            .sortedByDescending(LiveChatMessage::time)
    }
    val listState = rememberLazyListState()
    // Compose는 새 항목이 앞에 추가되면 기존 항목의 위치를 보존한다. 최신 댓글이 바뀐
    // 경우에만 맨 아래로 이동하고, 과거 댓글 페이지를 불러올 때는 위치를 유지한다.
    val newestCommentId = ordered.firstOrNull()?.liveChattingId
    LaunchedEffect(newestCommentId) {
        if (newestCommentId != null) listState.animateScrollToItem(0)
    }
    // 맨 위(가장 오래된 댓글)까지 올리면 이전 댓글을 이어서 불러온다. 사용자가 직접 올려 본 경우에만 부른다
    val reachedOldest by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            listState.canScrollBackward && last >= listState.layoutInfo.totalItemsCount - 1
        }
    }
    LaunchedEffect(reachedOldest, hasMore, loadingEarlier, loadEarlierError) {
        if (reachedOldest && hasMore && !loadingEarlier && loadEarlierError == null) onLoadEarlier()
    }
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = if (compact) 96.dp else 190.dp),
        state = listState,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(ordered, key = LiveChatMessage::liveChattingId) { message ->
            val fromSeller = sellerMemberId != null && message.memberId == sellerMemberId
            val mine = currentMemberId != null && message.memberId == currentMemberId
            Surface(
                color = if (fromSeller) Colors.Navy.copy(alpha = .62f) else Color.Black.copy(alpha = .28f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable(enabled = !mine) { onMessageClick(message) }
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = liveCommentNameColor(message.memberId, fromSeller, mine), fontWeight = FontWeight.Bold)) {
                            if (fromSeller) append("[판매자] ")
                            append(liveChatSpeakerLabel(message, currentMemberId, fromSeller = false))
                        }
                        append("  ")
                        append(message.content)
                    },
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        if (loadingEarlier || loadEarlierError != null) item(key = "earlier") {
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (loadingEarlier) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(
                    "이전 댓글을 불러오지 못했어요 · 다시 시도",
                    Modifier.clickable(onClick = onLoadEarlier).padding(6.dp),
                    color = Color.White.copy(alpha = .8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun LiveReportTypeAction(title: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFF8F9FB),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).graphicsLayer(alpha = if (enabled) 1f else .5f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Colors.Muted, fontSize = 10.sp)
        }
    }
}

// 오른쪽 세로 버튼 아래 작은 글자. 아이콘만으로는 하트가 무엇을 찜하는지 알기 어려웠다
@Composable
private fun LiveActionLabel(text: String) {
    Text(text, Modifier.padding(top = 3.dp), color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun LiveAction(@DrawableRes icon: Int, description: String, label: String, dimmed: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).background(Color.Black.copy(alpha = .32f), CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(icon),
                contentDescription = description,
                modifier = Modifier.size(26.dp).graphicsLayer(alpha = if (dimmed) .45f else 1f),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        LiveActionLabel(label)
    }
}

@Composable
private fun LiveFavoriteAction(selected: Boolean, updating: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).graphicsLayer(alpha = if (updating) .6f else 1f)
                .background(Color.Black.copy(alpha = .32f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(if (selected) R.drawable.favorite_selected else R.drawable.favorite_outline),
                contentDescription = if (selected) "현재 상품 찜 해제" else "현재 상품 찜하기",
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(if (selected) Colors.Live else Color.White)
            )
        }
        LiveActionLabel("상품 찜")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LiveBidSheet(currentPrice: Int, bidCount: Int, autoPayAgreed: Boolean, onAutoPayAgreedChange: (Boolean) -> Unit, onDismiss: () -> Unit, onConfirm: (BidSubmission) -> Unit) {
    // 상품 상세와 같은 규칙(서버 Auction.minNextBid 구간 + 10원 단위). 예전 currentPrice+1 은 서버가 전부 거절하는 금액이었다
    val minimum = minimumBidAmount(currentPrice, bidCount)
    var amountText by rememberSaveable(currentPrice) { mutableStateOf(minimum.toString()) }
    val typedAmount = amountText.toIntOrNull() ?: 0
    val amount = roundUpToBidUnit(typedAmount)
    val snapped = typedAmount > 0 && amount != typedAmount
    val valid = typedAmount > 0 && isValidBidAmount(amount, minimum)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("라이브 입찰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("현재가 ${"%,d".format(currentPrice)}원 · ${"%,d".format(minimum)}원 이상", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth(), suffix = { Text("원") }, isError = amountText.isNotBlank() && !valid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1_000, 5_000, 10_000).forEach { increment ->
                    Button(
                        onClick = { amountText = steppedBidAmount(amountText.toIntOrNull() ?: minimum, increment, minimum).toString() },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Surface, contentColor = Colors.Navy),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+%,d원".format(increment), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
            if (snapped) Text("10원 단위로 올려 ${"%,d".format(amount)}원으로 입찰돼요", color = Colors.MintInk, fontSize = 11.sp)
            Text(BID_NOTICES.joinToString("\n") { "• $it" }, color = Colors.Muted, fontSize = 11.sp, lineHeight = 17.sp)
            AutoPayConsentRow(amount, autoPayAgreed, onAutoPayAgreedChange)
            Button({ onConfirm(BidSubmission(amount)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid && autoPayAgreed, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("${"%,d".format(amount)}원 입찰하기", fontWeight = FontWeight.Bold) }
        }
    }
}
