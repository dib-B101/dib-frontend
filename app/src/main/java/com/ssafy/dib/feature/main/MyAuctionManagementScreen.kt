package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.domain.auction.SaleHistoryItem
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun MyAuctionManagementScreen(
    sales: List<SaleHistoryItem>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    selectedStatus: String?,
    actionAuctionId: String?,
    actionMessage: String?,
    actionError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onStatusSelected: (String?) -> Unit,
    onOpenAuction: (String) -> Unit,
    onUpdate: (String, Long, Long) -> Unit,
    onStart: (auctionId: String, startPrice: Long, auctionTimeSeconds: Long) -> Unit,
    onCancel: (String) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    onRelist: ((String) -> Unit)? = null,
    relistedAuctionId: String? = null,
    onRelistConsumed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var starting by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelling by rememberSaveable { mutableStateOf<String?>(null) }
    val items = sales.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MyAuctionHeader(onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }
    ) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            actionMessage?.let { message ->
                item { Text(message, Modifier.fillMaxWidth().background(Colors.Mint.copy(alpha = .18f), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.MintInk, fontSize = 12.sp) }
            }
            actionError?.let { message ->
                item { Text(message, Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.Urgent, fontSize = 12.sp) }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(auctionStatusFilters, key = { it.second ?: "ALL" }) { (label, status) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { onStatusSelected(status) },
                            label = { Text(label) },
                            enabled = !isLoading && !isLoadingMore && actionAuctionId == null
                        )
                    }
                }
            }
            when {
                isLoading -> item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null -> item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(errorMessage, color = Colors.Muted, fontSize = 12.sp)
                        OutlinedButton(onRetry) { Text("다시 불러오기") }
                    }
                }
                items.isEmpty() -> item { Text("등록한 경매가 없어요.", Modifier.fillMaxWidth().padding(vertical = 64.dp), color = Colors.Muted, fontSize = 14.sp) }
                else -> items(items, key = { it.auction.auctionId }) { sale ->
                    MyAuctionCard(
                        sale = sale,
                        actionLoading = actionAuctionId == sale.auction.auctionId,
                        actionsEnabled = actionAuctionId == null,
                        onOpen = { onOpenAuction(sale.auction.auctionId) },
                        onEdit = { editing = sale.auction.auctionId },
                        onStart = { starting = sale.auction.auctionId },
                        onCancel = { cancelling = sale.auction.auctionId },
                        onRelist = onRelist?.let { relist -> { relist(sale.auction.auctionId) } }
                    )
                }
            }
            if (!isLoading && errorMessage == null && (hasNext || isLoadingMore || loadMoreError != null)) item(key = "my-auction-load-more") {
                LaunchedEffect(items.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        isLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                        loadMoreError != null -> {
                            Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp)
                            TextButton(onClick = onLoadMore) { Text("더 불러오기") }
                        }
                    }
                }
            }
            }
        }
    }

    editing?.let { auctionId ->
        val auction = items.firstOrNull { it.auction.auctionId == auctionId }?.auction
        if (auction != null) {
            AuctionConditionDialog(
                dialogKey = "edit-$auctionId",
                title = "경매 조건 수정",
                confirmLabel = "저장",
                startPrice = auction.startPriceOrNull?.toLong(),
                auctionTimeSeconds = auction.auctionTimeSeconds.takeIf { it > 0 },
                loading = actionAuctionId == auctionId,
                onDismiss = { editing = null },
                onConfirm = { price, seconds -> editing = null; onUpdate(auctionId, price, seconds) }
            )
        }
    }
    starting?.let { auctionId ->
        val auction = items.firstOrNull { it.auction.auctionId == auctionId }?.auction
        if (auction != null) {
            AuctionConditionDialog(
                dialogKey = "start-$auctionId",
                title = "경매를 시작할까요?",
                confirmLabel = "경매 시작",
                description = "시작가와 경매 시간을 정하면 바로 경매가 시작돼요.",
                startPrice = auction.startPriceOrNull?.toLong(),
                auctionTimeSeconds = auction.auctionTimeSeconds.takeIf { it > 0 },
                loading = actionAuctionId == auctionId,
                onDismiss = { starting = null },
                onConfirm = { price, seconds -> starting = null; onStart(auctionId, price, seconds) }
            )
        }
    }
    // 재등록 직후 경매는 시작가/경매 시간이 비어 있고 목록 필터에서 빠질 수 있어 목록 조회 없이 바로 시작 다이얼로그를 띄운다
    relistedAuctionId?.let { auctionId ->
        AuctionConditionDialog(
            dialogKey = "relist-$auctionId",
            title = "다시 올린 경매를 시작할까요?",
            confirmLabel = "경매 시작",
            description = "시작가와 경매 시간을 새로 정하면 바로 경매가 시작돼요.",
            startPrice = null,
            auctionTimeSeconds = null,
            loading = actionAuctionId == auctionId,
            onDismiss = { onRelistConsumed?.invoke() },
            onConfirm = { price, seconds -> onRelistConsumed?.invoke(); onStart(auctionId, price, seconds) }
        )
    }
    cancelling?.let { auctionId ->
        DibDialog(
            onDismissRequest = { cancelling = null },
            title = "예정 경매를 취소할까요?",
            text = { Text("취소 후 상품은 다른 경매에 다시 등록할 수 있어요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = { DibDialogConfirmButton("경매 취소", { cancelling = null; onCancel(auctionId) }, destructive = true) },
            dismissButton = { DibDialogDismissButton({ cancelling = null }, label = "유지") }
        )
    }
}

@Composable
private fun MyAuctionCard(
    sale: SaleHistoryItem,
    actionLoading: Boolean,
    actionsEnabled: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRelist: (() -> Unit)? = null
) {
    val auction = sale.auction
    // 등록이 거절됐거나 다시 검수 중인 상품의 경매는 SCHEDULED 로 남아 있지만 시작할 수 없다.
    // 예전엔 경매 상태만 보고 "경매 예정"과 시작 버튼을 보여줬다
    val productStatus = sale.productStatus?.uppercase()
    val productRejected = productStatus in setOf("REJECTED", "REVIEW_REJECTED")
    val productInReview = productStatus in setOf("PENDING", "PENDING_REVIEW")
    val scheduled = auction.status.equals("SCHEDULED", ignoreCase = true) && !productRejected && !productInReview
    val ended = auction.status.equals("ENDED", ignoreCase = true)
    // 입찰이 0건이고 주문도 없으면 유찰이다. 낙찰(SOLD) 건은 서버가 relist 를 거절한다
    val failedToSell = ended && auction.bidCount == 0 && sale.orderId == null
    Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(72.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        when {
                            productRejected -> "등록 거절"
                            productInReview -> "검수 대기"
                            else -> auctionStatusLabel(auction.status)
                        },
                        color = if (productRejected) Colors.Urgent else auctionStatusColor(auction.status),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (failedToSell) Text("유찰", Modifier.background(Colors.UrgentBackground, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp), color = Colors.Urgent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(auction.title, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("시작가 ${auction.startPriceOrNull?.let { "%,d원".format(it) } ?: "가격 미정"} · 입찰 ${auction.bidCount}회", color = Colors.Muted, fontSize = 11.sp)
                sale.orderStatus?.let { Text("거래 ${orderStatusLabel(it)}", color = Colors.MintInk, fontSize = 10.sp) }
            }
        }
        if (actionLoading) CircularProgressIndicator(Modifier.size(22.dp).align(Alignment.CenterHorizontally), color = Colors.Navy, strokeWidth = 2.dp)
        else if (productRejected || productInReview) {
            Text(
                if (productRejected) "등록이 거절된 상품이에요. 등록 상품 관리에서 사유를 확인하고 수정해주세요."
                else "검수 중인 상품이에요. 승인되면 경매를 시작할 수 있어요.",
                color = Colors.Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        } else if (scheduled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onEdit, Modifier.weight(1f), enabled = actionsEnabled) { Text("조건 수정") }
                Button(onStart, Modifier.weight(1f), enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("지금 시작") }
            }
            TextButton(onCancel, Modifier.fillMaxWidth(), enabled = actionsEnabled) { Text("경매 취소", color = Colors.Urgent) }
        } else if (auction.status.equals("ACTIVE", true) || ended) {
            OutlinedButton(onOpen, Modifier.fillMaxWidth()) { Text("경매 상세 보기") }
            if (failedToSell && onRelist != null) {
                Button(onRelist, Modifier.fillMaxWidth(), enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("다시 올리기") }
            }
        }
    }
}

