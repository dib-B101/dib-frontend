package com.ssafy.dib.feature.auction

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.core.ui.DibProfileAvatar
import com.ssafy.dib.core.ui.DibReportButton
import com.ssafy.dib.core.ui.DibSnackbarHost
import com.ssafy.dib.core.ui.auctionUrgencyPulse
import com.ssafy.dib.core.time.formatServerTime
import com.ssafy.dib.core.time.formatRemainingTime
import com.ssafy.dib.feature.home.ProductPhoto
import com.ssafy.dib.feature.home.allHomeAuctions
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.domain.product.ProductDetail
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.domain.auction.AuctionBidHistoryItem
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class DetailAuctionState { Active, HighestBidder, Scheduled, Cancelled, Lost, Won }

internal fun detailAuctionState(status: String, remainingSeconds: Int, isHighestBidder: Boolean): DetailAuctionState =
    when (status.uppercase()) {
        "SCHEDULED" -> DetailAuctionState.Scheduled
        "CANCELLED", "CANCELED" -> DetailAuctionState.Cancelled
        "ACTIVE" -> when {
            remainingSeconds > 0 && isHighestBidder -> DetailAuctionState.HighestBidder
            remainingSeconds > 0 -> DetailAuctionState.Active
            isHighestBidder -> DetailAuctionState.Won
            else -> DetailAuctionState.Lost
        }
        "ENDED" -> if (isHighestBidder) DetailAuctionState.Won else DetailAuctionState.Lost
        else -> DetailAuctionState.Lost
    }

data class RealtimeBidFeedback(
    val accepted: Boolean,
    val message: String,
    val currentPrice: Int?,
    val minAllowedAmount: Int?,
    val errorCode: String?,
    val eventKey: String
)

