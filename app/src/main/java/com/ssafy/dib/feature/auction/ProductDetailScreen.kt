package com.ssafy.dib.feature.auction

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.feature.home.ProductPhoto
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.feature.home.allHomeAuctions
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DetailAuctionState { Active, HighestBidder, Lost, Won }

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
    remoteLoading: Boolean,
    remoteError: String?,
    onRetry: () -> Unit,
    realtimeStatus: String?,
    realtimeNotice: String?,
    realtimeBiddingEnabled: Boolean,
    realtimeBidFeedback: RealtimeBidFeedback?,
    onRealtimeBid: (Int) -> Boolean,
    onDepositInvalid: () -> Unit,
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    onSellerClick: (String) -> Unit,
    onReportClick: () -> Unit,
    onTransactionClick: () -> Unit,
    onLoginRequired: () -> Unit,
    paidBidAmount: Int,
    depositPaid: Boolean,
    onPaymentConsumed: () -> Unit,
    onDepositPayment: (BidSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = remoteAuction ?: allHomeAuctions.firstOrNull { it.id == productId } ?: allHomeAuctions.first()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var favorite by rememberSaveable(productId) { mutableStateOf(false) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var remainingSeconds by rememberSaveable(productId) { mutableIntStateOf(if (productId == "lost" || productId == "won") 0 else product.remainingSeconds) }
    var currentPrice by rememberSaveable(productId) { mutableIntStateOf(product.price) }
    var isHighestBidder by rememberSaveable(productId) { mutableStateOf(productId == "won") }
    var myHighestBidAmount by rememberSaveable(productId) { mutableStateOf(product.myBidAmount) }
    var bidError by rememberSaveable { mutableStateOf("") }
    var priceUpdateScheduled by rememberSaveable { mutableStateOf(false) }
    var bidSubmitting by rememberSaveable(productId) { mutableStateOf(false) }
    val auctionState = when {
        remainingSeconds > 0 && isHighestBidder -> DetailAuctionState.HighestBidder
        remainingSeconds > 0 -> DetailAuctionState.Active
        isHighestBidder -> DetailAuctionState.Won
        else -> DetailAuctionState.Lost
    }

    LaunchedEffect(productId) {
        while (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds--
        }
    }

    LaunchedEffect(remoteAuction?.price, remoteAuction?.bidCount, remoteAuction?.remainingSeconds, remoteAuction?.status) {
        remoteAuction?.let { updated ->
            currentPrice = updated.price
            remainingSeconds = if (updated.status == "ENDED") 0 else updated.remainingSeconds
            updated.myBidAmount?.let { myHighestBidAmount = it }
            updated.isHighestBidder?.let { isHighestBidder = it }
            myHighestBidAmount?.let { ownBid ->
                if (isHighestBidder && updated.price > ownBid) isHighestBidder = false
            }
        }
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
            if (feedback.errorCode == "DEPOSIT_REQUIRED") onDepositInvalid()
            if (feedback.errorCode == "AUCTION_NOT_ACTIVE") remainingSeconds = 0
            val minimumGuide = feedback.minAllowedAmount?.let { "\n최소 ${"%,d".format(it)}원부터 입찰할 수 있어요." }.orEmpty()
            bidError = feedback.message + minimumGuide
            showBidSheet = feedback.errorCode != "AUCTION_NOT_ACTIVE"
            snackbar.showSnackbar(feedback.message)
        }
    }

    LaunchedEffect(paidBidAmount) {
        if (paidBidAmount > 0) {
            if (realtimeBiddingEnabled) {
                val sent = onRealtimeBid(paidBidAmount)
                bidSubmitting = sent
                onPaymentConsumed()
                snackbar.showSnackbar(
                    if (sent) "${"%,d".format(paidBidAmount)}원 입찰 결과를 확인하고 있어요."
                    else "실시간 연결을 준비하고 있어요. 잠시 후 다시 입찰해주세요."
                )
                return@LaunchedEffect
            }
            val wasExtended = remainingSeconds in 1..30
            currentPrice = paidBidAmount
            isHighestBidder = true
            if (wasExtended) remainingSeconds += 15
            onPaymentConsumed()
            snackbar.showSnackbar(
                if (wasExtended) "보증금 결제·입찰 완료 · 경매 시간이 15초 연장됐어요"
                else "보증금 결제 완료 · ${"%,d".format(paidBidAmount)}원으로 입찰했어요"
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentColor = Colors.Text,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DetailAppBar(onBack = onBack, onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "dib 경매 · ${product.name} · ${product.priceLabel}")
                }
                context.startActivity(Intent.createChooser(share, "상품 공유"))
            })
        },
        bottomBar = {
            StickyBidAction(
                price = currentPrice + 1,
                favorite = favorite,
                state = auctionState,
                submitting = bidSubmitting,
                onFavorite = { selected ->
                    if (isAuthenticated) {
                        favorite = selected
                        scope.launch {
                            snackbar.showSnackbar(if (selected) "찜 목록에 저장했어요" else "찜에서 삭제했어요")
                        }
                    } else {
                        onLoginRequired()
                    }
                },
                onBid = {
                    if (!isAuthenticated) onLoginRequired()
                    else if (auctionState == DetailAuctionState.Active && !bidSubmitting) showBidSheet = true
                },
                onTransaction = onTransactionClick
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (remoteLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Mint) }
            realtimeStatus?.let { status ->
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
                ProductGallery(product.photo, product.imageUrls, onImageClick)
            }
            item { ProductSummary(product.name, currentPrice, product.bidCount, remainingSeconds, auctionState) }
            item {
                SellerSummary(onClick = { onSellerClick(product.sellerMemberId) })
            }
            item {
                ProductInformation(
                    productName = product.name,
                    category = product.category,
                    onReport = onReportClick
                )
            }
        }
    }

    if (showBidSheet) {
        LaunchedEffect(Unit) {
            if (!realtimeBiddingEnabled && !priceUpdateScheduled) {
                priceUpdateScheduled = true
                delay(2_500)
                if (showBidSheet) {
                    currentPrice += 500
                    bidError = "다른 입찰이 먼저 반영됐어요\n최신 입찰가를 확인하고 다시 입찰해 주세요."
                    snackbar.showSnackbar("새 입찰로 500원 올랐어요")
                }
            }
        }
        BidSheet(
            productName = product.name,
            currentPrice = currentPrice,
            submissionError = bidError,
            depositPaid = depositPaid,
            onDismiss = { showBidSheet = false; bidError = "" },
            onContinue = { submission ->
                if (submission.amount <= currentPrice) {
                    bidError = "다른 입찰이 먼저 반영됐어요\n최신 입찰가를 확인하고 다시 입찰해 주세요."
                } else {
                    showBidSheet = false
                    bidError = ""
                    onDepositPayment(submission)
                }
            }
        )
    }
}