@Composable
internal fun AuctionConditionDialog(
    dialogKey: String,
    title: String,
    confirmLabel: String,
    startPrice: Long?,
    auctionTimeSeconds: Long?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
    description: String? = null
) {
    var price by rememberSaveable(dialogKey) { mutableStateOf(startPrice?.toString().orEmpty()) }
    var minutes by rememberSaveable(dialogKey) { mutableStateOf(auctionTimeSeconds?.let { (it / 60).coerceAtLeast(MIN_AUCTION_MINUTES).toString() }.orEmpty()) }
    val parsedPrice = price.toLongOrNull()
    val parsedMinutes = minutes.toLongOrNull()
    // 서버(TradeInputValidator)와 같은 10원 단위 규칙. 1001원으로 시작하면 첫 입찰 최소 금액이 10원 단위에 걸려 아무도 입찰할 수 없다
    val priceValid = parsedPrice != null && parsedPrice >= MIN_AUCTION_START_PRICE && parsedPrice % 10L == 0L
    val minutesValid = parsedMinutes != null && parsedMinutes >= MIN_AUCTION_MINUTES
    DibDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = title,
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                description?.let { Text(it, color = Colors.Muted, fontSize = 12.sp) }
                OutlinedTextField(price, { price = it.filter(Char::isDigit).take(10) }, label = { Text("시작가") }, suffix = { Text("원") }, singleLine = true, isError = price.isNotBlank() && !priceValid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("시작가는 1,000원 이상, 10원 단위여야 해요.", color = if (price.isNotBlank() && !priceValid) Colors.Urgent else Colors.Muted, fontSize = 11.sp)
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(5) }, label = { Text("진행 시간") }, suffix = { Text("분") }, singleLine = true, isError = minutes.isNotBlank() && !minutesValid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("경매 시간은 5분 이상이어야 해요.", color = if (minutes.isNotBlank() && !minutesValid) Colors.Urgent else Colors.Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { DibDialogConfirmButton(confirmLabel, onClick = { onConfirm(parsedPrice!!, parsedMinutes!! * 60) }, enabled = priceValid && minutesValid, loading = loading) },
        dismissButton = { DibDialogDismissButton(onDismiss, enabled = !loading) }
    )
}

