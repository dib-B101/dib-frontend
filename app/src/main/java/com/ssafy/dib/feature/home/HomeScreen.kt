package com.ssafy.dib.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibSeeAllButton
import com.ssafy.dib.core.ui.DibSnackbarHost
import com.ssafy.dib.feature.auction.AuctionBidSheet
import com.ssafy.dib.core.ui.DibNotificationBell
import com.ssafy.dib.core.ui.DibSearchBar
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.AuctionUrgencyBadge
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.domain.auction.RecommendedLive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Figma 01_Wireframe / Full Scroll Views / 01_Home_Full (53:50). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isAuthenticated: Boolean,
    remoteAuctions: List<HomeAuction>?,
    remoteLives: List<RecommendedLive>?,
    showSampleContent: Boolean,
    remoteLoading: Boolean,
    remoteError: String?,
    onRetry: () -> Unit,
    onBookmarkChange: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    onLiveClick: () -> Unit,
    onSearchClick: () -> Unit,
    onViewAllAuctions: () -> Unit,
    onCategoryClick: () -> Unit,
    // 마감 임박 카드에서 바로 입찰. 결과 메시지는 bidNotice 로 돌아오고 토스트로 보여준 뒤 onBidNoticeShown 으로 비운다
    onPlaceBid: (auctionId: String, amount: Int) -> Unit,
    bidNotice: String?,
    onBidNoticeShown: () -> Unit,
    onLoginRequired: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    /**
     * 홈 탭을 다시 눌렀다는 신호. 값이 바뀔 때마다 목록을 맨 위로 올린다 (QA #21 · #23).
     *
     * 화면이 스스로 알 수 없는 사건이라 바깥에서 받는다 — 하단 내비는 탭 클릭을
     * AppNavHost 로 올려보내고, 그쪽이 "같은 탭을 다시 눌렀는지" 를 판단한다.
     */
    tabReselectSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var favoriteIds by rememberSaveable { mutableStateOf(listOf("sneakers")) }
    var closingSoon by rememberSaveable { mutableStateOf(false) }
    var contentView by rememberSaveable { mutableStateOf(DibContentView.Grid) }
    var bidTarget by remember { mutableStateOf<HomeAuction?>(null) }
    val toastHost = remember { SnackbarHostState() }
    LaunchedEffect(bidNotice) {
        val notice = bidNotice ?: return@LaunchedEffect
        toastHost.showSnackbar(notice)
        onBidNoticeShown()
    }
    val displayedAuctions = remoteAuctions ?: if (showSampleContent) allAuctions else emptyList()
    val homeRecommendations = displayedAuctions.take(4)
    val displayedLives = remoteLives ?: if (showSampleContent) null else emptyList()
    val closingAuctions = remoteAuctions?.filter { it.status.equals("ACTIVE", ignoreCase = true) && it.remainingSeconds > 0 }
        ?.distinctBy(HomeAuction::id)?.sortedBy(HomeAuction::remainingSeconds)?.take(5)
        ?: if (showSampleContent) listOf(deadlineAuction, recommended[0], allAuctions[0], allAuctions[1]) else emptyList()
    val highlightedDeadline = closingAuctions.firstOrNull()
    val followingDeadlines = closingAuctions.drop(1)
    var deadlineSeconds by rememberSaveable(highlightedDeadline?.id) {
        mutableIntStateOf(highlightedDeadline?.remainingSeconds ?: 0)
    }

    LaunchedEffect(remoteAuctions) {
        remoteAuctions?.let { auctions ->
            favoriteIds = auctions.filter(HomeAuction::bookmarked).map(HomeAuction::id)
        }
    }

    // 첫 조합에서는 0 이라 움직이지 않는다. 탭을 다시 누른 순간부터만 올린다.
    LaunchedEffect(tabReselectSignal) {
        if (tabReselectSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(highlightedDeadline?.id, highlightedDeadline?.remainingSeconds) {
        val deadline = highlightedDeadline ?: return@LaunchedEffect
        deadlineSeconds = deadline.remainingSeconds
        while (deadlineSeconds > 0) {
            delay(1_000)
            deadlineSeconds--
        }
        delay(1_500)
        onRetry()
    }

    fun updateFavorite(id: String, selected: Boolean) {
        if (isAuthenticated) {
            favoriteIds = if (selected) (favoriteIds + id).distinct() else favoriteIds - id
            onBookmarkChange(id, selected)
        } else {
            onLoginRequired()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentColor = Colors.Text,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HomeHeader()
        },
        snackbarHost = { DibSnackbarHost(toastHost) },
        bottomBar = {
            DibBottomNavigation(
                selectedTab = DibMainTab.Home,
                onTabSelected = { tab ->
                    onTabSelected(tab)
                }
            )
        }
    ) { padding ->
        DibPullToRefreshBox(isRefreshing = remoteLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { DibSearchBar("어떤 상품을 찾고 있나요?", onSearchClick) }

            item {
                HomeAuctionSwitcher(closingSoon, { closingSoon = false }, { closingSoon = true }, onCategoryClick)
            }
            if (remoteLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Mint) }
            remoteError?.let { message ->
                item {
                    Row(
                        Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                        Text("다시 시도", Modifier.clickable(onClick = onRetry).padding(6.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (displayedAuctions.isEmpty() && displayedLives?.isNotEmpty() != true && !remoteLoading && remoteError == null) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("아직 진행 중인 경매가 없어요", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("상품이 등록되면 이곳에서 바로 확인할 수 있어요", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            } else if (closingSoon && highlightedDeadline != null && remoteError == null) {
                item {
                    DeadlineSection(
                        auction = highlightedDeadline,
                        deadlineSeconds = deadlineSeconds,
                        favorite = highlightedDeadline.id in favoriteIds,
                        onFavorite = { updateFavorite(highlightedDeadline.id, it) },
                        onProductClick = { onProductClick(highlightedDeadline.id) },
                        // 상세로 가지 않고 바로 입찰 시트를 띄운다. 로그인 전이면 로그인으로
                        onBidClick = { if (isAuthenticated) bidTarget = highlightedDeadline else onLoginRequired() },
                        onViewAllClick = onViewAllAuctions
                    )
                }
                if (followingDeadlines.isNotEmpty()) {
                    item {
                        AuctionGridSection(
                            title = "이어서 마감돼요",
                            auctions = followingDeadlines,
                            favoriteIds = favoriteIds,
                            viewMode = contentView,
                            onViewModeChange = { contentView = it },
                            onFavorite = ::updateFavorite,
                            onProductClick = onProductClick,
                            actionLabel = "전체 경매",
                            onAction = onViewAllAuctions
                        )
                    }
                }
            } else if (closingSoon && remoteError == null) {
                item {
                    Text(
                        "곧 마감되는 경매가 없어요",
                        Modifier.fillMaxWidth().padding(vertical = 72.dp),
                        color = Colors.Muted,
                        fontSize = 14.sp
                    )
                }
            } else if (remoteError == null) {
                item { HomeLiveSection(displayedLives, onLiveClick) }
                item {
                    AuctionGridSection(
                        title = "추천 경매",
                        auctions = homeRecommendations,
                        favoriteIds = favoriteIds,
                        viewMode = contentView,
                        onViewModeChange = { contentView = it },
                        onFavorite = ::updateFavorite,
                        onProductClick = onProductClick,
                        actionLabel = "전체 경매",
                        onAction = onViewAllAuctions
                    )
                }
            }
        }
        }
    }
    bidTarget?.let { target ->
        AuctionBidSheet(
            productName = target.name,
            currentPrice = target.price,
            bidCount = target.bidCount,
            submissionError = "",
            onDismiss = { bidTarget = null },
            onContinue = { submission ->
                bidTarget = null
                onPlaceBid(target.id, submission.amount)
            }
        )
    }
}

@Composable
private fun HomeAuctionSwitcher(selectedClosing: Boolean, onGeneral: () -> Unit, onClosing: () -> Unit, onCategory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(Colors.Surface, RoundedCornerShape(15.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
            listOf("추천" to false, "마감 임박" to true).forEach { (label, value) ->
                val selected = selectedClosing == value
                Surface(
                    onClick = if(value) onClosing else onGeneral,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = if(selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(11.dp),
                    shadowElevation = if(selected) 1.dp else 0.dp
                ) { Box(contentAlignment = Alignment.Center) { Text(label, color = if(selected) Colors.Navy else Colors.Muted, fontSize = 13.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium) } }
            }
        Surface(
            onClick = onCategory,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            color = Color.Transparent,
            shape = RoundedCornerShape(11.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("카테고리  ›", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeLiveSection(remoteLives: List<RecommendedLive>?, onLiveClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("LIVE", "전체 보기", onLiveClick)
        val cards = remoteLives?.take(2)?.map { item ->
            val isLive = item.status.equals("LIVE", ignoreCase = true)
            HomeLiveCard(
                title = item.title,
                description = item.description ?: "Live 상품을 확인해보세요",
                isLive = isLive,
                footer = if (isLive) "방송 중 · 눌러서 보기" else homeLiveScheduleLabel(item.scheduledAt),
                photo = null
            )
        } ?: listOf(
            HomeLiveCard("오디오마켓 라이브", "노이즈 캔슬링 헤드폰", true, "상품 5개 · 눌러서 보기", ProductPhoto.Headphones),
            HomeLiveCard("빈티지마켓 라이브", "빈티지 필름 카메라", false, "상품 3개 · 오늘 20:00", ProductPhoto.Camera)
        )
        if (cards.isEmpty()) {
            Text("현재 방송 중인 Live가 없어요.", Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(18.dp), color = Colors.Muted, fontSize = 12.sp)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.forEach { card ->
                Surface(
                    onClick = onLiveClick,
                    modifier = Modifier.weight(1f),
                    color = Colors.Background,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Colors.Border),
                    shadowElevation = 0.dp
                ) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // 가로로 길던 썸네일을 정방형에 가깝게 — 카드 두 장이 나란히 있을 때 세로가 너무 눌려 보였다
                    Box(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(12.dp)).background(if (card.isLive) Color(0xFF18253A) else Colors.NavySoft)) {
                        card.photo?.let { ProductPhoto(it, modifier = Modifier.matchParentSize()) } ?: Image(
                            painter = painterResource(R.drawable.live_video),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(38.dp),
                            colorFilter = ColorFilter.tint(if (card.isLive) Colors.Mint else Colors.Navy.copy(alpha = .55f))
                        )
                        Surface(Modifier.padding(7.dp), color = if(card.isLive) Colors.Live else Colors.Navy, shape = RoundedCornerShape(9.dp)) {
                            Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!card.isLive) Image(painterResource(R.drawable.ic_schedule), null, Modifier.size(12.dp))
                                Text(if(card.isLive) "● LIVE" else "예정", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(card.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                    Text(card.description, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                    Text(card.footer, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Colors.MintInk, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class HomeLiveCard(
    val title: String,
    val description: String,
    val isLive: Boolean,
    val footer: String,
    val photo: ProductPhoto?
)

internal fun homeLiveScheduleLabel(value: String?, zoneId: ZoneId = ZoneId.systemDefault()): String =
    value?.let { scheduledAt ->
        runCatching {
            Instant.parse(scheduledAt).atZone(zoneId).format(DateTimeFormatter.ofPattern("M월 d일 HH:mm 예정"))
        }.getOrNull()
    } ?: "방송 예정"

@Composable
private fun HomeHeader() {
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFFBF9F4)).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.dib_official_logo),
            "dib",
            Modifier.size(70.dp, 44.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
        // 알림 벨은 모든 헤더가 같은 것을 쓴다 (미읽음 개수·클릭 처리는 AppNavHost 가 제공)
        DibNotificationBell(tint = Colors.Navy.copy(alpha = .72f))
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.semantics { heading() }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        DibSeeAllButton(onClick, label = action)
    }
}

@Composable
private fun AuctionGridSection(
    title: String,
    auctions: List<HomeAuction>,
    favoriteIds: List<String>,
    viewMode: DibContentView,
    onViewModeChange: (DibContentView) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.semantics { heading() }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            DibViewModeToggle(viewMode, onViewModeChange)
            actionLabel?.let { label -> DibSeeAllButton(onAction, label = label) }
        }
        if (viewMode == DibContentView.Grid) {
            auctions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { auction ->
                        HomeAuctionCard(
                            auction = auction,
                            favorite = auction.id in favoriteIds,
                            onFavorite = { onFavorite(auction.id, it) },
                            onClick = { onProductClick(auction.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            auctions.forEach { auction ->
                HomeAuctionListCard(
                    auction = auction,
                    favorite = auction.id in favoriteIds,
                    onFavorite = { onFavorite(auction.id, it) },
                    onClick = { onProductClick(auction.id) }
                )
            }
        }
    }
}


@Composable
private fun DeadlineSection(
    auction: HomeAuction,
    deadlineSeconds: Int,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onProductClick: () -> Unit,
    onBidClick: () -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("마감 임박 경매", "전체 경매", onViewAllClick)
        Row(
            Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(18.dp))
                .border(1.dp, Colors.Border, RoundedCornerShape(18.dp))
                .clickable(onClick = onProductClick).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(144.dp).background(Colors.Image, RoundedCornerShape(12.dp))) {
                ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
                DibWishlistButton(favorite, onFavorite, auction.name, Modifier.align(Alignment.TopEnd))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 남은 시간은 가격 밑 회색 글자 대신 배지 하나로 보여준다 (일·시·분)
                DeadlineBadge(deadlineSeconds)
                Text(auction.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                Text(auction.priceText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBidClick,
                        enabled = deadlineSeconds > 0,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (deadlineSeconds <= 15) Colors.Live else Colors.Navy),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (deadlineSeconds <= 0) "경매 종료" else "입찰하기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onProductClick,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Colors.Navy),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("상품 보기", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** "마감 임박 · n일 m시간 k분" 배지. 마지막 1분만 초를 보여준다. */
@Composable
private fun DeadlineBadge(seconds: Int) {
    Surface(color = Colors.UrgentBackground, shape = RoundedCornerShape(9.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.drawable.timer_outline), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Colors.Urgent))
            Text(if (seconds <= 0) "마감" else "마감 임박 · ${deadlineCountdownLabel(seconds)}", color = Colors.Urgent, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun deadlineCountdownLabel(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val days = safe / 86_400
    val hours = safe % 86_400 / 3_600
    val minutes = safe % 3_600 / 60
    val remainder = safe % 60
    return when {
        days > 0 -> "${days}일 ${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간 ${minutes}분"
        minutes > 0 -> "${minutes}분 ${remainder}초"
        else -> "${remainder}초"
    }
}

@Composable
private fun PopularSection(auctions: List<HomeAuction>, onProductClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("인기 경매", "전체보기")
        auctions.forEachIndexed { index, auction ->
            Row(
                Modifier.fillMaxWidth().height(60.dp).clickable { onProductClick(auction.id) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(color = Colors.Surface, shape = RoundedCornerShape(13.dp), modifier = Modifier.size(26.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.size(54.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(auction.name, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                    Text(auction.priceText, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                    Text(homeAuctionMeta(auction), color = Colors.Muted, fontSize = 9.sp, lineHeight = 11.sp)
                }
            }
        }
    }
}

@Composable
internal fun ProductPhoto(photo: ProductPhoto, imageUrl: String? = null, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(10.dp))) {
        if (!imageUrl.isNullOrBlank()) {
            DibNetworkImage(imageUrl, null, Modifier.matchParentSize())
            return@BoxWithConstraints
        }
        if (photo == ProductPhoto.Placeholder) {
            Box(Modifier.matchParentSize().background(Colors.Image), contentAlignment = Alignment.Center) {
                Text("상품 이미지", color = Colors.Muted, fontSize = 12.sp)
            }
            return@BoxWithConstraints
        }
        val scale = maxOf(1f, maxWidth.value / maxHeight.value)
        Image(
            painter = painterResource(R.drawable.product_photo),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                transformOrigin = if (photo == ProductPhoto.Camera) TransformOrigin(0f, .5f) else TransformOrigin(1f, .5f)
            ),
            contentScale = ContentScale.FillHeight,
            alignment = if (photo == ProductPhoto.Camera) Alignment.CenterStart else Alignment.CenterEnd
        )
    }
}

internal fun formatClock(seconds: Int): String = "%02d:%02d".format(
    seconds.coerceAtLeast(0) / 60,
    seconds.coerceAtLeast(0) % 60
)
