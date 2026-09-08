package com.ssafy.dib.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Figma 01_Wireframe / Full Scroll Views / 01_Home_Full (53:50). */
@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoriteIds by rememberSaveable { mutableStateOf(listOf("sneakers")) }
    var deadlineSeconds by rememberSaveable { mutableIntStateOf(204) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (deadlineSeconds > 0) {
            delay(1_000)
            deadlineSeconds--
        }
    }

    fun updateFavorite(id: String, selected: Boolean) {
        favoriteIds = if (selected) (favoriteIds + id).distinct() else favoriteIds - id
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentColor = Colors.Text,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HomeHeader {
                scope.launch { snackbar.showSnackbar("알림 화면은 다음 단계에서 연결해요") }
            }
        },
        bottomBar = {
            DibBottomNavigation(
                selectedTab = DibMainTab.Home,
                onTabSelected = { tab ->
                    if (tab == DibMainTab.Home) onTabSelected(tab)
                    else scope.launch { snackbar.showSnackbar("${tab.label} 화면은 다음 단계에서 연결해요") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { SearchField(query = query, onQueryChange = { query = it }) }

            if (query.isBlank()) {
                item {
                    AuctionGridSection(
                        title = "추천 경매",
                        action = "전체보기",
                        auctions = recommended,
                        imageHeight = 104,
                        favoriteIds = favoriteIds,
                        onFavorite = ::updateFavorite,
                        onProductClick = onProductClick
                    )
                }
                item {
                    DeadlineSection(
                        deadlineSeconds = deadlineSeconds,
                        favorite = "camera" in favoriteIds,
                        onFavorite = { updateFavorite("camera", it) },
                        onProductClick = { onProductClick("camera") },
                        onFeedClick = {
                            scope.launch { snackbar.showSnackbar("피드 화면은 다음 단계에서 연결해요") }
                        }
                    )
                }
                item {
                    PopularSection(
                        auctions = popularAuctions,
                        onProductClick = onProductClick
                    )
                }
                item {
                    AuctionGridSection(
                        title = "전체 경매",
                        action = "둘러보기",
                        auctions = allAuctions,
                        imageHeight = 100,
                        favoriteIds = favoriteIds,
                        onFavorite = ::updateFavorite,
                        onProductClick = onProductClick
                    )
                }
            } else {
                val results = allHomeAuctions.distinctBy(HomeAuction::id).filter {
                    it.name.contains(query.trim(), ignoreCase = true)
                }
                item {
                    if (results.isEmpty()) {
                        EmptySearchResult { query = "" }
                    } else {
                        SearchResults(
                            auctions = results,
                            favoriteIds = favoriteIds,
                            onFavorite = ::updateFavorite,
                            onProductClick = onProductClick
                        )
                    }
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(Colors.Search, RoundedCornerShape(12.dp))
            .border(1.dp, Colors.SearchBorder, RoundedCornerShape(12.dp)),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Colors.Text, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painterResource(R.drawable.search_full),
                    null,
                    Modifier.size(19.dp),
                    colorFilter = ColorFilter.tint(Colors.MintInk)
                )
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("상품을 검색해보세요", color = Colors.Muted, fontSize = 13.sp)
                    innerTextField()
                }
            }
        }
    )
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
    action: String,
    auctions: List<HomeAuction>,
    imageHeight: Int,
    favoriteIds: List<String>,
    onFavorite: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, action)
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
                DibWishlistButton(favorite, onFavorite, "빈티지 필름 카메라", Modifier.align(Alignment.TopEnd))
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
                Text("빈티지 필름 카메라", fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                Text("현재가 34,500원", fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                Text("입찰 5명 · 다음 입찰 35,000원", color = Colors.Muted, fontSize = 10.sp, lineHeight = 12.sp)
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
                Box(Modifier.size(54.dp).background(Colors.Image, RoundedCornerShape(9.dp)))
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
private fun SearchResults(
    auctions: List<HomeAuction>,
    favoriteIds: List<String>,
    onFavorite: (String, Boolean) -> Unit,
    onProductClick: (String) -> Unit
) {
    AuctionGridSection(
        title = "검색 결과 ${auctions.size}개",
        action = "",
        auctions = auctions,
        imageHeight = 100,
        favoriteIds = favoriteIds,
        onFavorite = onFavorite,
        onProductClick = onProductClick
    )
}

@Composable
private fun EmptySearchResult(onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("조건에 맞는 경매가 없어요", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = onReset) { Text("검색 초기화", color = Colors.Navy) }
    }
}

@Composable
internal fun ProductPhoto(photo: ProductPhoto, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(10.dp))) {
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
