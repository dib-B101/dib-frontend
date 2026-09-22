package com.ssafy.dib.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
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
@Composable
fun HomeScreen(
    isAuthenticated: Boolean,
    remoteAuctions: List<HomeAuction>?,
    remoteLives: List<RecommendedLive>?,
    showSampleContent: Boolean,
    remoteLoading: Boolean,
    remoteError: String?,
    unreadNotificationCount: Int,
    onRetry: () -> Unit,
    onBookmarkChange: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    onLiveClick: () -> Unit,
    onSearchClick: () -> Unit,
    onViewAllAuctions: () -> Unit,
    onNotificationsClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onLoginRequired: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var favoriteIds by rememberSaveable { mutableStateOf(listOf("sneakers")) }
    var deadlineSeconds by rememberSaveable { mutableIntStateOf(204) }
    var closingSoon by rememberSaveable { mutableStateOf(false) }
    var contentView by rememberSaveable { mutableStateOf(DibContentView.Grid) }
    val displayedAuctions = remoteAuctions ?: if (showSampleContent) allAuctions else emptyList()
    val homeRecommendations = displayedAuctions.take(4)
    val displayedLives = remoteLives ?: if (showSampleContent) null else emptyList()
    val closingAuctions = remoteAuctions?.distinctBy(HomeAuction::id)?.sortedBy(HomeAuction::remainingSeconds)?.take(5)
        ?: if (showSampleContent) listOf(deadlineAuction, recommended[0], allAuctions[0], allAuctions[1]) else emptyList()
    val highlightedDeadline = closingAuctions.firstOrNull()
    val followingDeadlines = closingAuctions.drop(1)

    LaunchedEffect(remoteAuctions) {
        remoteAuctions?.let { auctions ->
            favoriteIds = auctions.filter(HomeAuction::bookmarked).map(HomeAuction::id)
        }
    }

    LaunchedEffect(highlightedDeadline?.id) {
        val deadline = highlightedDeadline ?: return@LaunchedEffect
        deadlineSeconds = deadline.remainingSeconds
        while (deadlineSeconds > 0) {
            delay(1_000)
            deadlineSeconds--
        }
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
            HomeHeader(unreadNotificationCount, onNotificationsClick)
        },
        bottomBar = {
            DibBottomNavigation(
                selectedTab = DibMainTab.Home,
                onTabSelected = { tab ->
                    onTabSelected(tab)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { SearchField(onSearchClick) }

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
        SectionHeader("지금 LIVE", "모두 보기", onLiveClick)
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
                    modifier = Modifier.weight(1f).heightIn(min = 170.dp),
                    color = Colors.Background,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Colors.Border),
                    shadowElevation = 0.dp
                ) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(12.dp)).background(if (card.isLive) Color(0xFF18253A) else Colors.NavySoft)) {
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
private fun HomeHeader(unreadNotificationCount: Int, onNotificationsClick: () -> Unit) {
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
        Box(Modifier.size(44.dp).clickable(onClick = onNotificationsClick), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.notification),
                contentDescription = if (unreadNotificationCount > 0) "새 알림 ${unreadNotificationCount}개" else "알림",
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(Colors.Navy.copy(alpha = .72f))
            )
            if (unreadNotificationCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = 2.dp).heightIn(min = 17.dp)
                        .background(Colors.Live, RoundedCornerShape(9.dp)).padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .background(Colors.Search, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painterResource(R.drawable.search_full), null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
        Text("어떤 상품을 찾고 있나요?", color = Colors.Muted, fontSize = 14.sp)
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
        Row(
            Modifier.clickable(onClick = onClick).padding(start = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(action, color = Colors.Navy.copy(alpha = .76f), fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
            Image(painterResource(R.drawable.chevron_right), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(Colors.Navy.copy(alpha = .66f)))
        }
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
            actionLabel?.let { label ->
                Row(Modifier.clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Image(painterResource(R.drawable.chevron_right), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(Colors.Navy))
                }
            }
            DibViewModeToggle(viewMode, onViewModeChange)
        }
        if (viewMode == DibContentView.Grid) {
            auctions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { auction ->
                        AuctionCard(
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
                AuctionListCard(
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
private fun AuctionListCard(
    auction: HomeAuction,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(116.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.size(92.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(auction.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(auction.priceText, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(auction.meta, color = Colors.Muted, fontSize = 10.sp)
        }
        DibWishlistButton(favorite, onFavorite, auction.name)
    }
}

@Composable
private fun AuctionCard(
    auction: HomeAuction,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Colors.Background,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Colors.Border),
        shadowElevation = 0.dp
    ) {
    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.05f)
                .background(Colors.Image, RoundedCornerShape(13.dp))
        ) {
            ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
            DibWishlistButton(
                selected = favorite,
                onSelectedChange = onFavorite,
                productName = auction.name,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(auction.name, maxLines = 1, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
            Text(auction.priceText, color = Colors.Navy, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
            Text(auction.meta, maxLines = 1, color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
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
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("마감 임박 경매", "전체 경매", onViewAllClick)
        Row(
            Modifier.fillMaxWidth().height(168.dp).background(Colors.Background, RoundedCornerShape(18.dp))
                .border(1.dp, Colors.Border, RoundedCornerShape(18.dp))
                .clickable(onClick = onProductClick).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.width(144.dp).fillMaxHeight().background(Colors.Image, RoundedCornerShape(12.dp))) {
                ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
                DibWishlistButton(favorite, onFavorite, auction.name, Modifier.align(Alignment.TopEnd))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                AuctionUrgencyBadge(deadlineSeconds, compact = true)
                Text(auction.name, maxLines = 2, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
                Text(auction.priceText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                Text("입찰 ${auction.bidCount}회", color = Colors.Muted, fontSize = 10.sp, lineHeight = 12.sp)
                Button(
                    onClick = onProductClick,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (deadlineSeconds <= 15) Colors.Live else Colors.Navy),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (deadlineSeconds <= 15) "지금 입찰" else "바로 입찰하기", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
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
                    Text(auction.meta, color = Colors.Muted, fontSize = 9.sp, lineHeight = 11.sp)
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
