package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibNetworkImage
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
    onStart: (String) -> Unit,
    onCancel: (String) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelling by rememberSaveable { mutableStateOf<String?>(null) }
    val items = sales.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MyAuctionHeader(onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
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
                        onStart = { onStart(sale.auction.auctionId) },
                        onCancel = { cancelling = sale.auction.auctionId }
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

    editing?.let { auctionId ->
        val auction = items.firstOrNull { it.auction.auctionId == auctionId }?.auction
        if (auction != null) {
            AuctionEditDialog(
                startPrice = auction.startPrice.toLong(),
                auctionTimeSeconds = auction.auctionTimeSeconds,
                loading = actionAuctionId == auctionId,
                onDismiss = { editing = null },
                onConfirm = { price, seconds -> editing = null; onUpdate(auctionId, price, seconds) }
            )
        }
    }
    cancelling?.let { auctionId ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("예정 경매를 취소할까요?") },
            text = { Text("취소 후 상품은 다른 경매에 다시 등록할 수 있어요.") },
            confirmButton = { TextButton({ cancelling = null; onCancel(auctionId) }) { Text("경매 취소", color = Colors.Urgent) } },
            dismissButton = { TextButton({ cancelling = null }) { Text("유지") } }
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
    onCancel: () -> Unit
) {
    val auction = sale.auction
    val scheduled = auction.status.equals("SCHEDULED", ignoreCase = true)
    Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(72.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(auctionStatusLabel(auction.status), color = auctionStatusColor(auction.status), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(auction.title, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("시작가 ${"%,d".format(auction.startPrice)}원 · 입찰 ${auction.bidCount}회", color = Colors.Muted, fontSize = 11.sp)
                sale.orderStatus?.let { Text("거래 ${orderStatusLabel(it)}", color = Colors.MintInk, fontSize = 10.sp) }
            }
        }
        if (actionLoading) CircularProgressIndicator(Modifier.size(22.dp).align(Alignment.CenterHorizontally), color = Colors.Navy, strokeWidth = 2.dp)
        else if (scheduled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onEdit, Modifier.weight(1f), enabled = actionsEnabled) { Text("조건 수정") }
                Button(onStart, Modifier.weight(1f), enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("지금 시작") }
            }
            TextButton(onCancel, Modifier.fillMaxWidth(), enabled = actionsEnabled) { Text("경매 취소", color = Colors.Urgent) }
        } else if (auction.status.equals("ACTIVE", true) || auction.status.equals("ENDED", true)) {
            OutlinedButton(onOpen, Modifier.fillMaxWidth()) { Text("경매 상세 보기") }
        }
    }
}

@Composable
private fun AuctionEditDialog(
    startPrice: Long,
    auctionTimeSeconds: Long,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var price by rememberSaveable { mutableStateOf(startPrice.toString()) }
    var minutes by rememberSaveable { mutableStateOf((auctionTimeSeconds / 60).coerceAtLeast(1).toString()) }
    val parsedPrice = price.toLongOrNull()
    val parsedMinutes = minutes.toLongOrNull()
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("경매 조건 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(price, { price = it.filter(Char::isDigit).take(10) }, label = { Text("시작가") }, suffix = { Text("원") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(5) }, label = { Text("진행 시간") }, suffix = { Text("분") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton({ onConfirm(parsedPrice!!, parsedMinutes!! * 60) }, enabled = parsedPrice != null && parsedPrice > 0 && parsedMinutes != null && parsedMinutes > 0 && !loading) { Text("저장") } },
        dismissButton = { TextButton(onDismiss, enabled = !loading) { Text("취소") } }
    )
}

@Composable
private fun MyAuctionHeader(onBack: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(52.dp).background(Colors.Background).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", Modifier.size(44.dp).clickable(onClick = onBack).padding(top = 7.dp), fontSize = 24.sp)
            Text("내 경매 관리", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = Colors.Border)
    }
}

internal fun auctionStatusLabel(status: String): String = when (status.uppercase()) {
    "SCHEDULED" -> "경매 예정"
    "ACTIVE" -> "진행 중"
    "ENDED" -> "종료"
    "CANCELLED" -> "취소"
    else -> status
}

private fun auctionStatusColor(status: String) = when (status.uppercase()) {
    "ACTIVE" -> Colors.Live
    "SCHEDULED" -> Colors.MintInk
    "CANCELLED" -> Colors.Urgent
    else -> Colors.Muted
}

private fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "결제 대기"
    "PAID", "PREPARING" -> "발송 준비"
    "SHIPPED" -> "배송 중"
    "DELIEVERED", "DELIVERED" -> "배송 완료"
    "CONFIRMED" -> "거래 확정"
    "CANCELLED" -> "취소"
    "REFUNDED" -> "환불"
    else -> status
}

private val auctionStatusFilters = listOf(
    "전체" to null,
    "예정" to "SCHEDULED",
    "진행 중" to "ACTIVE",
    "종료" to "ENDED",
    "취소" to "CANCELLED"
)
