package com.ssafy.dib.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

/** Figma 01_Wireframe / Full Scroll Views / 01_Home_Full (53:50). */
@Composable
fun HomeScreen(
    isAuthenticated: Boolean,
    remoteAuctions: List<HomeAuction>?,
    remoteLoading: Boolean,
    remoteError: String?,
    onRetry: () -> Unit,
    onProductClick: (String) -> Unit,
    onLiveClick: () -> Unit,
    onSearchClick: () -> Unit,
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
    val displayedAuctions = remoteAuctions ?: allAuctions
    val closingAuctions = remoteAuctions?.sortedBy(HomeAuction::remainingSeconds)?.take(4) ?: recommended
    val highlightedDeadline = closingAuctions.firstOrNull() ?: deadlineAuction

    LaunchedEffect(highlightedDeadline.id) {
        deadlineSeconds = highlightedDeadline.remainingSeconds
        while (deadlineSeconds > 0) {
            delay(1_000)
            deadlineSeconds--
        }
    }

    fun updateFavorite(id: String, selected: Boolean) {
        if (isAuthenticated) {
            favoriteIds = if (selected) (favoriteIds + id).distinct() else favoriteIds - id
        } else {
            onLoginRequired()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentColor = Colors.Text,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HomeHeader(onNotificationsClick)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
            if (remoteAuctions?.isEmpty() == true && !remoteLoading) {
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
            } else if (closingSoon) {
                item {
                    DeadlineSection(
                        auction = highlightedDeadline,
                        deadlineSeconds = deadlineSeconds,
                        favorite = highlightedDeadline.id in favoriteIds,
                        onFavorite = { updateFavorite(highlightedDeadline.id, it) },
                        onProductClick = { onProductClick(highlightedDeadline.id) },
                        onFeedClick = onLiveClick
                    )
                }
                item { AuctionGridSection("곧 마감되는 경매", closingAuctions, 100, favoriteIds, contentView, { contentView = it }, ::updateFavorite, onProductClick) }
            } else {
                item { HomeLiveSection(onLiveClick) }
                item { AuctionGridSection("전체 경매", displayedAuctions, 100, favoriteIds, contentView, { contentView = it }, ::updateFavorite, onProductClick) }
            }
        }
    }
}

@Composable
private fun HomeAuctionSwitcher(selectedClosing: Boolean, onGeneral: () -> Unit, onClosing: () -> Unit, onCategory: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF6F7F9), RoundedCornerShape(20.dp)).padding(4.dp)) {
            listOf("일반" to false, "마감임박" to true).forEach { (label, value) ->
                val selected = selectedClosing == value
                Surface(
                    onClick = if(value) onClosing else onGeneral,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = if(selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = if(selected) 2.dp else 0.dp
                ) { Box(contentAlignment = Alignment.Center) { Text(label, color = if(selected) Colors.Navy else Colors.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
            }
        }
        OutlinedButton(onClick = onCategory, Modifier.width(64.dp).fillMaxHeight(), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(0.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Colors.Border)) { Text("카테고리", color = Colors.Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HomeLiveSection(onLiveClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("지금 LIVE", "라이브 보기", onLiveClick)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("하루공방 라이브", "달빛 유약 머그컵", true),
                Triple("빈티지마켓 라이브", "빈티지 필름 카메라", false)
            ).forEach { (title, product, live) ->
                Column(Modifier.weight(1f).height(176.dp).clickable(onClick = onLiveClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.fillMaxWidth().height(100.dp).background(Colors.Image, RoundedCornerShape(10.dp))) {
                        Surface(Modifier.padding(8.dp), color = if(live) Colors.Live else Colors.Navy, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (!live) Image(painterResource(R.drawable.ic_schedule), null, Modifier.size(13.dp))
                                Text(if(live) "● LIVE" else "예정", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Image(painterResource(R.drawable.favorite_outline), "라이브 찜하기", Modifier.align(Alignment.TopEnd).padding(10.dp).size(22.dp))
                    }
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(product, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if(live) "상품 5개 · 목록 보기" else "상품 3개 · 오늘 20:00", color = Colors.Muted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onNotificationsClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(Colors.Background).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.dib_official_logo), "dib", Modifier.size(42.dp, 27.dp))
        Text(
            "알림",
            modifier = Modifier.clickable(onClick = onNotificationsClick).padding(vertical = 12.dp),
            color = Colors.Navy.copy(alpha = .72f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(Colors.Search, RoundedCornerShape(12.dp))
            .border(1.dp, Colors.SearchBorder, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painterResource(R.drawable.search_full), null, Modifier.size(19.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
        Text("상품을 검색해보세요", color = Colors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.semantics { heading() }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            action,
            Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
            color = Colors.Navy.copy(alpha = .76f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AuctionGridSection(
    title: String,
    auctions: List<HomeAuction>,
    imageHeight: Int,
    favoriteIds: List<String>,
    viewMode: DibContentView,
    onViewModeChange: (DibContentView) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, Modifier.semantics { heading() }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            DibViewModeToggle(viewMode, onViewModeChange)
        }
        if (viewMode == DibContentView.Grid) {
            auctions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { auction ->
                        AuctionCard(
                            auction = auction,
                            imageHeight = imageHeight,
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
        Modifier.fillMaxWidth().height(112.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.size(92.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(auction.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${auction.pricePrefix} ${auction.priceLabel}", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(auction.meta, color = Colors.Muted, fontSize = 10.sp)
        }
        DibWishlistButton(favorite, onFavorite, auction.name)
    }
}

@Composable
private fun AuctionCard(
    auction: HomeAuction,
    imageHeight: Int,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier.fillMaxWidth().height(imageHeight.dp)
                .background(Colors.Image, RoundedCornerShape(10.dp))
        ) {
            ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
            DibWishlistButton(
                selected = favorite,
                onSelectedChange = onFavorite,
                productName = auction.name,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(auction.name, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
            Text("${auction.pricePrefix} ${auction.priceLabel}", fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
            Text(auction.meta, color = Colors.Muted, fontSize = 9.sp, lineHeight = 11.sp)
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
    onFeedClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("마감 임박", "피드 보기", onFeedClick)
        Row(
            Modifier.fillMaxWidth().height(146.dp).background(Colors.Surface, RoundedCornerShape(14.dp))
                .clickable(onClick = onProductClick).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.width(132.dp).fillMaxHeight().background(Colors.Image, RoundedCornerShape(10.dp))) {
                ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.matchParentSize())
                DibWishlistButton(favorite, onFavorite, auction.name, Modifier.align(Alignment.TopEnd))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    modifier = Modifier.height(24.dp),
                    color = Colors.UrgentBackground,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "♧ ${remainingTimeLabel(deadlineSeconds)} 남음",
                        Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        color = Colors.Urgent,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(auction.name, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                Text("${auction.pricePrefix} ${auction.priceLabel}", fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                Text("입찰 ${auction.bidCount}명", color = Colors.Muted, fontSize = 10.sp, lineHeight = 12.sp)
                Button(
                    onClick = onProductClick,
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("바로 입찰하기", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Text("${auction.pricePrefix} ${auction.priceLabel}", fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
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
