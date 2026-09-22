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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.core.time.formatServerTime
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.auction.BidHistoryItem
import com.ssafy.dib.domain.auction.SaleHistoryItem
import com.ssafy.dib.domain.member.MemberProfile
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
    val orderId: String = "sample",
    val auctionId: String = "sample",
    val thumbnailUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTradesScreen(
    onTabSelected: (DibMainTab) -> Unit,
    onProductClick: (String) -> Unit,
    onTransactionClick: (role: String, orderId: String) -> Unit,
    remotePurchaseOrders: List<OrderSummary>?,
    remoteSaleOrders: List<SaleHistoryItem>?,
    remoteBids: List<BidHistoryItem>?,
    showSampleContent: Boolean,
    remoteLoading: Boolean,
    remoteError: String?,
    bidsLoading: Boolean,
    bidsError: String?,
    bidsHasNext: Boolean,
    bidsLoadingMore: Boolean,
    bidsLoadMoreError: String?,
    purchaseHasNext: Boolean,
    saleHasNext: Boolean,
    loadingMoreRole: OrderRole?,
    purchaseLoadMoreError: String?,
    saleLoadMoreError: String?,
    onRetry: () -> Unit,
    onBidsRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMoreBids: () -> Unit,
    onLoadMoreOrders: (OrderRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(TradeTab.Bid) }
    var contentView by rememberSaveable { mutableStateOf(DibContentView.List) }
    val items = when (selected) {
        TradeTab.Bid -> remoteBids?.filterNot { bid ->
            remotePurchaseOrders.orEmpty().any { order -> order.auctionId == bid.auctionId }
        }?.map(BidHistoryItem::toTradeItem)
            ?: if (!showSampleContent || bidsLoading || bidsError != null) emptyList() else listOf(
            TradeItem("다른 입찰 발생", "빈티지 필름 카메라", "현재가 35,000원 · 마감 00:42", "현재가보다 높게 입찰하기 →", TradeTone.Urgent),
            TradeItem("최고 입찰자", "빈티지 스니커즈", "내 입찰가 58,000원 · 마감 12분", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("경매 종료", "레더 숄더백", "최종가 72,000원 · 미낙찰", "결과 확인하기 →", TradeTone.Neutral)
        )
        TradeTab.Purchase -> remotePurchaseOrders?.map { it.toTradeItem(isSeller = false) }
            ?: if (!showSampleContent || remoteLoading || remoteError != null) emptyList() else listOf(
            TradeItem("결제 필요", "빈티지 필름 카메라", "낙찰가 35,000원 · 23:42:18 남음", "거래 진행하기 →", TradeTone.Urgent),
            TradeItem("배송 중", "노이즈 캔슬링 헤드폰", "판매자가 상품을 발송했어요", "배송 조회하기 →", TradeTone.Positive),
            TradeItem("구매 완료", "레더 카드지갑", "거래가 안전하게 완료됐어요", "거래 내역 보기 →", TradeTone.Neutral)
        )
        TradeTab.Sale -> remoteSaleOrders?.map(SaleHistoryItem::toTradeItem)
            ?: if (!showSampleContent || remoteLoading || remoteError != null) emptyList() else listOf(
            TradeItem("경매 진행 중", "빈티지 스니커즈", "현재가 58,000원 · 입찰 12회", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("발송 필요", "빈티지 필름 카메라", "구매자 결제 완료 · 1일 남음", "배송 정보 입력하기 →", TradeTone.Urgent),
            TradeItem("판매 완료", "원목 라운지 체어", "구매 확정 · 정산 예정", "거래 내역 보기 →", TradeTone.Neutral)
        )
    }
    val selectedLoading = if (selected == TradeTab.Bid) bidsLoading || (remotePurchaseOrders == null && remoteLoading) else remoteLoading
    val selectedError = if (selected == TradeTab.Bid) bidsError else remoteError
    val selectedOrderRole = when (selected) {
        TradeTab.Purchase -> OrderRole.BUYER
        TradeTab.Sale -> OrderRole.SELLER
        TradeTab.Bid -> null
    }
    val selectedHasNext = when (selected) {
        TradeTab.Purchase -> purchaseHasNext
        TradeTab.Sale -> saleHasNext
        TradeTab.Bid -> false
    }
    val selectedLoadMoreError = when (selected) {
        TradeTab.Purchase -> purchaseLoadMoreError
        TradeTab.Sale -> saleLoadMoreError
        TradeTab.Bid -> null
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            Column(Modifier.background(Color.White)) {
                Text("내 거래", Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 18.dp, vertical = 15.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
        PullToRefreshBox(isRefreshing = remoteLoading || bidsLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.label} 현황 · ${items.size}건", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    DibViewModeToggle(contentView, { contentView = it })
                }
            }
            if (selectedLoading) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Colors.Navy)
                    }
                }
            } else if (selectedError != null) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(selectedError, color = Colors.Muted, fontSize = 12.sp)
                        OutlinedButton(onClick = if (selected == TradeTab.Bid) onBidsRetry else onRetry) { Text("다시 불러오기") }
                    }
                }
            } else if (items.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(18.dp)).padding(vertical = 42.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(48.dp).background(Colors.NavySoft, CircleShape), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.product_outline), null, Modifier.size(23.dp), colorFilter = ColorFilter.tint(Colors.Navy))
                        }
                        Text("아직 ${selected.label} 거래가 없어요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("경매에 참여하면 진행 상태를 여기서 확인할 수 있어요", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            }
            if (!selectedLoading && selectedError == null && contentView == DibContentView.List) {
                items(items.size) { index ->
                    TradeCard(items[index]) { openTradeItem(selected, items[index], onProductClick, onTransactionClick) }
                }
            } else if (!selectedLoading && selectedError == null) {
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
            if (!selectedLoading && selectedError == null && selectedOrderRole != null && (selectedHasNext || loadingMoreRole == selectedOrderRole || selectedLoadMoreError != null)) {
                item(key = "load-more-${selectedOrderRole.name}") {
                    LaunchedEffect(selectedOrderRole, items.size, selectedHasNext, loadingMoreRole, selectedLoadMoreError) {
                        if (selectedHasNext && loadingMoreRole == null && selectedLoadMoreError == null) onLoadMoreOrders(selectedOrderRole)
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            loadingMoreRole == selectedOrderRole -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            selectedLoadMoreError != null -> {
                                Text(selectedLoadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                TextButton(onClick = { onLoadMoreOrders(selectedOrderRole) }) { Text("더 불러오기") }
                            }
                        }
                    }
                }
            }
            if (!selectedLoading && selectedError == null && selected == TradeTab.Bid && (bidsHasNext || bidsLoadingMore || bidsLoadMoreError != null)) {
                item(key = "load-more-bids") {
                    LaunchedEffect(items.size, bidsHasNext, bidsLoadingMore, bidsLoadMoreError) {
                        if (bidsHasNext && !bidsLoadingMore && bidsLoadMoreError == null) onLoadMoreBids()
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            bidsLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            bidsLoadMoreError != null -> {
                                Text(bidsLoadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                TextButton(onClick = onLoadMoreBids) { Text("더 불러오기") }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private fun BidHistoryItem.toTradeItem() = TradeItem(
    status = "입찰 참여",
    title = "경매 #$auctionId",
    meta = "내 입찰가 ${"%,d".format(amount)}원 · ${formatServerTime(createdAt) ?: createdAt.take(16).replace('T', ' ')}",
    action = "경매 상태 보기 →",
    tone = TradeTone.Positive,
    auctionId = auctionId
)

private fun OrderSummary.toTradeItem(isSeller: Boolean): TradeItem {
    val normalized = status.uppercase()
    val statusLabel = when (normalized) {
        "PENDING" -> if (isSeller) "결제 대기" else "결제 필요"
        "PAID", "PREPARING" -> if (isSeller) "발송 필요" else "발송 준비 중"
        "SHIPPED" -> "배송 중"
        "DELIEVERED", "DELIVERED" -> "배송 완료"
        "CONFIRMED" -> if (isSeller) "판매 완료" else "구매 완료"
        "CANCELLED", "CANCELED" -> "거래 취소"
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
        meta = listOfNotNull(price, updatedAt?.let { formatServerTime(it, "yyyy.MM.dd") ?: it.take(10) }).joinToString(" · "),
        action = "거래 상세 보기 →",
        tone = tone,
        orderId = orderId,
        thumbnailUrl = thumbnailUrl
    )
}

private fun SaleHistoryItem.toTradeItem(): TradeItem {
    val normalizedOrderStatus = orderStatus?.uppercase()
    val normalizedAuctionStatus = auction.status.uppercase()
    val statusLabel = when (normalizedOrderStatus) {
        "PENDING" -> "결제 대기"
        "PAID", "PREPARING" -> "발송 필요"
        "SHIPPED" -> "배송 중"
        "DELIEVERED", "DELIVERED" -> "배송 완료"
        "CONFIRMED" -> "판매 완료"
        "CANCELLED", "CANCELED" -> "거래 취소"
        "REFUNDED" -> "환불 완료"
        else -> when (normalizedAuctionStatus) {
            "PENDING" -> "검수 대기"
            "SCHEDULED" -> "경매 예정"
            "ACTIVE" -> "경매 진행 중"
            "ENDED" -> "경매 종료"
            "CANCELLED", "CANCELED" -> "경매 취소"
            else -> normalizedAuctionStatus.ifBlank { "판매 경매" }
        }
    }
    val tone = when {
        normalizedOrderStatus in setOf("PENDING", "PAID", "DELIEVERED", "DELIVERED") -> TradeTone.Urgent
        normalizedOrderStatus in setOf("PREPARING", "SHIPPED") || normalizedAuctionStatus == "ACTIVE" -> TradeTone.Positive
        else -> TradeTone.Neutral
    }
    val priceLabel = if (auction.currentPrice > 0) "현재가 ${"%,d".format(auction.currentPrice)}원" else "시작가 ${"%,d".format(auction.startPrice)}원"
    return TradeItem(
        status = statusLabel,
        title = auction.title,
        meta = "$priceLabel · 입찰 ${auction.bidCount}회",
        action = if (orderId.isNullOrBlank()) "경매 상태 보기 →" else "거래 상세 보기 →",
        tone = tone,
        orderId = orderId.orEmpty(),
        auctionId = auction.auctionId
    )
}

private fun openTradeItem(
    selected: TradeTab,
    item: TradeItem,
    onProductClick: (String) -> Unit,
    onTransactionClick: (role: String, orderId: String) -> Unit
) {
    if (selected == TradeTab.Bid || (selected == TradeTab.Sale && item.orderId.isBlank())) {
        onProductClick(item.auctionId.takeIf { it != "sample" } ?: if (item.status == "경매 종료") "lost" else if (item.title.contains("카메라")) "camera" else "sneakers")
    } else {
        onTransactionClick(if (selected == TradeTab.Sale) "seller" else "buyer", item.orderId)
    }
}

@Composable private fun TradeCard(item: TradeItem, onClick: () -> Unit) {
    val chip = when(item.tone){ TradeTone.Urgent -> Color(0xFFFFF0EA); TradeTone.Positive -> Color(0xFFE8FAF5); TradeTone.Neutral -> Color(0xFFF1F3F5) }
    val ink = when(item.tone){ TradeTone.Urgent -> Color(0xFFE56F49); TradeTone.Positive -> Color(0xFF27806E); TradeTone.Neutral -> Color(0xFF6B7280) }
    Row(Modifier.fillMaxWidth().heightIn(min = 148.dp).background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Colors.Border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (item.thumbnailUrl.isNullOrBlank()) {
            Box(Modifier.size(86.dp).background(Colors.NavySoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.product_outline), null, Modifier.size(30.dp), colorFilter = ColorFilter.tint(Colors.Navy.copy(alpha = .55f)))
            }
        } else {
            DibNetworkImage(item.thumbnailUrl, item.title, Modifier.size(86.dp), placeholderText = item.title.take(1))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = chip, shape = RoundedCornerShape(12.dp)) { Text(item.status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = ink, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold) }
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text(item.meta, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF6B7280), fontSize = 11.sp, lineHeight = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.action.removeSuffix(" →"), Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, color = if(item.tone == TradeTone.Urgent) ink else Colors.Navy, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                Image(painterResource(R.drawable.chevron_right), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(if(item.tone == TradeTone.Urgent) ink else Colors.Navy))
            }
        }
    }
}

@Composable
private fun TradeGridCard(item: TradeItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val chip = when(item.tone){ TradeTone.Urgent -> Colors.UrgentBackground; TradeTone.Positive -> Color(0xFFE8FAF5); TradeTone.Neutral -> Color(0xFFF1F3F5) }
    val ink = when(item.tone){ TradeTone.Urgent -> Colors.Urgent; TradeTone.Positive -> Colors.MintInk; TradeTone.Neutral -> Colors.Muted }
    Column(
        modifier.heightIn(min = 232.dp).background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (item.thumbnailUrl.isNullOrBlank()) {
            Box(Modifier.fillMaxWidth().height(92.dp).background(Colors.NavySoft, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.product_outline), null, Modifier.size(29.dp), colorFilter = ColorFilter.tint(Colors.Navy.copy(alpha = .55f)))
            }
        } else {
            DibNetworkImage(item.thumbnailUrl, item.title, Modifier.fillMaxWidth().height(92.dp), placeholderText = item.title.take(1))
        }
        Surface(color = chip, shape = RoundedCornerShape(10.dp)) { Text(item.status, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = ink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        Text(item.title, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(item.meta, maxLines = 2, color = Colors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.action.removeSuffix(" →"), maxLines = 1, color = if(item.tone == TradeTone.Urgent) ink else Colors.Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Image(painterResource(R.drawable.chevron_right), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(if(item.tone == TradeTone.Urgent) ink else Colors.Navy))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    profile: MemberProfile?,
    profileLoading: Boolean,
    profileError: String?,
    onRetryProfile: () -> Unit,
    onRefresh: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    onProfileEditClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onRegisteredProductsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onInquiriesClick: () -> Unit,
    onAddressesClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onSettlementsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onWithdrawalClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Text("마이", Modifier.fillMaxWidth().height(60.dp).background(Color.White).padding(horizontal = 18.dp, vertical = 15.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }
    ) { padding ->
        PullToRefreshBox(isRefreshing = profileLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Colors.Border, RoundedCornerShape(20.dp)).padding(18.dp)) {
                    if (profileLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Colors.Mint)
                    profileError?.let { message ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                            TextButton(onRetryProfile) { Text("재시도") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(58.dp).background(Colors.MintSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Text(profile?.nickname?.take(1)?.uppercase() ?: "?", color = Colors.Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(profile?.nickname ?: "내 프로필", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(profile?.email ?: "내 계정 정보를 확인해보세요", color = Colors.Muted, fontSize = 12.sp)
                            profile?.status?.let { status -> Text(memberStatusLabel(status), color = Color(0xFF27806E), fontSize = 11.sp) }
                        }
                        OutlinedButton(onProfileEditClick, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("프로필 수정", fontSize = 11.sp) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Colors.Border)
                    Text(
                        profile?.let { p -> p.score?.let { "받은 평점 ★ %.1f".format(it) } ?: "아직 받은 평점이 없어요" }
                            ?: "평점을 불러오는 중",
                        color = Colors.Navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item { Text("바로가기", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Colors.Border, RoundedCornerShape(18.dp)).padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf(
                        Triple(R.drawable.favorite_outline, "찜한 경매", onFavoritesClick),
                        Triple(R.drawable.nav_register_full, "등록 상품", onRegisteredProductsClick),
                        Triple(R.drawable.notification_vector, "알림", onNotificationsClick),
                        Triple(R.drawable.nav_feed_full, "문의 내역", onInquiriesClick)
                    ).forEach { (icon, label, action) ->
                        Column(Modifier.width(76.dp).clickable(onClick = action), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(42.dp).background(Colors.NavySoft, CircleShape), contentAlignment = Alignment.Center) {
                                Image(painterResource(icon), label, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Colors.Navy))
                            }
                            Text(label, Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            item { Text("내 정보 · 설정", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Colors.Border, RoundedCornerShape(18.dp))) {
                    MenuRow("배송지 관리", onClick = onAddressesClick)
                    MenuRow("결제수단 관리", onClick = onPaymentMethodsClick)
                    MenuRow("정산 계좌 관리", onClick = onAccountsClick)
                    MenuRow("정산 내역", onClick = onSettlementsClick)
                    MenuRow("알림 설정", onClick = onNotificationSettingsClick)
                    MenuRow("신고 내역", onClick = onReportsClick)
                    MenuRow("회원 탈퇴", onClick = onWithdrawalClick)
                    MenuRow("로그아웃", Color(0xFFEF596B)) { confirmation = "로그아웃" }
                }
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

private fun memberStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "정상 이용 중"
    "SUSPENDED" -> "이용 정지"
    "WITHDRAWN" -> "탈퇴 처리 중"
    else -> status
}

@Composable private fun MenuRow(label: String, color: Color = Colors.Text, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = color, fontSize = 14.sp); Image(painterResource(R.drawable.chevron_right),null,Modifier.size(18.dp),colorFilter=ColorFilter.tint(Colors.Muted)) } }

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
    modifier: Modifier = Modifier,
    latestStatus: String? = null,
    statusRefreshing: Boolean = false,
    onRefreshStatus: (() -> Unit)? = null
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    val photoUris = remember { mutableStateListOf<Uri>() }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var modelName by rememberSaveable { mutableStateOf("") }
    var releaseYear by rememberSaveable { mutableStateOf("") }
    var categoryDialog by rememberSaveable { mutableStateOf(false) }
    var conditionDialog by rememberSaveable { mutableStateOf(false) }
    var imageValidationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showPhotoReorder by remember { mutableStateOf(false) }
    var validationRequested by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedCategory = categories.firstOrNull { it.categoryId == categoryId }
    val formValid = photoUris.isNotEmpty() && name.isNotBlank() && categoryId.isNotBlank() && condition.isNotBlank() && description.isNotBlank()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)) { uris ->
        val remaining = (10 - photoUris.size).coerceAtLeast(0)
        val selectedUris = uris.filterNot { photoUris.contains(it) }.take(remaining)
        val validationMessage = when {
            uris.isEmpty() -> null
            remaining == 0 -> "\uc0c1\ud488 \uc0ac\uc9c4\uc740 \ucd5c\ub300 10\uc7a5\uae4c\uc9c0 \ub4f1\ub85d\ud560 \uc218 \uc788\uc5b4\uc694."
            else -> productImageValidationMessage(context.contentResolver, selectedUris)
        }
        if (validationMessage != null) {
            imageValidationMessage = validationMessage
        } else {
            photoUris.addAll(selectedUris)
        }
    }

    if (showPhotoReorder) {
        ProductPhotoReorderScreen(
            images = photoUris.toList(),
            onSave = { reorderedImages ->
                photoUris.clear()
                photoUris.addAll(reorderedImages)
                showPhotoReorder = false
            },
            onBack = { showPhotoReorder = false },
            modifier = modifier
        )
        return
    }

    if (result != null) {
        Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(72.dp).background(Colors.MintSoft, CircleShape), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.check_circle), null, Modifier.size(40.dp), colorFilter = ColorFilter.tint(Colors.MintInk)) }
                // 등록 응답은 PENDING 이고 경매도 아직 없다. 검수가 끝나야 경매를 시작할 수 있다
                val status = (latestStatus ?: result.status).uppercase()
                val approved = status in setOf("REGISTERED", "APPROVED")
                val rejected = status in setOf("REJECTED", "REVIEW_REJECTED")
                Text(
                    if (approved) "상품 등록이 완료됐어요" else "상품을 접수했어요",
                    Modifier.padding(top = 20.dp),
                    color = Colors.Text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        result.productId.startsWith("PREVIEW-") -> "개발 미리보기 상품으로 등록됐어요."
                        approved -> "검수가 끝났어요. 지금 경매를 시작할 수 있어요. · 상품 번호 ${result.productId}"
                        rejected -> "등록이 거절됐어요. 등록 상품 관리에서 사유를 확인하고 수정해주세요. · 상품 번호 ${result.productId}"
                        else -> "검수 중이에요. 승인되면 경매를 시작할 수 있어요.\n결과 알림이 따로 없어서 아래 새로고침으로 확인해주세요. · 상품 번호 ${result.productId}"
                    },
                    Modifier.padding(top = 10.dp),
                    color = Colors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                if (onRefreshStatus != null && !result.productId.startsWith("PREVIEW-")) {
                    OutlinedButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(48.dp),
                        enabled = !statusRefreshing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (statusRefreshing) CircularProgressIndicator(Modifier.size(20.dp), color = Colors.Navy, strokeWidth = 2.dp)
                        else Text("검수 상태 새로고침", color = Colors.Navy, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if (approved) "경매 시작 준비" else "등록 상품 관리로 이동", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Column(Modifier.background(Colors.Background)) { Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { if(step > 1) step-- else onBack() }) { Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter=ColorFilter.tint(Colors.Text)) }; Text("상품 등록", color=Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }; HorizontalDivider(color=Colors.Border) } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if(step == 1) "상품 정보" else "등록 확인", color=Colors.Text,fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("$step / 2", color = Colors.Muted, fontSize = 12.sp) }; LinearProgressIndicator({ step / 2f }, Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp), color = Colors.Navy, trackColor = Colors.Border) }
            when(step) {
                1 -> {
                    item { Text("상품 사진 *  최대 10장 · 첫 번째 사진이 대표 이미지\n$PRODUCT_IMAGE_POLICY_LABEL", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    item {
                        val photoError = validationRequested && photoUris.isEmpty()
                        val photoFull = photoUris.size >= 10
                        Box(
                            Modifier.fillMaxWidth()
                                .height(if (photoError) 112.dp else 88.dp)
                                .background(Colors.Background, RoundedCornerShape(16.dp))
                                .border(if (photoError) 2.dp else 1.dp, if (photoError) Colors.Urgent else Colors.Border, RoundedCornerShape(12.dp))
                                .clickable(enabled = !photoFull) { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!photoFull) Image(
                                    painterResource(R.drawable.add),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    colorFilter = ColorFilter.tint(if (photoError) Colors.Urgent else Colors.Navy)
                                )
                                Text(
                                    when {
                                        photoFull -> "최대 10장을 모두 등록했어요"
                                        photoError -> "사진을 1장 이상 등록해주세요"
                                        photoUris.isEmpty() -> "사진 추가"
                                        else -> "사진 추가 (${photoUris.size}/10)"
                                    },
                                    color = when {
                                        photoError -> Colors.Urgent
                                        photoFull -> Colors.Muted
                                        else -> Colors.Text
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (photoError) Text("첫 번째 사진이 대표 이미지 · 최대 10장", color = Colors.Muted, fontSize = 12.sp)
                                if (photoFull && !photoError) Text("사진을 바꾸려면 아래에서 먼저 삭제해주세요", color = Colors.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    if (photoUris.isNotEmpty()) item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(photoUris, key = { _, uri -> uri.toString() }) { index, uri ->
                                ProductImageThumbnail(
                                    uri = uri,
                                    representative = index == 0,
                                    onRemove = { photoUris.remove(uri) },
                                    onMakeRepresentative = if (index == 0) null else ({
                                        val current = photoUris.indexOf(uri)
                                        if (current > 0) moveProductImage(photoUris, current, 0)
                                    })
                                )
                            }
                        }
                    }
                    if (photoUris.size > 1) item {
                        OutlinedButton(
                            onClick = { showPhotoReorder = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("사진 순서 편집", fontWeight = FontWeight.Bold) }
                    }
                    item { RegisterTextField("상품명 *", name, { name = it }, "입력해주세요", errorMessage = "상품명을 입력해주세요".takeIf { validationRequested && name.isBlank() }) }
                    item { RegisterSelect("카테고리 *", selectedCategory?.name ?: "선택해주세요", placeholder = selectedCategory == null, errorMessage = "카테고리를 선택해주세요".takeIf { validationRequested && categoryId.isBlank() }) { categoryDialog = true } }
                    if (categoriesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colors.Navy) }
                    categoriesError?.let { message -> item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp); TextButton(onRetryCategories) { Text("재시도") } } } }
                    item { RegisterSelect("상품 상태 *", conditionLabel(condition), placeholder = condition.isBlank(), errorMessage = "상품 상태를 선택해주세요".takeIf { validationRequested && condition.isBlank() }) { conditionDialog = true } }
                    item { RegisterTextField("상품 설명 *", description, { description = it }, "상품의 특징과 하자를 자세히 적어주세요", 100.dp, errorMessage = "상품 설명을 입력해주세요".takeIf { validationRequested && description.isBlank() }) }
                    item { RegisterTextField("모델명 (선택)", modelName, { modelName = it }, "예: Galaxy S24") }
                    item { RegisterTextField("출시연도 (선택)", releaseYear, { releaseYear = it.filter(Char::isDigit).take(4) }, "예: 2024", keyboardType = KeyboardType.Number) }
                    item { Text("가격과 경매 시간은 경매를 시작할 때 정해요", Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Muted, fontSize = 12.sp) }
                }
                else -> {
                    item { Text("등록 내용을 확인해주세요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(name, color=Colors.Text,fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("${selectedCategory?.name} · ${conditionLabel(condition)}", color = Colors.Muted); Text("사진 ${photoUris.size}장 · 첫 번째 사진이 대표 이미지", color = Colors.Navy, fontWeight = FontWeight.Bold); modelName.takeIf(String::isNotBlank)?.let { Text("모델명 $it", color = Colors.Muted, fontSize = 12.sp) }; releaseYear.toIntOrNull()?.let { Text("출시연도 ${it}년", color = Colors.Muted, fontSize = 12.sp) }; Text("가격과 경매 시간은 경매를 시작할 때 정해요", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); HorizontalDivider(color=Colors.Border);Text(description, color = Colors.Muted, fontSize = 12.sp,lineHeight=18.sp) } }
                    item { Text("등록하면 AI 검수를 먼저 받아요. 승인되면 그때 경매가 만들어지고, 시작가와 경매 시간은 경매를 시작할 때 정할 수 있어요.", Modifier.fillMaxWidth().background(Color(0xFFFFF0EA), RoundedCornerShape(12.dp)).padding(16.dp), color = Color(0xFFE56F49), fontSize = 12.sp, lineHeight = 18.sp) }
                    submitError?.let { message -> item { Text(message, color = Colors.Urgent, fontSize = 12.sp) } }
                }
            }
            item {
                val canContinue = formValid
                Button(
                    onClick = {
                        if (step == 1) {
                            if (canContinue) step = 2 else validationRequested = true
                        } else {
                            onSubmit(ProductRegistrationForm(name.trim(), description.trim(), categoryId, condition, modelName.trim().ifBlank { null }, releaseYear.toIntOrNull(), photoUris.map { ProductImageSelection(it) }))
                        }
                    },
                    enabled = !submitLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (step == 1 && !canContinue) Color(0xFFE0E0E0) else Colors.Navy,
                        contentColor = if (step == 1 && !canContinue) Color(0xFF949494) else Color.White
                    )
                ) {
                    if (submitLoading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if(step == 1) "등록 내용 확인" else "상품 등록", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (categoryDialog) AlertDialog(
        onDismissRequest = { categoryDialog = false },
        title = { Text("카테고리 선택") },
        text = { LazyColumn { items(categories.size) { index -> val category = categories[index]; Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; categoryDialog = false }.padding(vertical = 14.dp), color = Colors.Navy) } } },
        confirmButton = { TextButton({ categoryDialog = false }) { Text("닫기") } }
    )
    if (conditionDialog) ProductConditionDialog(
        selected = condition,
        onSelect = { condition = it },
        onDismiss = { conditionDialog = false }
    )
    imageValidationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { imageValidationMessage = null },
            title = { Text("사진을 올릴 수 없어요") },
            text = { Text(message) },
            confirmButton = { TextButton({ imageValidationMessage = null }) { Text("확인") } }
        )
    }
}

@Composable
internal fun ProductPhotoReorderScreen(
    images: List<Uri>,
    onSave: (List<Uri>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderedImages = remember { mutableStateListOf<Uri>().apply { addAll(images) } }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.background(Colors.Background)) { Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick=onBack){Image(painterResource(R.drawable.back),"뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))};Text("사진 순서 편집", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) };HorizontalDivider(color=Colors.Border) }
        },
        bottomBar = {
            Button(
                onClick = { onSave(reorderedImages.toList()) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                shape = RoundedCornerShape(12.dp)
            ) { Text("순서 저장", fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start=18.dp,end=18.dp,top=20.dp,bottom=24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("사진을 길게 눌러 순서를 바꿔보세요", color = Colors.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("첫 번째 사진이 상품 목록과 경매의 썸네일로 사용됩니다", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 11.sp)
            }
            item {
                LazyRow(
                    Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(reorderedImages, key = { _, uri -> uri.toString() }) { index, uri ->
                        ReorderPhotoPreview(uri, index == 0, index + 1)
                    }
                }
            }
            itemsIndexed(reorderedImages, key = { _, uri -> uri.toString() }) { index, uri ->
                SortableProductPhotoRow(
                    uri = uri,
                    index = index,
                    count = reorderedImages.size,
                    onMove = { direction ->
                        val currentIndex = reorderedImages.indexOf(uri)
                        val targetIndex = (currentIndex + direction).coerceIn(0, reorderedImages.lastIndex)
                        moveProductImage(reorderedImages, currentIndex, targetIndex)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReorderPhotoPreview(uri: Uri, representative: Boolean, number: Int) {
    val bitmap = rememberProductBitmap(uri)
    Box(Modifier.size(76.dp).background(Colors.Image, RoundedCornerShape(8.dp)), contentAlignment = Alignment.BottomStart) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text(
            if (representative) "대표" else number.toString(),
            Modifier.padding(8.dp),
            color = if (representative) Color.White else Colors.Navy,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SortableProductPhotoRow(uri: Uri, index: Int, count: Int, onMove: (Int) -> Unit) {
    val bitmap = rememberProductBitmap(uri)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val threshold = with(density) { 36.dp.toPx() }
    var dragOffset by remember(uri) { mutableFloatStateOf(0f) }
    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = dragOffset }
            .pointerInput(uri, count) {
                detectDragGesturesAfterLongPress(
                    onDragEnd = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount.y
                    when {
                        dragOffset > threshold && index < count - 1 -> {
                            onMove(1)
                            dragOffset -= threshold * 2
                        }
                        dragOffset < -threshold && index > 0 -> {
                            onMove(-1)
                            dragOffset += threshold * 2
                        }
                    }
                }
            },
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Colors.Border)
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Colors.Image, RoundedCornerShape(8.dp))) {
                bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (index == 0) "대표 이미지" else "상품 이미지 ${index + 1}", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(if (index == 0) "첫 번째 사진" else "드래그하여 순서 변경", color = Colors.Muted, fontSize = 10.sp)
            }
            Image(painterResource(R.drawable.drag_handle),"순서 변경",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Muted))
        }
    }
}

@Composable
private fun rememberProductBitmap(uri: Uri): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap()
        }
    }
    return bitmap
}

data class ProductRegistrationForm(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val modelName: String?,
    val releaseYear: Int?,
    val images: List<ProductImageSelection>
)

@Composable
private fun ProductImageThumbnail(uri: Uri, representative: Boolean, onRemove: () -> Unit, onMakeRepresentative: (() -> Unit)?) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap()
        }
    }
    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(86.dp).background(Colors.Image, RoundedCornerShape(10.dp))) {
            bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (representative) Surface(Modifier.align(Alignment.TopStart).padding(5.dp), color = Colors.Navy, shape = RoundedCornerShape(8.dp)) { Text("대표", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp) }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clickable(onClick = onRemove),
                color = Color(0xCC1A1A1A),
                shape = CircleShape
            ) {
                Image(
                    painterResource(R.drawable.close),
                    contentDescription = "사진 삭제",
                    modifier = Modifier.padding(3.dp).size(14.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        Surface(
            modifier = Modifier.clickable(enabled = onMakeRepresentative != null) { onMakeRepresentative?.invoke() },
            color = if (representative) Colors.Navy else Color(0xFFF1F5FA),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(
                if (representative) "대표 이미지" else "대표로",
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (representative) Color.White else Colors.Navy,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 등록·수정 두 화면이 같은 값·라벨·선택 UI 를 쓰도록 여기 한 곳에만 둔다
internal val PRODUCT_CONDITIONS = listOf("GOOD", "NORMAL", "BAD")

internal fun conditionLabel(condition: String) = when (condition) {
    "GOOD" -> "상 · 사용감 적음"
    "NORMAL" -> "중 · 일반 사용감"
    "BAD" -> "하 · 하자 있음"
    else -> "상 · 중 · 하"
}

// 카테고리와 같은 방식으로 목록을 펼쳐 고른다. 예전엔 등록 화면이 탭마다 상→중→하로
// 순환하고 수정 화면은 칩 3개라, 같은 값을 고르는데 화면마다 방식이 달랐다
@Composable
internal fun ProductConditionDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("상품 상태 선택") },
        text = {
            Column {
                PRODUCT_CONDITIONS.forEach { value ->
                    Text(
                        conditionLabel(value),
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 14.dp),
                        color = Colors.Navy,
                        fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("닫기") } }
    )
}

@Composable
private fun RegisterTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    height: androidx.compose.ui.unit.Dp = 80.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null
) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = height + if (errorMessage != null) 18.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (height >= 100.dp) 76.dp else 56.dp),
            placeholder = { Text(errorMessage ?: placeholder, color = if (errorMessage != null) Colors.Urgent else Color(0xFF8A9099), fontSize = 13.sp) },
            singleLine = height < 90.dp,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Colors.Navy,
                unfocusedBorderColor = Color(0xFFDDE1E7),
                errorBorderColor = Colors.Urgent
            )
        )
        if (errorMessage != null) Text(errorMessage, color = Colors.Urgent, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
// placeholder 여부는 호출부가 상태값으로 판단해 넘긴다. 표시 문자열을 contains 로 추측하면
// 선택값 "상 · 사용감 적음" 이 안내문 "상 · 중 · 하" 와 같이 걸려 선택해도 회색으로 남았다
private fun RegisterSelect(label: String, value: String, placeholder: Boolean, errorMessage: String? = null, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = if (errorMessage == null) 80.dp else 98.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(Color.White, RoundedCornerShape(12.dp))
                .border(if (errorMessage != null) 1.5.dp else 1.dp, if (errorMessage != null) Colors.Urgent else Color(0xFFDDE1E7), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, Modifier.weight(1f), color = if (placeholder) Color(0xFF8A9099) else Colors.Text, fontSize = 14.sp)
            Image(painterResource(R.drawable.chevron_right),null,Modifier.size(18.dp),colorFilter=ColorFilter.tint(Colors.Muted))
        }
        errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp, lineHeight = 15.sp) }
    }
}