internal const val MIN_AUCTION_START_PRICE = 1_000L
internal const val MIN_AUCTION_MINUTES = 5L

@Composable
private fun MyAuctionHeader(onBack: () -> Unit) {
    DibSubAppBar("내 경매 관리", onBack)
}

internal fun auctionStatusLabel(status: String): String = when (status.uppercase()) {
    "SCHEDULED" -> "경매 예정"
    "ACTIVE" -> "진행 중"
    "ENDED" -> "종료"
    "CANCELLED", "CANCELED" -> "취소"
    else -> status
}

private fun auctionStatusColor(status: String) = when (status.uppercase()) {
    "ACTIVE" -> Colors.Live
    "SCHEDULED" -> Colors.MintInk
    "CANCELLED", "CANCELED" -> Colors.Urgent
    else -> Colors.Muted
}

private fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "결제 대기"
    "PAID", "PREPARING" -> "발송 준비"
    "SHIPPED" -> "배송 중"
    "DELIEVERED", "DELIVERED" -> "배송 완료"
    "CONFIRMED" -> "거래 확정"
    "CANCELLED", "CANCELED" -> "취소"
    "REFUNDED" -> "환불"
    else -> status
}

private val auctionStatusFilters = listOf(
    "전체" to null,
    "예정" to "SCHEDULED",
    "진행 중" to "ACTIVE",
    "종료" to "ENDED",
    "취소" to "CANCELED"
)