/** Figma 01_Wireframe / 03_Product_Detail states with a functional bid sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    remoteAuction: HomeAuction?,
    productDetail: ProductDetail?,
    showSampleContent: Boolean,
    remoteLoading: Boolean,
    remoteError: String?,
    onRetry: () -> Unit,
    bidHistory: List<AuctionBidHistoryItem>?,
    bidHistoryLoading: Boolean,
    bidHistoryError: String?,
    bidHistoryHasNext: Boolean,
    onBidHistoryRetry: () -> Unit,
    onBidHistoryLoadMore: () -> Unit,
    bookmarkLoading: Boolean,
    bookmarkError: String?,
    onBookmarkChange: (Boolean) -> Unit,
    realtimeStatus: String?,
    realtimeNotice: String?,
    realtimeBiddingEnabled: Boolean,
    realtimeConnected: Boolean,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (Int) -> Boolean,
    isAuthenticated: Boolean,
    isOwnAuction: Boolean,
    onBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    onSellerClick: (String) -> Unit,
    onReportClick: () -> Unit,
    onTransactionClick: () -> Unit,
    onLoginRequired: () -> Unit,
    similarProducts: List<RegisteredProduct>?,
    similarProductsLoading: Boolean,
    similarProductsError: String?,
    onSimilarProductsRetry: () -> Unit,
    onSimilarProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = remoteAuction ?: if (showSampleContent) {
        allHomeAuctions.firstOrNull { it.id == productId } ?: allHomeAuctions.first()
    } else null
    if (product == null) {
        Scaffold(
            modifier = modifier.fillMaxSize().safeDrawingPadding(),
            containerColor = Colors.Background,
            contentColor = Colors.Text,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { DetailAppBar(onBack = onBack, onShare = {}, shareEnabled = false) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (remoteLoading) {
                        CircularProgressIndicator(color = Colors.Navy)
                        Text("경매 정보를 불러오고 있어요", color = Colors.Muted, fontSize = 13.sp)
                    } else {
                        Text("경매 정보를 불러오지 못했어요", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(remoteError ?: "잠시 후 다시 시도해주세요.", color = Colors.Muted, fontSize = 12.sp)
                        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                            Text("다시 불러오기")
                        }
                    }
                }
            }
        }
        return
    }
    val productName = productDetail?.title ?: product.name
    val productImages = productDetail?.imageUrls?.takeIf { it.isNotEmpty() } ?: product.imageUrls
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var favorite by rememberSaveable(productId) { mutableStateOf(remoteAuction?.bookmarked == true) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var remainingSeconds by rememberSaveable(productId) { mutableIntStateOf(if (productId == "lost" || productId == "won") 0 else product.remainingSeconds) }
    var currentPrice by rememberSaveable(productId) { mutableIntStateOf(product.price) }
    var isHighestBidder by rememberSaveable(productId) { mutableStateOf(productId == "won") }
    var myHighestBidAmount by rememberSaveable(productId) { mutableStateOf(product.myBidAmount) }
    var bidError by rememberSaveable { mutableStateOf("") }
    var bidSubmitting by rememberSaveable(productId) { mutableStateOf(false) }
    val auctionState = detailAuctionState(product.status, remainingSeconds, isHighestBidder)

    // 0초에서 멈추지 않고 계속 돈다. 예전엔 0이 되면 루프가 끝나, 예정 경매가 시작돼 남은 시간이 새로 들어와도
    // 숫자가 줄지 않았다
    LaunchedEffect(productId) {
        while (true) {
            delay(1_000)
            if (remainingSeconds > 0) remainingSeconds--
        }
    }

    LaunchedEffect(remoteAuction?.price, remoteAuction?.bidCount, remoteAuction?.remainingSeconds, remoteAuction?.status) {
        remoteAuction?.let { updated ->
            currentPrice = updated.price
            remainingSeconds = if (updated.status.equals("ACTIVE", ignoreCase = true)) updated.remainingSeconds else 0
            updated.myBidAmount?.let { myHighestBidAmount = it }
            updated.isHighestBidder?.let { isHighestBidder = it }
            myHighestBidAmount?.let { ownBid ->
                if (isHighestBidder && updated.price > ownBid) isHighestBidder = false
            }
        }
    }

    LaunchedEffect(remoteAuction?.bookmarked) {
        remoteAuction?.let { favorite = it.bookmarked }
    }

    LaunchedEffect(bookmarkError) {
        bookmarkError?.let { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(realtimeNotice) {
        realtimeNotice?.let { snackbar.showSnackbar(it.substringBefore('|')) }
    }

    LaunchedEffect(realtimeBidFeedback?.eventKey) {
        val feedback = realtimeBidFeedback ?: return@LaunchedEffect
        bidSubmitting = false
        feedback.currentPrice?.let { currentPrice = it }
        if (feedback.accepted) {
            isHighestBidder = true
            myHighestBidAmount = feedback.currentPrice
            bidError = ""
            snackbar.showSnackbar(feedback.message)
        } else {
            if (feedback.errorCode == "AUCTION_NOT_ACTIVE") remainingSeconds = 0
            val minimumGuide = feedback.minAllowedAmount?.let { "\n최소 ${"%,d".format(it)}원부터 입찰할 수 있어요." }.orEmpty()
            bidError = feedback.message + minimumGuide
            showBidSheet = feedback.errorCode != "AUCTION_NOT_ACTIVE"
            snackbar.showSnackbar(feedback.message)
        }
    }

    LaunchedEffect(bidSubmitting) {
        if (bidSubmitting) {
            delay(10_000L)
            if (bidSubmitting) {
                bidSubmitting = false
                snackbar.showSnackbar("입찰 응답이 늦어지고 있어요. 현재가를 확인한 뒤 다시 시도해주세요.")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentColor = Colors.Text,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DetailAppBar(onBack = onBack, onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "dib 경매 · $productName · ${product.priceLabel}")
                }
                context.startActivity(Intent.createChooser(share, "상품 공유"))
            })
        },
        bottomBar = {
            StickyBidAction(
                price = minimumBidAmount(currentPrice, product.bidCount),
                favorite = favorite,
                state = auctionState,
                submitting = bidSubmitting,
                biddingAvailable = realtimeBiddingEnabled,
                isOwnAuction = isOwnAuction,
                onFavorite = { selected ->
                    if (isAuthenticated && !bookmarkLoading) {
                        favorite = selected
                        onBookmarkChange(selected)
                        scope.launch {
                            snackbar.showSnackbar(if (selected) "찜 목록에 저장했어요" else "찜에서 삭제했어요")
                        }
                    } else {
                        onLoginRequired()
                    }
                },
                onBid = {
                    if (!isAuthenticated) onLoginRequired()
                    else if (isOwnAuction) scope.launch { snackbar.showSnackbar("내 경매에는 입찰할 수 없어요.") }
                    else if (!realtimeBiddingEnabled) scope.launch { snackbar.showSnackbar("실시간 입찰 연결을 사용할 수 없어요.") }
                    else if (auctionState == DetailAuctionState.Active && !bidSubmitting) showBidSheet = true
                },
                onTransaction = onTransactionClick
            )
        },
        snackbarHost = { DibSnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (remoteLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Mint) }
            realtimeStatus?.takeUnless { it == "실시간 연결됨" }?.let { status ->
                item {
                    Text(
                        "● $status",
                        Modifier.fillMaxWidth().background(Colors.Mint.copy(alpha = .18f)).padding(horizontal = 16.dp, vertical = 7.dp),
                        color = Colors.MintInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            remoteError?.let { message ->
                item {
                    Row(
                        Modifier.fillMaxWidth().background(Colors.UrgentBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                        Text("다시 시도", Modifier.clickable(onClick = onRetry).padding(6.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                ProductGallery(
                    product.photo, productImages, auctionState, product.bidCount, remainingSeconds,
                    productDetail?.status, productDetail?.condition ?: product.productCondition, onImageClick
                )
            }
            item {
                ProductSummary(
                    productName, currentPrice, product.startPrice, product.bidCount,
                    remainingSeconds, auctionState, product.priceUndecided && currentPrice <= 0,
                    productDetail, product,
                    onSellerClick = { onSellerClick(productDetail?.memberId ?: product.sellerMemberId) }
                )
            }
            item {
                ProductInformation(
                    productName = productName,
                    category = product.category,
                    auction = product,
                    detail = productDetail,
                    canReport = !isOwnAuction,
                    onReport = onReportClick
                )
            }
            item {
                AuctionBidHistorySection(
                    items = bidHistory,
                    isLoading = bidHistoryLoading,
                    errorMessage = bidHistoryError,
                    hasNext = bidHistoryHasNext,
                    onRetry = onBidHistoryRetry,
                    onLoadMore = onBidHistoryLoadMore
                )
            }
            if (similarProductsLoading || similarProductsError != null || !similarProducts.isNullOrEmpty()) {
                item {
                    SimilarProductsSection(
                        products = similarProducts.orEmpty(),
                        loading = similarProductsLoading,
                        errorMessage = similarProductsError,
                        onRetry = onSimilarProductsRetry,
                        onProductClick = onSimilarProductClick
                    )
                }
            }
        }
    }

    if (showBidSheet) {
        AuctionBidSheet(
            productName = productName,
            currentPrice = currentPrice,
            bidCount = product.bidCount,
            submissionError = bidError,
            onDismiss = { showBidSheet = false; bidError = "" },
            onContinue = { submission ->
                if (!isValidBidAmount(submission.amount, minimumBidAmount(currentPrice, product.bidCount))) {
                    bidError = "다른 입찰이 먼저 반영됐어요\n최신 입찰가를 확인하고 다시 입찰해 주세요."
                } else if (!realtimeBiddingEnabled) {
                    bidError = "실시간 입찰 연결을 사용할 수 없어요."
                } else if (!realtimeConnected) {
                    bidError = "실시간 연결 중이에요. 연결된 뒤 다시 눌러주세요."
                } else if (onRealtimeBid(submission.amount)) {
                    showBidSheet = false
                    bidError = ""
                    bidSubmitting = true
                    scope.launch { snackbar.showSnackbar("${"%,d".format(submission.amount)}원 입찰 결과를 확인하고 있어요.") }
                } else {
                    bidError = "입찰 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요."
                }
            }
        )
    }
}

@Composable
private fun SimilarProductsSection(
    products: List<RegisteredProduct>,
    loading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onProductClick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("비슷한 상품", Modifier.weight(1f), color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Colors.MintInk)
        }
        errorMessage?.let { message ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(Colors.UrgentBackground, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                TextButton(onClick = onRetry) { Text("재시도", fontSize = 11.sp) }
            }
        }
        if (products.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(products, key = { it.productId }) { product ->
                    Column(Modifier.width(144.dp).clickable { product.auctionId?.let(onProductClick) }) {
                        DibNetworkImage(product.thumbnailUrl, product.title, Modifier.fillMaxWidth().height(144.dp))
                        Text(product.title, Modifier.padding(top = 8.dp), maxLines = 2, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(productConditionLabelForCard(product.condition), color = Colors.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun productConditionLabelForCard(condition: String): String = when (condition.uppercase()) {
    "NEW" -> "새 상품"
    "LIKE_NEW" -> "거의 새 상품"
    "GOOD" -> "좋음"
    "NORMAL" -> "보통"
    "BAD", "FAIR" -> "사용감 있음"
    else -> condition
}

@Composable
private fun AuctionBidHistorySection(
    items: List<AuctionBidHistoryItem>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("입찰 이력", fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
        errorMessage?.let { message ->
            Row(Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                TextButton(onRetry) { Text("재시도", fontSize = 11.sp) }
            }
        }
        if (items.isNullOrEmpty() && isLoading) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = Colors.Mint, strokeWidth = 2.dp) }
        } else if (items.isNullOrEmpty() && errorMessage == null) {
            Text("아직 입찰 내역이 없어요.", Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(10.dp)).padding(16.dp), color = Colors.Muted, fontSize = 12.sp)
        } else {
            val historyItems = items.orEmpty()
            val visibleItems = if (expanded) historyItems else historyItems.take(3)
            if (isLoading) Text("입찰 이력 갱신 중", color = Colors.Muted, fontSize = 11.sp)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Colors.Border)) {
                visibleItems.forEachIndexed { index, bid ->
                    // 첫 줄이 현재 최고 입찰(최신순 = 최고가순). 배경과 배지로 눈에 띄게 한다
                    val top = index == 0
                    Row(Modifier.fillMaxWidth().background(if (top) Colors.NavySoft else Colors.Background).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(bid.bidderNickname ?: bid.maskedBidderId, color = if (top) Colors.Navy else Colors.Text, fontSize = if (top) 13.sp else 12.sp, fontWeight = FontWeight.Bold)
                                if (top) Surface(color = Colors.Navy, shape = RoundedCornerShape(6.dp)) {
                                    Text("최고 입찰", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(formatBidCreatedAt(bid.createdAt), color = Colors.Muted, fontSize = 10.sp)
                        }
                        Text("${"%,d".format(bid.amount)}원", color = Colors.Navy, fontSize = if (top) 16.sp else 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (index < visibleItems.lastIndex) HorizontalDivider(color = Colors.Border)
                }
            }
            if (historyItems.size > 3 && !expanded) OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(10.dp)) {
                Text("입찰 이력 더 보기", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else if (hasNext) OutlinedButton(onLoadMore, Modifier.fillMaxWidth().height(44.dp), enabled = !isLoading, shape = RoundedCornerShape(10.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Colors.Navy, strokeWidth = 2.dp) else Text("입찰 이력 더 보기", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    HorizontalDivider(color = Colors.Border)
}

private fun formatBidCreatedAt(value: String): String =
    formatServerTime(value) ?: value.take(16).replace('T', ' ')

@Composable
private fun DetailAppBar(onBack: () -> Unit, onShare: () -> Unit, shareEnabled: Boolean = true) {
    DibSubAppBar("경매 상세", onBack, actions = {
        IconButton(onClick = onShare, enabled = shareEnabled) {
            Image(painterResource(R.drawable.share), "공유", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
    })
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProductGallery(photo: ProductPhoto, imageUrls: List<String>, state: DetailAuctionState, bidCount: Int, remainingSeconds: Int, productStatus: String?, condition: String?, onImageClick: (Int) -> Unit) {
    val pageCount = imageUrls.size.takeIf { it > 0 } ?: 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(Colors.Image)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier.fillMaxSize().clickable { onImageClick(page) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrls.isNotEmpty()) {
                    ProductPhoto(photo, imageUrls[page], Modifier.fillMaxSize())
                } else {
                    ProductPhoto(photo, modifier = Modifier.fillMaxSize())
                }
            }
        }
        // 끝난 경매는 사진 전체를 반투명 검정으로 덮고 그 위에 상태를 크게 쓴다.
        // 예전엔 가운데 작은 남색 상자 하나라 사진 위에서 글자가 잘 안 보였고, 낙찰·유찰·거래 완료 구분도 없었다
        endedGalleryLabel(state, bidCount, productStatus)?.let { (label, caption) ->
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                    caption?.let { Text(it, color = Color.White.copy(alpha = .85f), fontSize = 13.sp) }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val finishedLabel = endedGalleryLabel(state, bidCount, productStatus)?.first
            when {
                state == DetailAuctionState.Cancelled -> GalleryBadge("경매 취소", R.drawable.close, Color(0xDD1A1A1A))
                finishedLabel != null -> GalleryBadge(
                    finishedLabel,
                    if (finishedLabel == "유찰") R.drawable.warning_outline else R.drawable.check_circle,
                    Color(0xDD1A1A1A)
                )
                state == DetailAuctionState.Scheduled -> GalleryBadge("경매 예정", R.drawable.timer_outline, Colors.Navy)
                else -> {
                    GalleryBadge("실시간 경매", null, Color(0xFFE43D4B))
                    if (remainingSeconds in 1..300) {
                        GalleryBadge("마감 임박", R.drawable.timer_outline, Color(0xFFFFECEE), Colors.Urgent)
                    }
                }
            }
            GalleryBadge("상태 ${conditionLabel(condition)}", R.drawable.product_outline, Color(0xE614294A))
        }
        if (pageCount > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = .48f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .semantics { contentDescription = "상품 사진 ${pagerState.currentPage + 1} / $pageCount" },
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    Box(
                        Modifier.size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .background(
                                if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = .55f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryBadge(label: String, icon: Int?, background: Color, foreground: Color = Color.White) {
    Row(
        Modifier.shadow(2.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
            .background(background).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon == null) BroadcastIcon(foreground)
        else Image(painterResource(icon), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(foreground))
        Text(label, color = foreground, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BroadcastIcon(color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val stroke = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color, radius = 1.5.dp.toPx(), center = center)
        for (radius in listOf(3.5.dp.toPx(), 5.5.dp.toPx())) {
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)
            drawArc(color, 125f, 110f, false, topLeft, arcSize, style = stroke)
            drawArc(color, -55f, 110f, false, topLeft, arcSize, style = stroke)
        }
    }
}

/** 끝난 경매 사진 위에 덮을 문구(제목, 설명). 진행·예정 경매면 null */
internal fun endedGalleryLabel(state: DetailAuctionState, bidCount: Int, productStatus: String?): Pair<String, String?>? = when {
    productStatus.equals("SOLD", ignoreCase = true) && state != DetailAuctionState.Active && state != DetailAuctionState.HighestBidder ->
        "거래 완료" to (if (state == DetailAuctionState.Won) "내가 낙찰한 상품이에요" else null)
    state == DetailAuctionState.Won -> "낙찰 완료" to "내가 낙찰한 상품이에요"
    state == DetailAuctionState.Lost -> if (bidCount > 0) "낙찰 완료" to "다른 분이 낙찰했어요" else "유찰" to "입찰 없이 끝난 경매예요"
    state == DetailAuctionState.Cancelled -> "경매 취소" to null
    else -> null
}

