package com.ssafy.dib.feature.main

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductRegistrationResult
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class TradeTab(val label: String) { Bid("입찰"), Purchase("구매"), Sale("판매") }
private enum class TradeTone { Urgent, Positive, Neutral }
private data class TradeItem(
    val status: String,
    val title: String,
    val meta: String,
    val action: String,
    val tone: TradeTone,
    val orderId: String = "sample"
)

@Composable
fun MyTradesScreen(
    onTabSelected: (DibMainTab) -> Unit,
    onProductClick: (String) -> Unit,
    onTransactionClick: (role: String, orderId: String) -> Unit,
    remotePurchaseOrders: List<OrderSummary>?,
    remoteSaleOrders: List<OrderSummary>?,
    remoteLoading: Boolean,
    remoteError: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(TradeTab.Bid) }
    var contentView by rememberSaveable { mutableStateOf(DibContentView.List) }
    val items = when (selected) {
        TradeTab.Bid -> listOf(
            TradeItem("다른 입찰 발생", "빈티지 필름 카메라", "현재가 35,000원 · 마감 00:42", "현재가보다 높게 입찰하기 →", TradeTone.Urgent),
            TradeItem("최고 입찰자", "빈티지 스니커즈", "내 입찰가 58,000원 · 마감 12분", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("경매 종료", "레더 숄더백", "최종가 72,000원 · 미낙찰", "결과 확인하기 →", TradeTone.Neutral)
        )
        TradeTab.Purchase -> remotePurchaseOrders?.map { it.toTradeItem(isSeller = false) }
            ?: if (remoteLoading || remoteError != null) emptyList() else listOf(
            TradeItem("결제 필요", "빈티지 필름 카메라", "낙찰가 35,000원 · 23:42:18 남음", "거래 진행하기 →", TradeTone.Urgent),
            TradeItem("배송 중", "노이즈 캔슬링 헤드폰", "판매자가 상품을 발송했어요", "배송 조회하기 →", TradeTone.Positive),
            TradeItem("구매 완료", "레더 카드지갑", "거래가 안전하게 완료됐어요", "거래 내역 보기 →", TradeTone.Neutral)
        )
        TradeTab.Sale -> remoteSaleOrders?.map { it.toTradeItem(isSeller = true) }
            ?: if (remoteLoading || remoteError != null) emptyList() else listOf(
            TradeItem("경매 진행 중", "빈티지 스니커즈", "현재가 58,000원 · 입찰 12명", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("발송 필요", "빈티지 필름 카메라", "구매자 결제 완료 · 1일 남음", "배송 정보 입력하기 →", TradeTone.Urgent),
            TradeItem("판매 완료", "원목 라운지 체어", "구매 확정 · 정산 예정", "거래 내역 보기 →", TradeTone.Neutral)
        )
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Surface, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            Column(Modifier.background(Color.White)) {
                Text("내 거래", Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp, vertical = 13.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(45.dp).padding(horizontal = 16.dp)) {
                    TradeTab.entries.forEach { tab ->
                        Column(Modifier.weight(1f).fillMaxHeight().clickable { selected = tab }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(tab.label, color = if (tab == selected) Colors.Navy else Colors.Muted, fontSize = 14.sp, fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.height(9.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (tab == selected) Colors.Navy else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFE7E9ED))
            }
        },
        bottomBar = { DibBottomNavigation(DibMainTab.Trades, onTabSelected) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("${selected.label} 현황", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("${items.size}건", color = Colors.Muted, fontSize = 11.sp) }
                    DibViewModeToggle(contentView, { contentView = it })
                }
            }
            if (selected != TradeTab.Bid && remoteLoading) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Colors.Navy)
                    }
                }
            } else if (selected != TradeTab.Bid && remoteError != null) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(remoteError, color = Colors.Muted, fontSize = 12.sp)
                        OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
                    }
                }
            } else if (items.isEmpty()) {
                item {
                    Text(
                        "아직 ${selected.label} 거래가 없어요.",
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        color = Colors.Muted,
                        fontSize = 13.sp
                    )
                }
            }
            if (!remoteLoading && remoteError == null && contentView == DibContentView.List) {
                items(items.size) { index ->
                    TradeCard(items[index]) { openTradeItem(selected, items[index], onProductClick, onTransactionClick) }
                }
            } else if (!remoteLoading && remoteError == null) {
                items(items.chunked(2).size) { rowIndex ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val row = items.chunked(2)[rowIndex]
                        row.forEach { item ->
                            TradeGridCard(item, Modifier.weight(1f)) { openTradeItem(selected, item, onProductClick, onTransactionClick) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun OrderSummary.toTradeItem(isSeller: Boolean): TradeItem {
    val normalized = status.uppercase()
    val statusLabel = when (normalized) {
        "PENDING" -> if (isSeller) "결제 대기" else "결제 필요"
        "PAID", "PREPARING" -> if (isSeller) "발송 필요" else "발송 준비 중"
        "SHIPPED" -> "배송 중"
        "DELIEVERED", "DELIVERED" -> "배송 완료"
        "CONFIRMED" -> if (isSeller) "판매 완료" else "구매 완료"
        "CANCELLED" -> "거래 취소"
        "REFUNDED" -> "환불 완료"
        else -> normalized.ifBlank { "거래 진행 중" }
    }
    val tone = when (normalized) {
        "PENDING", "PAID", "DELIEVERED", "DELIVERED" -> TradeTone.Urgent
        "PREPARING", "SHIPPED" -> TradeTone.Positive
        else -> TradeTone.Neutral
    }
    val price = if (finalPrice > 0) "낙찰가 ${"%,d".format(finalPrice)}원" else "결제 금액 확인 중"
    return TradeItem(
        status = statusLabel,
        title = title,
        meta = listOfNotNull(price, updatedAt?.take(10)).joinToString(" · "),
        action = "거래 상세 보기 →",
        tone = tone,
        orderId = orderId
    )
}

private fun openTradeItem(
    selected: TradeTab,
    item: TradeItem,
    onProductClick: (String) -> Unit,
    onTransactionClick: (role: String, orderId: String) -> Unit
) {
    if (selected == TradeTab.Bid) {
        onProductClick(if (item.status == "경매 종료") "lost" else if (item.title.contains("카메라")) "camera" else "sneakers")
    } else {
        onTransactionClick(if (selected == TradeTab.Sale) "seller" else "buyer", item.orderId)
    }
}

@Composable private fun TradeCard(item: TradeItem, onClick: () -> Unit) {
    val chip = when(item.tone){ TradeTone.Urgent -> Color(0xFFFFF0EA); TradeTone.Positive -> Color(0xFFE8FAF5); TradeTone.Neutral -> Color(0xFFF1F3F5) }
    val ink = when(item.tone){ TradeTone.Urgent -> Color(0xFFE56F49); TradeTone.Positive -> Color(0xFF27806E); TradeTone.Neutral -> Color(0xFF6B7280) }
    Row(Modifier.fillMaxWidth().height(136.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(88.dp).background(Color(0xFFECECEC), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("상품 이미지", color = Color(0xFF858B94), fontSize = 10.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(color = chip, shape = RoundedCornerShape(12.dp)) { Text(item.status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(item.meta, color = Color(0xFF6B7280), fontSize = 11.sp)
            Text(item.action, color = if(item.tone == TradeTone.Urgent) ink else Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TradeGridCard(item: TradeItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val chip = when(item.tone){ TradeTone.Urgent -> Colors.UrgentBackground; TradeTone.Positive -> Color(0xFFE8FAF5); TradeTone.Neutral -> Color(0xFFF1F3F5) }
    val ink = when(item.tone){ TradeTone.Urgent -> Colors.Urgent; TradeTone.Positive -> Colors.MintInk; TradeTone.Neutral -> Colors.Muted }
    Column(
        modifier.height(232.dp).background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(92.dp).background(Colors.Image, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("상품 이미지", color = Colors.Muted, fontSize = 10.sp)
        }
        Surface(color = chip, shape = RoundedCornerShape(10.dp)) { Text(item.status, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = ink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Text(item.title, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(item.meta, maxLines = 2, color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
        Text(item.action, maxLines = 1, color = if(item.tone == TradeTone.Urgent) ink else Colors.Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MyPageScreen(
    onTabSelected: (DibMainTab) -> Unit,
    onProfileEditClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onRegisteredProductsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onInquiriesClick: () -> Unit,
    onAddressesClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onWithdrawalClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F8FA), contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Text("마이", Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp, vertical = 13.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE1E5EA), RoundedCornerShape(14.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(Color(0xFFDDF8F0), CircleShape), contentAlignment = Alignment.Center) { Text("d", color = Colors.Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) { Text("dib러버", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("@dib_user", color = Colors.Muted, fontSize = 12.sp); Text("팔로워 128 · 팔로잉 24", color = Color(0xFF27806E), fontSize = 11.sp) }
                        OutlinedButton(onProfileEditClick, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("프로필 수정", fontSize = 11.sp) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Colors.Border)
                    Text("★ 4.8  ·  거래 32회  ·  응답 빠름", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Text("바로가기", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("♡" to "찜한 상품", "▣" to "등록 상품", "♧" to "알림", "◉" to "문의 내역").forEach { (icon,label) ->
                        Column(Modifier.width(76.dp).clickable { when(label) { "찜한 상품" -> onFavoritesClick(); "등록 상품" -> onRegisteredProductsClick(); "알림" -> onNotificationsClick(); else -> onInquiriesClick() } }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(40.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text(icon, color = Colors.Navy, fontSize = 22.sp) }; Text(label, Modifier.padding(top = 5.dp), color = Colors.Muted, fontSize = 11.sp) }
                    }
                }
            }
            item { Text("내 정보 · 설정", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE1E5EA), RoundedCornerShape(14.dp))) {
                    MenuRow("배송지 관리", onClick = onAddressesClick)
                    MenuRow("정산 계좌 관리", onClick = onAccountsClick)
                    MenuRow("알림 설정", onClick = onNotificationSettingsClick)
                    MenuRow("신고 내역", onClick = onReportsClick)
                    MenuRow("회원 탈퇴", onClick = onWithdrawalClick)
                    MenuRow("로그아웃", Color(0xFFEF596B)) { confirmation = "로그아웃" }
                }
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("$action 할까요?") },
            text = { Text("현재 계정에서 로그아웃하고 시작 화면으로 이동해요.") },
            confirmButton = { TextButton({ confirmation = null; onLogout() }) { Text("로그아웃") } },
            dismissButton = { TextButton({ confirmation = null }) { Text("취소") } }
        )
    }
}

@Composable private fun MenuRow(label: String, color: Color = Colors.Text, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = color, fontSize = 14.sp); Text("›", color = Colors.Muted, fontSize = 20.sp) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductRegisterScreen(
    categories: List<ProductCategory>,
    categoriesLoading: Boolean,
    categoriesError: String?,
    submitLoading: Boolean,
    submitError: String?,
    result: ProductRegistrationResult?,
    onRetryCategories: () -> Unit,
    onSubmit: (ProductRegistrationForm) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    val photoUris = remember { mutableStateListOf<Uri>() }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryDialog by rememberSaveable { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.categoryId == categoryId }
    val formValid = photoUris.isNotEmpty() && name.isNotBlank() && categoryId.isNotBlank() && condition.isNotBlank() && description.isNotBlank()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)) { uris ->
        photoUris.clear()
        photoUris.addAll(uris.take(10))
    }

    if (result != null) {
        Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Surface) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✓", color = Colors.MintInk, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("상품 검수를 요청했어요", Modifier.padding(top = 16.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("승인되면 경매를 등록할 수 있어요.\n상품 번호 ${result.productId}", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("확인", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Surface, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(48.dp).clickable { if(step > 1) step-- else onBack() }.wrapContentSize(), fontSize = 24.sp); Text("상품 등록", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if(step == 1) "상품 정보" else "등록 확인", fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("$step / 2", color = Colors.Muted, fontSize = 12.sp) }; LinearProgressIndicator({ step / 2f }, Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp), color = Colors.Navy, trackColor = Colors.Border) }
            when(step) {
                1 -> {
                    item { Text("상품 사진 *  1~10장 · 첫 사진이 대표", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    item { Box(Modifier.fillMaxWidth().height(88.dp).background(Colors.Surface, RoundedCornerShape(12.dp)).border(1.dp, Colors.Border, RoundedCornerShape(12.dp)).clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("+", color = Colors.Navy, fontSize = 26.sp); Text(if (photoUris.isEmpty()) "사진 선택" else "사진 다시 선택 (${photoUris.size}/10)", fontSize = 14.sp, fontWeight = FontWeight.Bold) } } }
                    if (photoUris.isNotEmpty()) item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoUris.size) { index -> ProductImageThumbnail(photoUris[index], index == 0) }
                        }
                    }
                    item { RegisterTextField("상품명 *", name, { name = it }, "입력해주세요") }
                    item { RegisterSelect("카테고리 *", selectedCategory?.name ?: "선택해주세요") { categoryDialog = true } }
                    if (categoriesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Navy) }
                    categoriesError?.let { message -> item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp); TextButton(onRetryCategories) { Text("재시도") } } } }
                    item { RegisterSelect("상품 상태 *", conditionLabel(condition)) { condition = when(condition){"GOOD"->"NORMAL";"NORMAL"->"BAD";else->"GOOD"} } }
                    item { RegisterTextField("상품 설명 *", description, { description = it }, "상품의 특징과 하자를 자세히 적어주세요", 100.dp) }
                }
                else -> {
                    item { Text("등록 내용을 확인해주세요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("${selectedCategory?.name} · ${conditionLabel(condition)}", color = Colors.Muted); Text("사진 ${photoUris.size}장 · 첫 사진이 대표", color = Colors.Navy, fontWeight = FontWeight.Bold); Text(description, color = Colors.Muted, fontSize = 12.sp) } }
                    item { Text("AI 상품 검수 요청 후 승인되면 경매를 시작할 수 있어요.", Modifier.fillMaxWidth().background(Color(0xFFFFF0EA), RoundedCornerShape(12.dp)).padding(16.dp), color = Color(0xFFE56F49), fontSize = 12.sp) }
                    submitError?.let { message -> item { Text(message, color = Colors.Urgent, fontSize = 12.sp) } }
                }
            }
            item { Button(onClick = { if(step == 1) step = 2 else onSubmit(ProductRegistrationForm(name.trim(), description.trim(), categoryId, condition, photoUris.toList())) }, enabled = formValid && !submitLoading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (submitLoading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp) else Text(if(step == 1) "등록 내용 확인" else "AI 검수 요청", fontWeight = FontWeight.Bold) } }
        }
    }
    if (categoryDialog) AlertDialog(
        onDismissRequest = { categoryDialog = false },
        title = { Text("카테고리 선택") },
        text = { LazyColumn { items(categories.size) { index -> val category = categories[index]; Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; categoryDialog = false }.padding(vertical = 14.dp), color = Colors.Navy) } } },
        confirmButton = { TextButton({ categoryDialog = false }) { Text("닫기") } }
    )
}

data class ProductRegistrationForm(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val imageUris: List<Uri>
)

@Composable
private fun ProductImageThumbnail(uri: Uri, representative: Boolean) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap()
        }
    }
    Box(Modifier.size(86.dp).background(Colors.Image, RoundedCornerShape(10.dp))) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (representative) Surface(Modifier.align(Alignment.TopStart).padding(5.dp), color = Colors.Navy, shape = RoundedCornerShape(8.dp)) { Text("대표", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp) }
    }
}

private fun conditionLabel(condition: String) = when (condition) {
    "GOOD" -> "상 · 사용감 적음"
    "NORMAL" -> "중 · 일반 사용감"
    "BAD" -> "하 · 하자 있음"
    else -> "상 · 중 · 하"
}

@Composable private fun RegisterTextField(label:String,value:String,onChange:(String)->Unit,placeholder:String,height: androidx.compose.ui.unit.Dp = 72.dp,keyboardType: KeyboardType = KeyboardType.Text){ Column(Modifier.fillMaxWidth().height(height), verticalArrangement=Arrangement.spacedBy(6.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold); OutlinedTextField(value,onChange,Modifier.fillMaxWidth().weight(1f),placeholder={Text(placeholder,color=Color(0xFF8A9099),fontSize=13.sp)},singleLine=height<90.dp,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Color(0xFFDDE1E7)))} }
@Composable private fun RegisterSelect(label:String,value:String,onClick:()->Unit){Column(Modifier.fillMaxWidth().height(72.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth().weight(1f).background(Color.White,RoundedCornerShape(12.dp)).border(1.dp,Color(0xFFDDE1E7),RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text(value,Modifier.weight(1f),color=if(value.contains("선택")||value.contains("상 ·"))Color(0xFF8A9099)else Colors.Text,fontSize=14.sp);Text("›",color=Colors.Muted,fontSize=22.sp)}}}
