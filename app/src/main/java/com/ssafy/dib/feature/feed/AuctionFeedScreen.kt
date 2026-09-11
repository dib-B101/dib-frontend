package com.ssafy.dib.feature.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.feature.auction.BidParticipationFields
import com.ssafy.dib.feature.auction.BidSubmission
import com.ssafy.dib.feature.auction.sampleBidAddresses
import com.ssafy.dib.feature.auction.samplePaymentMethods
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FeedAuction(
    val id: String,
    val name: String,
    val price: Int,
    val bidCount: Int,
    val initialSeconds: Int,
    val pageCount: Int = 5
)

private val feedAuctions = listOf(
    FeedAuction("camera", "빈티지 필름 카메라", 34_500, 5, 204),
    FeedAuction("sneakers", "빈티지 스니커즈", 81_000, 4, 4_320, 4),
    FeedAuction("headphones", "무선 헤드폰", 52_000, 7, 25, 3)
)

/** Figma 01_Wireframe / 02_Auction_Card_Feed (9:3). */
@Composable
fun AuctionFeedScreen(
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedPagerState = rememberPagerState(pageCount = feedAuctions::size)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var favoriteIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    val remainingSeconds = remember {
        mutableStateListOf<Int>().also { list -> list.addAll(feedAuctions.map(FeedAuction::initialSeconds)) }
    }
    val currentPrices = remember {
        mutableStateListOf<Int>().also { list -> list.addAll(feedAuctions.map(FeedAuction::price)) }
    }
    val bidCounts = remember {
        mutableStateListOf<Int>().also { list -> list.addAll(feedAuctions.map(FeedAuction::bidCount)) }
    }
    var selectedBidPage by rememberSaveable { mutableIntStateOf(-1) }
    var bidSuccessPage by remember { mutableIntStateOf(-1) }
    var extendedPage by remember { mutableIntStateOf(-1) }
    var lastBidAmount by remember { mutableIntStateOf(0) }
    var showFirstEntryGuide by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            remainingSeconds.indices.forEach { index ->
                if (remainingSeconds[index] > 0) remainingSeconds[index]--
            }
        }
    }

    LaunchedEffect(bidSuccessPage) {
        if (bidSuccessPage >= 0) {
            delay(1_600)
            bidSuccessPage = -1
        }
    }

    LaunchedEffect(extendedPage) {
        if (extendedPage >= 0) {
            delay(1_600)
            extendedPage = -1
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        bottomBar = {
            DibBottomNavigation(
                selectedTab = DibMainTab.Feed,
                onTabSelected = { tab ->
                    if (tab == DibMainTab.Home || tab == DibMainTab.Feed) onTabSelected(tab)
                    else scope.launch { snackbar.showSnackbar("${tab.label} 화면은 다음 단계에서 연결해요") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            VerticalPager(
                state = feedPagerState,
                modifier = Modifier.fillMaxSize(),
                key = { feedAuctions[it].id }
            ) { page ->
                val auction = feedAuctions[page]
                FeedPage(
                    auction = auction,
                    currentPrice = currentPrices[page],
                    bidCount = bidCounts[page],
                    remainingSeconds = remainingSeconds[page],
                    favorite = auction.id in favoriteIds,
                    onFavoriteChange = { selected ->
                        favoriteIds = if (selected) (favoriteIds + auction.id).distinct() else favoriteIds - auction.id
                    },
                    onRefresh = {
                        scope.launch {
                            feedPagerState.animateScrollToPage(0)
                            snackbar.showSnackbar("최신 경매를 불러왔어요")
                        }
                    },
                    onProductClick = { onProductClick(auction.id) },
                    onBidClick = { selectedBidPage = page },
                    showBidSuccess = bidSuccessPage == page,
                    showExtended = extendedPage == page,
                    lastBidAmount = lastBidAmount
                )
            }
            AnimatedVisibility(visible = showFirstEntryGuide, enter = fadeIn(), exit = fadeOut()) {
                FeedFirstEntryGuide(onDismiss = { showFirstEntryGuide = false })
            }
        }
    }

    if (selectedBidPage >= 0) {
        FeedBidSheet(
            currentPrice = currentPrices[selectedBidPage],
            onDismiss = { selectedBidPage = -1 },
            onConfirm = { submission ->
                val page = selectedBidPage
                selectedBidPage = -1
                val shouldExtend = remainingSeconds[page] in 1..30
                currentPrices[page] = submission.amount
                bidCounts[page]++
                if (shouldExtend) remainingSeconds[page] += 15
                lastBidAmount = submission.amount
                if (shouldExtend) extendedPage = page else bidSuccessPage = page
            }
        )
    }
}

@Composable
private fun FeedPage(
    auction: FeedAuction,
    currentPrice: Int,
    bidCount: Int,
    remainingSeconds: Int,
    favorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onProductClick: () -> Unit,
    onBidClick: () -> Unit,
    showBidSuccess: Boolean,
    showExtended: Boolean,
    lastBidAmount: Int
) {
    val imagePagerState = rememberPagerState(pageCount = { auction.pageCount })
    var showFavoriteFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(showFavoriteFeedback) {
        if (showFavoriteFeedback) {
            delay(700)
            showFavoriteFeedback = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFC2C2C2))) {
        HorizontalPager(state = imagePagerState, modifier = Modifier.fillMaxSize()) { imagePage ->
            Box(
                Modifier.fillMaxSize().background(
                    when (imagePage % 3) {
                        0 -> Color(0xFFC2C2C2)
                        1 -> Color(0xFFB8B8B8)
                        else -> Color(0xFFCCCCCC)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("상품 영상 / 이미지", color = Color(0xFF595959), fontSize = 12.sp)
            }
        }

        Box(
            Modifier.fillMaxWidth().height(230.dp).align(Alignment.BottomCenter).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .76f)))
            )
        )

        Surface(
            modifier = Modifier.padding(start = 16.dp, top = 18.dp).size(34.dp).align(Alignment.TopStart),
            color = Color.Black.copy(alpha = .28f),
            shape = CircleShape,
            onClick = onRefresh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("↻", color = Color.White.copy(alpha = .96f), fontSize = 24.sp)
            }
        }

        Surface(
            modifier = Modifier.padding(end = 16.dp, top = 18.dp).align(Alignment.TopEnd),
            color = Color(0xFF1F1F1F).copy(alpha = .55f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "${imagePagerState.currentPage + 1} / ${auction.pageCount}",
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 216.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(auction.pageCount) { index ->
                Box(
                    Modifier.size(if (index == imagePagerState.currentPage) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (index == imagePagerState.currentPage) 1f else .55f))
                )
            }
        }

        AnimatedVisibility(
            visible = showFavoriteFeedback,
            modifier = Modifier.align(Alignment.Center).offset(y = (-36).dp),
            enter = fadeIn() + scaleIn(initialScale = .55f),
            exit = fadeOut() + scaleOut(targetScale = 1.15f)
        ) {
            Image(
                painter = painterResource(R.drawable.favorite_selected),
                contentDescription = null,
                modifier = Modifier.size(76.dp),
                colorFilter = ColorFilter.tint(Colors.Favorite)
            )
        }

        AnimatedVisibility(
            visible = showBidSuccess,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).offset(y = 54.dp),
            enter = fadeIn() + scaleIn(initialScale = .9f),
            exit = fadeOut()
        ) {
            Surface(color = ColorsMint, shape = RoundedCornerShape(10.dp)) {
                Text(
                    "↗ 새 입찰 ${"%,d".format(lastBidAmount)}원",
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = Color(0xFF195E4E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    auction.name,
                    Modifier.clickable(onClick = onProductClick),
                    color = Color.White.copy(alpha = .98f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "상세보기 ›",
                    Modifier.clickable(onClick = onProductClick),
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                when {
                    remainingSeconds <= 0 -> "경매 종료"
                    remainingSeconds <= 60 -> "마감 임박"
                    else -> "마감까지"
                },
                color = Color.White.copy(alpha = .72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    formatClock(remainingSeconds),
                    color = if (remainingSeconds in 1..60) Color(0xFFFF805F) else Color.White,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("남음", color = Color.White.copy(alpha = .82f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("%,d원".format(currentPrice), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("입찰 ${bidCount}명", color = Color.White.copy(alpha = .78f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onBidClick,
                enabled = remainingSeconds > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF212121),
                    disabledContainerColor = Color.White.copy(alpha = .62f),
                    disabledContentColor = Color(0xFF595959)
                )
            ) {
                Text(
                    if (remainingSeconds > 0) "%,d원 입찰하기".format(currentPrice + 500) else "경매가 종료됐어요",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(
            visible = showExtended,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).offset(y = 54.dp),
            enter = fadeIn() + scaleIn(initialScale = .9f),
            exit = fadeOut()
        ) {
            Surface(color = Color(0xFFFFEEE8), shape = RoundedCornerShape(18.dp)) {
                Text(
                    "◷ 새 입찰로 마감 15초 연장",
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = Color(0xFFB83813),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 139.dp).size(44.dp, 52.dp)
                .clickable {
                    if (!favorite) showFavoriteFeedback = true
                    onFavoriteChange(!favorite)
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(if (favorite) R.drawable.favorite_selected else R.drawable.favorite_outline),
                contentDescription = if (favorite) "찜 해제" else "찜하기",
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(if (favorite) Colors.Favorite else Color.White)
            )
            Text("찜", color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FeedFirstEntryGuide(onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .56f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(color = Color.Black.copy(alpha = .5f), shape = CircleShape, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("↔", color = Color.White, fontSize = 34.sp) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("좌우로 사진 보기", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("상품의 다른 사진을 확인해요", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Surface(color = Color.Black.copy(alpha = .5f), shape = CircleShape, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("↑", color = Color.White, fontSize = 34.sp) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("위로 넘겨 다음 경매", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("다른 경매 카드로 이동", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
            }
            Text("화면을 눌러 시작", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
        }
    }
}

private val ColorsMint = Color(0xFFD3F2E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedBidSheet(
    currentPrice: Int,
    onDismiss: () -> Unit,
    onConfirm: (BidSubmission) -> Unit
) {
    val minimum = currentPrice + 1
    var amountText by rememberSaveable(currentPrice) { mutableStateOf(minimum.toString()) }
    var paymentMethodId by rememberSaveable { mutableStateOf(samplePaymentMethods.first().id) }
    var addressId by rememberSaveable { mutableStateOf(sampleBidAddresses.first().id) }
    val amount = amountText.toIntOrNull() ?: 0
    val valid = amount >= minimum && paymentMethodId.isNotBlank() && addressId.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(width = 40.dp, height = 4.dp, color = Color(0xFFB8B8B8)) }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("입찰하기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("×", Modifier.clickable(onClick = onDismiss).padding(8.dp), color = Color(0xFF6B6B6B), fontSize = 24.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("현재가", color = Color(0xFF6B6B6B), fontSize = 11.sp)
                    Text("%,d원".format(currentPrice), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text("${"%,d".format(minimum)}원 이상 입찰 가능", color = Color(0xFF858585), fontSize = 11.sp)
            }
            Text("입찰 금액", fontSize = 12.sp)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                suffix = { Text("원", fontWeight = FontWeight.Bold) },
                isError = amountText.isNotEmpty() && !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp)
            )
            BidParticipationFields(
                selectedPaymentMethodId = paymentMethodId,
                onPaymentMethodSelected = { paymentMethodId = it },
                selectedAddressId = addressId,
                onAddressSelected = { addressId = it }
            )
            Text(
                if (valid) "첫 입찰에는 상품별 보증금 1,000원이 필요해요" else "현재가보다 큰 금액을 입력해주세요",
                color = if (valid) Color(0xFF6B6B6B) else Color(0xFFB34821),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = { onConfirm(BidSubmission(amount, paymentMethodId, addressId)) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13284B))
            ) {
                Text("${"%,d".format(amount)}원 입찰하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