@Composable
private fun DetailAppBar(onBack: () -> Unit, onShare: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).background(Colors.Background).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
        Text("상품 상세", Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.28).sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onShare) {
            Image(painterResource(R.drawable.share), "공유", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProductGallery(photo: ProductPhoto, imageUrls: List<String>, onImageClick: (Int) -> Unit) {
    val pageCount = imageUrls.size.takeIf { it > 0 } ?: 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    Box(Modifier.fillMaxWidth().height(236.dp).background(Colors.Image)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier.fillMaxSize().clickable { onImageClick(page) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrls.isNotEmpty()) {
                    ProductPhoto(photo, imageUrls[page], Modifier.fillMaxSize())
                } else if (page == 0) {
                    ProductPhoto(photo, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Colors.Image), contentAlignment = Alignment.Center) {
                        Text("상품 이미지 ${page + 1}", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        Surface(
            color = Colors.Background.copy(alpha = .9f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text(
                "${pagerState.currentPage + 1} / $pageCount",
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp
            )
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(pageCount) { index ->
                Box(
                    Modifier.size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                        .background(
                            if (index == pagerState.currentPage) Colors.Navy else Colors.Background.copy(alpha = .8f),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ProductSummary(
    name: String,
    price: Int,
    bidCount: Int,
    remainingSeconds: Int,
    state: DetailAuctionState
) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                DetailAuctionState.Lost -> Badge("경매 종료")
                DetailAuctionState.Won -> Badge("낙찰 완료", success = true)
                else -> Badge("마감 임박", urgent = true)
            }
            Badge("상품 상태 · 중")
        }
        Text(name, fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(if (state == DetailAuctionState.Active || state == DetailAuctionState.HighestBidder) "현재가" else "낙찰가", "%,d원".format(price), Colors.Navy, 24)
            Metric(
                when (state) {
                    DetailAuctionState.Lost -> "총 입찰"
                    DetailAuctionState.Won -> "거래까지"
                    else -> "남은 시간"
                },
                when (state) {
                    DetailAuctionState.Lost -> "${bidCount}명"
                    DetailAuctionState.Won -> "23시간 42분"
                    else -> formatClock(remainingSeconds)
                },
                if (remainingSeconds in 1..59) Colors.Urgent else Colors.Text,
                24
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("시작가 20,000원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Text("${bidCount}명 입찰 중", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        if (state == DetailAuctionState.Active) {
            Row(Modifier.fillMaxWidth().background(Colors.Mint, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(R.drawable.trending_up), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
                Text("현재가보다 큰 금액을 자유롭게 입력할 수 있어요", color = Colors.MintInk, fontSize = 12.sp, lineHeight = 18.sp)
            }
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
private fun Badge(label: String, urgent: Boolean = false, success: Boolean = false) {
    val color = when {
        urgent -> Colors.UrgentBackground
        success -> Color(0xFFF0F7F2)
        else -> Colors.Surface
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = when { urgent -> Colors.Urgent; success -> Color(0xFF297345); else -> Colors.Muted },
            fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Metric(label: String, value: String, color: androidx.compose.ui.graphics.Color, valueSize: Int) {
    Column {
        Text(label, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Text(value, color = color, fontSize = valueSize.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SellerSummary(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(20.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(Colors.Surface, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.seller), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Colors.Muted))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("seller01", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text("★ 4.8  ·  거래 32회", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Colors.Muted))
    }
    HorizontalDivider(color = Colors.Border)
}

@Composable
private fun ProductInformation(productName: String, category: String, onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("배송 정보", fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge("안전배송")
                Badge("배송비 포함")
            }
        }
        InfoBlock("상품 설명", "$productName 상품입니다. 사용감은 있지만 기본 기능은 정상 작동합니다. 구성품과 외관 상태는 사진을 확인해주세요.")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("상품 정보", fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Colors.Border)) {
                InfoRow("상품 상태", "중고 · 사용감 있음")
                HorizontalDivider(color = Colors.Border)
                InfoRow("카테고리", category)
            }
        }
        Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("입찰 전, 확인해주세요", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text("• 첫 입찰 전에 상품별 보증금 1,000원을 결제해요\n• 재입찰에는 추가 보증금이 없어요\n• 현재가보다 큰 금액을 자유롭게 입력해요\n• 종료 30초 이내 새 입찰 시 15초 연장돼요\n• 패찰 시 보증금은 자동 반환돼요",
                color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Text("이 상품 신고하기", Modifier.clickable(onClick = onReport).padding(vertical = 4.dp),
            color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    onFavorite: (Boolean) -> Unit,
    onBid: () -> Unit,
    onTransaction: () -> Unit
) {
    Column(Modifier.fillMaxWidth().height(76.dp).background(Colors.Background).padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (state == DetailAuctionState.Lost) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Text("경매가 종료된 상품이에요", color = Colors.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Box(Modifier.size(48.dp).background(Colors.Surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                DibWishlistButton(favorite, onFavorite, "상품")
            }
            Button(
                onClick = onBid,
                enabled = state == DetailAuctionState.Active && !submitting,
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
                        submitting -> "입찰 결과 확인 중"
                        state == DetailAuctionState.HighestBidder -> "✓ 현재 최고 입찰 중이에요"
                        else -> "%,d원 입찰하기".format(price)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BidSheet(productName: String, currentPrice: Int, submissionError: String, depositPaid: Boolean, onDismiss: () -> Unit, onContinue: (BidSubmission) -> Unit) {
    val minimum = currentPrice + 1
    var amountText by rememberSaveable { mutableStateOf(minimum.toString()) }
    var paymentMethodId by rememberSaveable { mutableStateOf(samplePaymentMethods.first().id) }
    var addressId by rememberSaveable { mutableStateOf(sampleBidAddresses.first().id) }
    val amount = amountText.toIntOrNull() ?: 0
    val valid = amount >= minimum && paymentMethodId.isNotBlank() && addressId.isNotBlank()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Colors.Border) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("내 가격으로 입찰하기", fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Image(painterResource(R.drawable.close), "닫기", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                }
            }
            Text("$productName · 현재가 ${"%,d".format(currentPrice)}원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            if (submissionError.isNotBlank()) {
                Text(submissionError, Modifier.fillMaxWidth().background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp), color = Color(0xFFD1381F), fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("입찰 금액", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${"%,d".format(minimum)}원 이상", color = Colors.Muted, fontSize = 12.sp)
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(9) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("원", fontWeight = FontWeight.Bold) },
                isError = amountText.isNotEmpty() && !valid,
                supportingText = if (amountText.isNotEmpty() && amount < minimum) {{ Text("현재가보다 큰 금액을 입력해주세요") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(color = Colors.Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Colors.Navy, unfocusedBorderColor = Colors.Navy)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1_000, 5_000, 10_000).forEach { increment ->
                    Button(onClick = { amountText = ((amountText.toIntOrNull() ?: minimum) + increment).toString() },
                        modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Surface, contentColor = Colors.Navy),
                        contentPadding = PaddingValues(0.dp)) {
                        Text("+%,d원".format(increment), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            BidParticipationFields(
                selectedPaymentMethodId = paymentMethodId,
                onPaymentMethodSelected = { paymentMethodId = it },
                selectedAddressId = addressId,
                onAddressSelected = { addressId = it }
            )
            Surface(color = Colors.Search, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (depositPaid) "보증금 결제 완료" else "첫 입찰 보증금 1,000원", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (depositPaid) "이 경매에서는 추가 결제 없이 재입찰할 수 있어요." else "입찰 금액과 별도로 한 번만 결제하며 패찰 시 자동 반환돼요.", color = Colors.MintInk, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
            Text("입찰 후에는 취소할 수 없어요.\n종료 30초 이내 새 입찰 시 15초 연장돼요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Button(onClick = { onContinue(BidSubmission(amount, paymentMethodId, addressId)) }, enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                Text(if (depositPaid) "${"%,d".format(amount)}원 입찰하기" else "보증금 결제로 계속", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
