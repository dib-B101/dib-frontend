package com.ssafy.dib.feature.auction

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Figma 02_UI Design / 03_Product_Detail with a functional bid sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(productId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val product = allHomeAuctions.firstOrNull { it.id == productId } ?: allHomeAuctions.first()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var favorite by rememberSaveable(productId) { mutableStateOf(false) }
    var showBidSheet by rememberSaveable { mutableStateOf(false) }
    var remainingSeconds by rememberSaveable(productId) { mutableIntStateOf(product.remainingSeconds) }

    LaunchedEffect(productId) {
        while (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds--
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
                price = product.price + 500,
                favorite = favorite,
                onFavorite = { favorite = it },
                onBid = { if (remainingSeconds > 0) showBidSheet = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(Modifier.fillMaxWidth().height(256.dp).clip(RoundedCornerShape(0.dp))) {
                    ProductPhoto(product.photo, Modifier.fillMaxSize())
                    Surface(color = Colors.Background, shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                        Text("1 / 1", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
            item { ProductSummary(product.name, product.price, product.bidCount, remainingSeconds) }
            item {
                SellerSummary(onClick = {
                    scope.launch { snackbar.showSnackbar("판매자 프로필 화면은 다음 단계에서 연결해요") }
                })
            }
            item {
                ProductInformation(onReport = {
                    scope.launch { snackbar.showSnackbar("신고 화면은 다음 단계에서 연결해요") }
                })
            }
        }
    }

    if (showBidSheet) {
        BidSheet(
            productName = product.name,
            currentPrice = product.price,
            onDismiss = { showBidSheet = false },
            onContinue = { amount ->
                showBidSheet = false
                scope.launch { snackbar.showSnackbar("${"%,d".format(amount)}원 입찰 · 보증금 결제 단계로 이동합니다") }
            }
        )
    }
}

@Composable
private fun DetailAppBar(onBack: () -> Unit, onShare: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(Colors.Background).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onBack) {
            Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
        Text("상품 상세", fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.28).sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onShare) {
            Image(painterResource(R.drawable.share), "공유", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
    }
}

@Composable
private fun ProductSummary(name: String, price: Int, bidCount: Int, remainingSeconds: Int) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge("마감 임박", urgent = true)
            Badge("카메라 · 상태 중")
        }
        Text(name, fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("현재가", "%,d원".format(price), Colors.Navy, 28)
            Metric("마감까지", formatClock(remainingSeconds), Colors.Urgent, 24)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("시작가 20,000원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Text("${bidCount}명 입찰 중", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Row(Modifier.fillMaxWidth().background(Colors.Mint, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.trending_up), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Colors.MintInk))
            Text("%,d원부터 입찰할 수 있어요".format(price + 500), color = Colors.MintInk, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun Badge(label: String, urgent: Boolean = false) {
    Surface(color = if (urgent) Colors.UrgentBackground else Colors.Surface, shape = RoundedCornerShape(8.dp)) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = if (urgent) Colors.Urgent else Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
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
private fun ProductInformation(onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        InfoBlock("배송 정보", "안전배송 · 배송비 포함")
        InfoBlock("상품 설명", "필름 감성이 살아있는 빈티지 카메라입니다. 사용감은 있지만 촬영과 기본 기능은 정상 작동합니다. 구성품과 외관 상태는 사진을 확인해주세요.")
        Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("입찰 전, 확인해주세요", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text("• 최소 입찰 단위 500원\n• 첫 입찰 보증금 2,000원\n• 종료 30초 이내 새 입찰 시\n  남은 시간이 30초로 갱신돼요\n• 입찰 후에는 취소할 수 없어요",
                color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Text("이 상품 신고하기", Modifier.clickable(onClick = onReport).padding(vertical = 4.dp),
            color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
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
private fun StickyBidAction(price: Int, favorite: Boolean, onFavorite: (Boolean) -> Unit, onBid: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("첫 입찰 시 보증금 2,000원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(48.dp, 52.dp).background(Colors.Surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                DibWishlistButton(favorite, onFavorite, "상품")
            }
            Button(
                onClick = onBid,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
            ) {
                Text("%,d원 입찰하기".format(price), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BidSheet(productName: String, currentPrice: Int, onDismiss: () -> Unit, onContinue: (Int) -> Unit) {
    val minimum = currentPrice + 500
    var amountText by rememberSaveable { mutableStateOf(minimum.toString()) }
    val amount = amountText.toIntOrNull() ?: 0
    val valid = amount >= minimum && amount % 500 == 0
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Colors.Border) }
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("내 가격으로 입찰하기", fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Image(painterResource(R.drawable.close), "닫기", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                }
            }
            Text("$productName · 현재가 ${"%,d".format(currentPrice)}원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("입찰 금액", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("최소 ${"%,d".format(minimum)}원 · 500원 단위", color = Colors.Muted, fontSize = 12.sp)
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(9) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("원", fontWeight = FontWeight.Bold) },
                isError = amountText.isNotEmpty() && !valid,
                supportingText = if (amountText.isNotEmpty() && !valid) {{ Text("최소 금액 이상, 500원 단위로 입력해주세요") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(color = Colors.Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Colors.Navy, unfocusedBorderColor = Colors.Navy)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1_000, 5_000).forEach { increment ->
                    Button(onClick = { amountText = ((amountText.toIntOrNull() ?: minimum) + increment).toString() },
                        modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Surface, contentColor = Colors.Navy),
                        contentPadding = PaddingValues(0.dp)) {
                        Text("+%,d원".format(increment), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(Colors.Mint, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("첫 입찰 보증금 2,000원", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("입찰 금액과 별도로 한 번만 결제해요.\n패찰 시 경매 종료 후 자동 반환돼요.", color = Colors.MintInk, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Text("입찰 후에는 취소할 수 없어요.\n종료 30초 이내 새 입찰 시 30초로 갱신돼요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Button(onClick = { onContinue(amount) }, enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                Text("보증금 결제로 계속", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