@Composable
private fun ProductSummary(
    name: String,
    price: Int,
    startPrice: Int,
    bidCount: Int,
    remainingSeconds: Int,
    state: DetailAuctionState,
    priceUndecided: Boolean,
    sellerDetail: ProductDetail?,
    auction: HomeAuction,
    onSellerClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Colors.Background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(name, color = Colors.Text, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp, fontWeight = FontWeight.Bold)
        SellerSummary(sellerDetail, auction, onSellerClick)
        Row(
            Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 시작가는 현재가의 보조 정보로 두고 남은 시간을 별도 칸에 표시한다.
            Metric(
                when (state) {
                    DetailAuctionState.Active, DetailAuctionState.HighestBidder -> "현재가"
                    DetailAuctionState.Scheduled -> "시작가"
                    DetailAuctionState.Cancelled -> "취소 시점 가격"
                    DetailAuctionState.Lost, DetailAuctionState.Won -> "낙찰가"
                },
                if (priceUndecided) "가격 미정" else "%,d원".format(price),
                Colors.Text,
                22,
                Modifier.weight(1.2f),
                supportingText = if (state == DetailAuctionState.Scheduled) null
                    else if (priceUndecided) "시작가 미정" else "시작가 ${"%,d".format(startPrice)}원"
            )
            Metric(
                when (state) {
                    DetailAuctionState.Lost -> "총 입찰"
                    DetailAuctionState.Won -> "경매 상태"
                    DetailAuctionState.Scheduled, DetailAuctionState.Cancelled -> "경매 상태"
                    DetailAuctionState.Active, DetailAuctionState.HighestBidder -> "남은 시간"
                },
                when (state) {
                    DetailAuctionState.Lost -> "${bidCount}회"
                    DetailAuctionState.Won -> "종료"
                    DetailAuctionState.Scheduled -> "시작 대기"
                    DetailAuctionState.Cancelled -> "취소"
                    DetailAuctionState.Active, DetailAuctionState.HighestBidder -> formatRemainingTime(remainingSeconds)
                },
                if (remainingSeconds in 1..59) Colors.Urgent else Colors.Text,
                18,
                Modifier.weight(1f),
                valueModifier = if (state == DetailAuctionState.Active || state == DetailAuctionState.HighestBidder) {
                    Modifier.auctionUrgencyPulse(remainingSeconds)
                } else Modifier
            )
        }
        if (state == DetailAuctionState.Lost) {
            Text("아쉽게 낙찰되지 않았어요", Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp)).padding(14.dp), color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (state == DetailAuctionState.Won) {
            Column(Modifier.fillMaxWidth().background(Color(0xFFFFF4ED), RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("거래까지 42분 남았어요", color = Color(0xFFD1381F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("기한이 지나면 낙찰이 취소될 수 있어요", color = Color(0xFFA3381F), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Badge(label: String, urgent: Boolean = false, success: Boolean = false, modifier: Modifier = Modifier) {
    val color = when {
        urgent -> Colors.UrgentBackground
        success -> Color(0xFFF0F7F2)
        else -> Colors.Surface
    }
    Surface(modifier, color = color, shape = RoundedCornerShape(8.dp)) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = when { urgent -> Colors.Urgent; success -> Color(0xFF297345); else -> Colors.Muted },
            fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Metric(label: String, value: String, color: androidx.compose.ui.graphics.Color, valueSize: Int, modifier: Modifier = Modifier, valueModifier: Modifier = Modifier, supportingText: String? = null) {
    Column(modifier) {
        Text(label, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Text(value, valueModifier, color = color, fontSize = valueSize.sp, lineHeight = (valueSize + 6).sp, fontWeight = FontWeight.Bold)
        supportingText?.let { Text(it, color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

@Composable
private fun SellerSummary(detail: ProductDetail?, auction: HomeAuction, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DibProfileAvatar(detail?.sellerProfileImageUrl ?: auction.sellerProfileImageUrl, 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(detail?.sellerNickname ?: auction.sellerNickname ?: "판매자", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            val sellerMeta = listOfNotNull(
                (detail?.sellerRating ?: auction.sellerRating)?.let { "★ $it" },
                (detail?.sellerTradeCount ?: auction.sellerTradeCount)?.let { "거래 ${it}회" }
            ).joinToString("  ·  ")
            Text(sellerMeta.ifBlank { "판매자 정보를 확인해보세요" }, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Colors.Muted))
    }
}

@Composable
private fun ProductInformation(
    productName: String,
    category: String,
    auction: HomeAuction,
    detail: ProductDetail?,
    canReport: Boolean,
    onReport: () -> Unit
) {
    val description = detail?.description?.takeIf(String::isNotBlank)
        ?: auction.productDescription?.takeIf(String::isNotBlank)
        ?: "$productName 상품입니다. 자세한 상태는 사진을 확인해주세요."
    val condition = detail?.condition ?: auction.productCondition
    val modelName = detail?.modelName?.takeIf(String::isNotBlank)
        ?: auction.productModelName?.takeIf(String::isNotBlank)
    val releaseYear = detail?.releaseYear ?: auction.productReleaseYear
    val marketPrice = detail?.marketPrice ?: auction.productMarketPrice
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoBlock("상품 설명", description)
        Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("상품 정보", fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Colors.Border)) {
                InfoRow("상품 상태", conditionLabel(condition))
                HorizontalDivider(color = Colors.Border)
                InfoRow("카테고리", category)
                modelName?.let { model -> HorizontalDivider(color = Colors.Border); InfoRow("모델명", model) }
                releaseYear?.let { year -> HorizontalDivider(color = Colors.Border); InfoRow("출시연도", "${year}년") }
                marketPrice?.let { price -> HorizontalDivider(color = Colors.Border); InfoRow("시세", "${"%,d".format(price)}원") }
            }
        }
        Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("배송 정보", fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge("안전배송", success = true)
                Badge("배송비 포함", success = true)
            }
        }
        // 입찰 전 안내는 입찰 시트(AuctionBidSheet)에서 한 번만 보여준다
        if (canReport) {
            HorizontalDivider(color = Colors.Border)
            DibReportButton("이 상품 신고하기", onClick = onReport)
        }
    }
}

private fun conditionLabel(condition: String?): String = when (condition?.uppercase()) {
    "NEW" -> "새 상품"
    "LIKE_NEW" -> "거의 새 상품"
    "GOOD" -> "좋음"
    "NORMAL" -> "보통"
    "BAD", "FAIR" -> "사용감 있음"
    else -> "정보 없음"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, Modifier.width(88.dp), color = Colors.Muted, fontSize = 12.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Colors.Muted, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun StickyBidAction(
    price: Int,
    favorite: Boolean,
    state: DetailAuctionState,
    submitting: Boolean,
    biddingAvailable: Boolean,
    isOwnAuction: Boolean,
    onFavorite: (Boolean) -> Unit,
    onBid: () -> Unit,
    onTransaction: () -> Unit
) {
    Column(Modifier.fillMaxWidth().height(78.dp).background(Colors.Background).padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (state in setOf(DetailAuctionState.Lost, DetailAuctionState.Scheduled, DetailAuctionState.Cancelled)) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (state) {
                        DetailAuctionState.Scheduled -> "아직 시작 전인 경매예요"
                        DetailAuctionState.Cancelled -> "취소된 경매예요"
                        else -> "경매가 종료된 상품이에요"
                    },
                    color = Colors.Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            return@Column
        }
        if (state == DetailAuctionState.Won) {
            Button(onClick = onTransaction, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                Text("거래 진행하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DibWishlistButton(favorite, onFavorite, "상품", Modifier.size(48.dp), plain = true)
            Button(
                onClick = onBid,
                enabled = state == DetailAuctionState.Active && !submitting && !isOwnAuction && biddingAvailable,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Colors.Navy,
                    disabledContainerColor = Colors.Surface,
                    disabledContentColor = Colors.Muted
                )
            ) {
                Text(
                    when {
                        isOwnAuction -> "내 경매에는 입찰할 수 없어요"
                        state == DetailAuctionState.HighestBidder -> "현재 최고 입찰 중이에요"
                        !biddingAvailable -> "실시간 입찰 연결이 필요해요"
                        submitting -> "입찰 결과 확인 중"
                        else -> "%,d원 입찰하기".format(price)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

