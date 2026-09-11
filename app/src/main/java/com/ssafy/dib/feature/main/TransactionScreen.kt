package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private enum class TransactionStep { PaymentRequired, Paying, Shipping, Complete }

@Composable
fun TransactionScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableStateOf(TransactionStep.PaymentRequired) }
    var paymentMethod by rememberSaveable { mutableIntStateOf(0) }
    val amount = 58_000
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFAFBFC),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(start = 14.dp, top = 8.dp), fontSize = 22.sp)
                Text(if (step == TransactionStep.Paying) "낙찰 결제" else "거래 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                TransactionStep.PaymentRequired -> {
                    item { StateHeader("결제 필요", 1) }
                    item { StatusHero("▣", "낙찰 상품을 결제해주세요", "오늘 23:59까지 결제해야 거래가 유지돼요.", Color(0xFFFFEEE8)) }
                    item { ProductSummary(amount) }
                    item {
                        InfoCard(listOf("거래 상대" to "dib_user24", "배송지" to "서울 마포구 ·•••", "보증금" to "1,000원 결제"))
                    }
                    item { PrimaryButton("${"%,d".format(amount)}원 결제하기") { step = TransactionStep.Paying } }
                }
                TransactionStep.Paying -> {
                    item { Text("낙찰을 축하해요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Text("결제가 완료되면 판매자와의 거래 채팅이 활성화돼요.", color = Colors.Muted, fontSize = 11.sp) }
                    item { ProductSummary(amount) }
                    item {
                        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(14.dp)) {
                            Text("결제 수단", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            listOf("카드 결제", "계좌이체", "간편결제").forEachIndexed { index, label ->
                                Row(
                                    Modifier.fillMaxWidth().height(44.dp).clickable { paymentMethod = index },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (paymentMethod == index) "●" else "○", color = if (paymentMethod == index) Colors.Navy else Colors.Muted)
                                    Text(label, Modifier.padding(start = 7.dp).weight(1f), color = Colors.Navy, fontSize = 13.sp)
                                    if (index == paymentMethod) Text(if (index == 2) "TossPayments" else "선택됨", color = Color(0xFF41AA8E), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    item { PrimaryButton("${"%,d".format(amount)}원 결제하기") { step = TransactionStep.Shipping } }
                }
                TransactionStep.Shipping -> {
                    item { StateHeader("배송 중", 2) }
                    item { StatusHero("✓", "상품이 배송되고 있어요", "수령 후 상품을 확인하고 구매를 확정해주세요", Color(0xFFF1FAF7)) }
                    item { ProductSummary(amount) }
                    item { ProgressCard(2) }
                    item { InfoCard(listOf("택배사" to "CJ대한통운", "송장번호" to "1234-5678-9012"), "배송 정보") }
                    item { InfoCard(listOf("상품 금액" to "58,000원", "배송비" to "0원", "총 결제 금액" to "58,000원"), "결제 정보") }
                    item { PrimaryButton("구매 확정하기") { step = TransactionStep.Complete } }
                }
                TransactionStep.Complete -> {
                    item { StateHeader("구매 완료", 3) }
                    item { StatusHero("✓", "거래가 완료됐어요", "상품 후기를 남기면 다른 사용자에게 도움이 돼요.", Color(0xFFE8FAF5)) }
                    item { ProductSummary(amount) }
                    item { ProgressCard(3) }
                    item { InfoCard(listOf("결제 금액" to "58,000원", "보증금 환불" to "1,000원", "거래 상태" to "구매 확정"), "거래 정보") }
                    item { PrimaryButton("내 거래로 돌아가기", onBack) }
                }
            }
        }
    }
}

@Composable private fun StateHeader(label: String, progress: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = if (progress == 1) Color(0xFFF28A63) else Color(0xFF27806E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFDBE0E8), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(progress / 3f).height(4.dp).background(if (progress == 1) Color(0xFFF28A63) else Color(0xFF61D1B2), RoundedCornerShape(2.dp)))
        }
    }
}

@Composable private fun StatusHero(icon: String, title: String, body: String, color: Color) {
    Column(
        Modifier.fillMaxWidth().height(182.dp).background(color, RoundedCornerShape(18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, color = Colors.Navy, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(title, color = Colors.Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(body, Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 12.sp)
    }
}

@Composable private fun ProductSummary(amount: Int) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE6E9EE), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(68.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("상품 이미지", color = Colors.Muted, fontSize = 9.sp) }
        Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("빈티지 필름 카메라", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("낙찰가 ${"%,d".format(amount)}원 · 주문 2026-0903", color = Colors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable private fun ProgressCard(current: Int) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF7F8FA), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text("거래 진행", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
            listOf("결제 완료", "배송 중", "구매 확정").forEachIndexed { index, label ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(24.dp).background(if (index < current) Color(0xFF61D1B2) else Color(0xFFD7DBE0), CircleShape), contentAlignment = Alignment.Center) {
                        Text(if (index < current - 1) "✓" else "${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(label, Modifier.padding(top = 5.dp), color = if (index == current - 1) Colors.Navy else Colors.Muted, fontSize = 10.sp, fontWeight = if (index == current - 1) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable private fun InfoCard(rows: List<Pair<String, String>>, title: String? = null) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (title != null) { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold); HorizontalDivider(color = Color(0xFFECEEF1)) }
        rows.forEach { (label, value) -> Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp); Text(value, color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
