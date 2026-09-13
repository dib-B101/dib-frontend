package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.settlement.SettlementDetail
import com.ssafy.dib.domain.settlement.SettlementSummary
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun SettlementHistoryScreen(
    settlements: List<SettlementSummary>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSettlementClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettlementScaffold("정산 내역", onBack, modifier) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().background(Color(0xFFEAF8F4), RoundedCornerShape(14.dp)).padding(16.dp)) {
                    Text("판매 정산", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("구매 확정 후 지급된 금액과 수수료를 확인할 수 있어요.", Modifier.padding(top = 5.dp), color = Colors.Muted, fontSize = 12.sp)
                }
            }
            errorMessage?.let { message -> item { ErrorRow(message, onRetry) } }
            if (settlements.isNullOrEmpty() && isLoading) {
                item { LoadingBlock() }
            } else if (settlements.isNullOrEmpty() && errorMessage == null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("정산 내역이 없어요", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("판매 거래가 확정되면 이곳에서 확인할 수 있어요.", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            } else {
                items(settlements.orEmpty(), key = SettlementSummary::settlementId) { settlement ->
                    SettlementCard(settlement) { onSettlementClick(settlement.settlementId) }
                }
                if (hasNext) item {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("더 보기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementDetailScreen(
    detail: SettlementDetail?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettlementScaffold("정산 상세", onBack, modifier) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            errorMessage?.let { message -> item { ErrorRow(message, onRetry) } }
            if (detail == null && isLoading) item { LoadingBlock() }
            detail?.let { settlement ->
                item {
                    Column(Modifier.fillMaxWidth().background(Color(0xFFEAF8F4), RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (settlement.payoutAt == null) "정산 처리 중" else "정산 완료", color = Color(0xFF27806E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${"%,d".format(settlement.netAmount)}원", Modifier.padding(top = 8.dp), color = Colors.Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(formatSettlementDate(settlement.payoutAt), Modifier.padding(top = 6.dp), color = Colors.Muted, fontSize = 12.sp)
                    }
                }
                item { DetailCard("금액", listOf("판매 금액" to money(settlement.grossAmount), "플랫폼 수수료" to "-${money(settlement.commissionFee)}", "최종 정산액" to money(settlement.netAmount))) }
                item { DetailCard("지급 계좌", listOf("은행" to settlement.bankName, "계좌번호" to settlement.maskedAccountNumber)) }
                item { DetailCard("거래 정보", listOf("주문 번호" to settlement.orderId, "정산 번호" to settlement.settlementId)) }
            }
        }
    }
}

@Composable
private fun SettlementCard(item: SettlementSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color(0xFFEAF8F4), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("₩", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (item.payoutAt == null) "정산 처리 중" else "정산 완료", color = if (item.payoutAt == null) Colors.Urgent else Color(0xFF27806E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(money(item.netAmount), color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("주문 ${item.orderId} · ${formatSettlementDate(item.payoutAt)}", color = Colors.Muted, fontSize = 11.sp, maxLines = 1)
        }
        Text("›", color = Colors.Muted, fontSize = 24.sp)
    }
}

@Composable
private fun DetailCard(title: String, rows: List<Pair<String, String>>) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = Colors.Border)
        rows.forEach { (label, value) -> Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp); Text(value, color = Colors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFFFEEF0), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
        TextButton(onRetry) { Text("재시도") }
    }
}

@Composable private fun LoadingBlock() = Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Mint) }

@Composable
private fun SettlementScaffold(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFF7F9FB),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.height(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) } },
        content = content
    )
}

private fun money(value: Long) = "${"%,d".format(value)}원"
private fun formatSettlementDate(value: String?): String = value?.take(16)?.replace('T', ' ') ?: "지급 일정을 확인하고 있어요"
