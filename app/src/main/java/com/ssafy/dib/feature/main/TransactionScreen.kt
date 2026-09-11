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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.payment.PaymentPreparation

private enum class TransactionStep { PaymentRequired, Paying, PaymentFailed, PaymentSuccess, Preparing, Shipping, Delivered, Complete }
private enum class SellerStep { ShippingRequired, TrackingInput, Shipping, Settlement }

@Composable
fun TransactionScreen(
    role: String,
    remoteOrder: OrderSummary?,
    isLoading: Boolean,
    errorMessage: String?,
    confirmationLoading: Boolean,
    confirmationError: String?,
    paymentPreparation: PaymentPreparation?,
    paymentLoading: Boolean,
    paymentError: String?,
    onPreparePayment: () -> Unit,
    onCheckPayment: () -> Unit,
    onResetPayment: () -> Unit,
    onRetry: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading || errorMessage != null || remoteOrder != null) {
        RemoteTransactionScreen(
            role = role,
            order = remoteOrder,
            isLoading = isLoading,
            errorMessage = errorMessage,
            confirmationLoading = confirmationLoading,
            confirmationError = confirmationError,
            paymentPreparation = paymentPreparation,
            paymentLoading = paymentLoading,
            paymentError = paymentError,
            onPreparePayment = onPreparePayment,
            onCheckPayment = onCheckPayment,
            onResetPayment = onResetPayment,
            onRetry = onRetry,
            onConfirmPurchase = onConfirmPurchase,
            onBack = onBack,
            modifier = modifier
        )
    } else {
        SampleTransactionScreen(role, onBack, modifier)
    }
}

@Composable
private fun RemoteTransactionScreen(
    role: String,
    order: OrderSummary?,
    isLoading: Boolean,
    errorMessage: String?,
    confirmationLoading: Boolean,
    confirmationError: String?,
    paymentPreparation: PaymentPreparation?,
    paymentLoading: Boolean,
    paymentError: String?,
    onPreparePayment: () -> Unit,
    onCheckPayment: () -> Unit,
    onResetPayment: () -> Unit,
    onRetry: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(paymentPreparation?.orderId, paymentPreparation?.paymentUrl) {
        paymentPreparation?.paymentUrl?.let { url -> runCatching { uriHandler.openUri(url) } }
    }
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFAFBFC),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(start = 14.dp, top = 8.dp), fontSize = 22.sp)
                Text(if (role == "seller") "판매 거래 상세" else "구매 거래 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isLoading -> item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 80.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Colors.Navy)
                    }
                }
                errorMessage != null -> item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                        OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
                    }
                }
                order != null -> {
                    val presentation = orderPresentation(order.status, role == "seller")
                    item { StatusHero(presentation.icon, presentation.title, presentation.description, presentation.background) }
                    item { ProductSummary(order.finalPrice, order.title, order.orderId) }
                    item {
                        InfoCard(
                            listOf(
                                "주문 번호" to order.orderId,
                                "거래 상태" to presentation.statusLabel,
                                "낙찰 금액" to "${"%,d".format(order.finalPrice)}원"
                            ),
                            "거래 정보"
                        )
                    }
                    confirmationError?.let { message ->
                        item { Text(message, color = Colors.Urgent, fontSize = 12.sp) }
                    }
                    if (role != "seller" && order.status.uppercase() == "PENDING") {
                        paymentError?.let { message ->
                            item { Text(message, color = Colors.Urgent, fontSize = 12.sp) }
                        }
                        if (paymentPreparation == null) {
                            item {
                                Button(
                                    onClick = onPreparePayment,
                                    enabled = !paymentLoading,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) {
                                    if (paymentLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text("${"%,d".format(order.finalPrice)}원 결제하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            item {
                                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("결제 승인을 기다리고 있어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("결제 금액 ${"%,d".format(paymentPreparation.amount)}원", color = Colors.Muted, fontSize = 12.sp)
                                    Text(if (paymentPreparation.paymentUrl != null) "열린 결제 페이지에서 결제를 마쳐주세요." else "결제 페이지 주소가 없어 PG 설정 확인이 필요해요.", color = Colors.Muted, fontSize = 12.sp)
                                }
                            }
                            item {
                                Button(onClick = onCheckPayment, enabled = !paymentLoading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                                    if (paymentLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text("결제 상태 확인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            item { TextButton(onClick = onResetPayment, modifier = Modifier.fillMaxWidth()) { Text("결제수단 다시 선택", color = Colors.Navy) } }
                        }
                    }
                    if (role != "seller" && order.status.uppercase() in setOf("DELIEVERED", "DELIVERED")) {
                        item {
                            Button(
                                onClick = { showConfirm = true },
                                enabled = !confirmationLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                            ) {
                                if (confirmationLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("구매 확정", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("구매를 확정할까요?") },
            text = { Text("확정 후 판매자 정산이 시작되며 단순 변심으로 취소할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onConfirmPurchase() }) { Text("구매 확정") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("취소") } }
        )
    }
}

private data class OrderPresentation(
    val statusLabel: String,
    val icon: String,
    val title: String,
    val description: String,
    val background: Color
)

private fun orderPresentation(status: String, seller: Boolean): OrderPresentation = when (status.uppercase()) {
    "PENDING" -> OrderPresentation("결제 대기", "!", if (seller) "구매자의 결제를 기다리고 있어요" else "결제가 필요해요", "결제가 완료되면 거래가 시작돼요.", Color(0xFFFFEEE8))
    "PAID", "PREPARING" -> OrderPresentation("발송 준비 중", "▣", if (seller) "상품을 발송해주세요" else "판매자가 상품을 준비 중이에요", "배송 정보가 등록되면 바로 알려드릴게요.", Color(0xFFF1F5FA))
    "SHIPPED" -> OrderPresentation("배송 중", "✓", "상품이 배송되고 있어요", "배송 완료 후 상품 상태를 확인해주세요.", Color(0xFFF1FAF7))
    "DELIEVERED", "DELIVERED" -> OrderPresentation("배송 완료", "▣", if (seller) "구매 확정을 기다리고 있어요" else "상품을 받으셨나요?", "상품 상태를 확인한 뒤 구매를 확정해주세요.", Color(0xFFF1FAF7))
    "CONFIRMED" -> OrderPresentation(if (seller) "판매 완료" else "구매 완료", "✓", "거래가 완료됐어요", "안전하게 거래가 마무리됐어요.", Color(0xFFE8FAF5))
    "CANCELLED" -> OrderPresentation("거래 취소", "!", "거래가 취소됐어요", "상세 사유는 고객센터에서 확인할 수 있어요.", Color(0xFFF1F3F5))
    "REFUNDED" -> OrderPresentation("환불 완료", "✓", "환불이 완료됐어요", "결제수단의 환불 내역을 확인해주세요.", Color(0xFFF1F3F5))
    else -> OrderPresentation(status, "▣", "거래가 진행 중이에요", "최신 거래 상태를 확인해주세요.", Color(0xFFF1F5FA))
}

@Composable
private fun SampleTransactionScreen(role: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    if (role == "seller") {
        SellerTransactionScreen(onBack, modifier)
        return
    }
    var step by rememberSaveable { mutableStateOf(TransactionStep.PaymentRequired) }
    var paymentMethod by rememberSaveable { mutableIntStateOf(0) }
    var tossRetried by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val amount = 58_000
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFAFBFC),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(start = 14.dp, top = 8.dp), fontSize = 22.sp)
                Text(if (step in listOf(TransactionStep.Paying, TransactionStep.PaymentFailed, TransactionStep.PaymentSuccess)) "낙찰 결제" else "거래 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    item { StateHeader("재결제 필요", 1) }
                    item { StatusHero("!", "자동 결제를 완료하지 못했어요", "등록 결제수단을 변경하거나 기한 내 다시 결제해주세요.", Color(0xFFFFEEE8)) }
                    item { ProductSummary(amount) }
                    item {
                        InfoCard(listOf("거래 상대" to "dib_user24", "배송지" to "서울 마포구 ·•••", "자동 결제" to "승인 실패"))
                    }
                    item { PrimaryButton("결제수단 변경·재결제") { step = TransactionStep.Paying } }
                }
                TransactionStep.Paying -> {
                    item { Text("낙찰을 축하해요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Text("결제가 완료되면 판매자와의 거래 채팅이 활성화돼요.", color = Colors.Muted, fontSize = 11.sp) }
                    item { ProductSummary(amount) }
                    item {
                        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(14.dp)) {
                            Text("결제 수단", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            listOf("신한카드 ···· 1234", "우리카드 ···· 5678", "새 결제수단 등록").forEachIndexed { index, label ->
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
                    item { PrimaryButton("${"%,d".format(amount)}원 재결제하기") { step = if (paymentMethod == 2 && !tossRetried) TransactionStep.PaymentFailed else TransactionStep.PaymentSuccess } }
                }
                TransactionStep.PaymentFailed -> {
                    item { StatusHero("!", "결제를 완료하지 못했어요", "결제 승인 중 문제가 발생했어요. 결제 수단을 확인한 뒤 다시 시도해주세요.", Color(0xFFFFE9E9)) }
                    item { InfoCard(listOf("실패 사유" to "카드 승인 실패", "결제 상태" to "결제되지 않음")) }
                    item { PrimaryButton("다시 결제하기") { tossRetried = true; step = TransactionStep.Paying } }
                    item { TextButton({ paymentMethod = 0; step = TransactionStep.Paying }, Modifier.fillMaxWidth()) { Text("다른 결제 수단 선택", color = Colors.Navy) } }
                }
                TransactionStep.PaymentSuccess -> {
                    item { StatusHero("✓", "결제가 완료됐어요", "판매자와의 거래 채팅이 열렸어요. 배송·수령 방법을 협의해주세요.", Color(0xFFE8FAF5)) }
                    item { InfoCard(listOf("결제 금액" to "58,000원", "결제 수단" to if (paymentMethod == 2) "TossPayments" else "등록 카드 ···· 1234")) }
                    item { PrimaryButton("거래 시작") { step = TransactionStep.Preparing } }
                }
                TransactionStep.Preparing -> {
                    item { StateHeader("발송 준비 중", 1) }
                    item { StatusHero("▣", "판매자가 상품을 준비 중이에요", "발송되면 운송장과 배송 현황을 알려드릴게요.", Color(0xFFF1F5FA)) }
                    item { ProductSummary(amount) }
                    item { InfoCard(listOf("거래 상대" to "dib_user24", "배송지" to "서울 마포구 ·•••", "결제" to "58,000원 승인")) }
                    item { PrimaryButton("발송 알림 확인") { step = TransactionStep.Shipping } }
                }
                TransactionStep.Shipping -> {
                    item { StateHeader("배송 중", 2) }
                    item { StatusHero("✓", "상품이 배송되고 있어요", "수령 후 상품을 확인하고 구매를 확정해주세요", Color(0xFFF1FAF7)) }
                    item { ProductSummary(amount) }
                    item { ProgressCard(2) }
                    item { InfoCard(listOf("택배사" to "CJ대한통운", "송장번호" to "1234-5678-9012"), "배송 정보") }
                    item { InfoCard(listOf("상품 금액" to "58,000원", "배송비" to "0원", "총 결제 금액" to "58,000원"), "결제 정보") }
                    item { PrimaryButton("배송 조회하기") { step = TransactionStep.Delivered } }
                }
                TransactionStep.Delivered -> {
                    item { StateHeader("배송 완료", 2) }
                    item { StatusHero("▣", "상품을 받으셨나요?", "상품 상태를 확인한 뒤 구매를 확정해주세요.", Color(0xFFF1FAF7)) }
                    item { ProductSummary(amount) }
                    item { ProgressCard(2) }
                    item { InfoCard(listOf("상품 인수" to "09.09 14:20", "이동 중" to "09.10 08:10", "배송 출발" to "09.10 12:30", "배송 완료" to "09.10 16:42"), "배송 진행") }
                    item { PrimaryButton("구매 확정") { showConfirm = true } }
                }
                TransactionStep.Complete -> {
                    item { StateHeader("구매 완료", 3) }
                    item { StatusHero("✓", "거래가 완료됐어요", "상품 후기를 남기면 다른 사용자에게 도움이 돼요.", Color(0xFFE8FAF5)) }
                    item { ProductSummary(amount) }
                    item { ProgressCard(3) }
                    item { InfoCard(listOf("결제 금액" to "58,000원", "결제 방식" to "등록 결제수단 자동 결제", "거래 상태" to "구매 확정"), "거래 정보") }
                    item { PrimaryButton("내 거래로 돌아가기", onBack) }
                }
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("구매를 확정할까요?") },
            text = { Text("구매 확정 후 판매자 정산이 시작되며 단순 변심으로 결제를 취소할 수 없습니다.") },
            confirmButton = { TextButton({ showConfirm = false; step = TransactionStep.Complete }) { Text("구매 확정") } },
            dismissButton = { TextButton({ showConfirm = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun SellerTransactionScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableStateOf(SellerStep.ShippingRequired) }
    var carrier by rememberSaveable { mutableStateOf("") }
    var tracking by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFFAFBFC),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(start = 14.dp, top = 8.dp), fontSize = 22.sp); Text(if (step == SellerStep.TrackingInput) "배송 정보 입력" else if (step == SellerStep.Settlement) "정산 상세" else "판매 거래 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
                SellerStep.ShippingRequired -> {
                    item { StateHeader("발송 필요", 1) }
                    item { StatusHero("▣", "구매자가 결제를 완료했어요", "상품을 포장하고 배송 정보를 등록해주세요.", Color(0xFFFFEEE8)) }
                    item { ProductSummary(35_000) }
                    item { InfoCard(listOf("구매자" to "dib_user24", "배송지" to "서울 마포구 ·•••", "발송 기한" to "1일 남음")) }
                    item { PrimaryButton("배송 정보 입력하기") { step = SellerStep.TrackingInput } }
                }
                SellerStep.TrackingInput -> {
                    item { ProductSummary(35_000) }
                    item { OutlinedTextField(carrier, { carrier = it }, Modifier.fillMaxWidth(), label = { Text("택배사") }, placeholder = { Text("택배사를 선택해주세요") }, singleLine = true, shape = RoundedCornerShape(12.dp)) }
                    item { OutlinedTextField(tracking, { tracking = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("송장번호") }, placeholder = { Text("숫자만 입력해주세요") }, singleLine = true, shape = RoundedCornerShape(12.dp)) }
                    item { Text("등록하면 구매자에게 배송 알림이 전송돼요", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(14.dp), color = Color(0xFF27806E), fontSize = 12.sp) }
                    item { Button({ step = SellerStep.Shipping }, Modifier.fillMaxWidth().height(52.dp), enabled = carrier.isNotBlank() && tracking.length >= 8, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("배송 정보 등록", fontWeight = FontWeight.Bold) } }
                }
                SellerStep.Shipping -> {
                    item { StateHeader("배송 중", 2) }
                    item { StatusHero("✓", "배송 정보가 등록됐어요", "구매자가 상품을 확인하면 정산이 시작돼요.", Color(0xFFE8FAF5)) }
                    item { InfoCard(listOf("택배사" to carrier, "송장번호" to tracking, "구매 확정" to "대기 중"), "배송 정보") }
                    item { PrimaryButton("구매 확정 상태 반영") { step = SellerStep.Settlement } }
                }
                SellerStep.Settlement -> {
                    item { StatusHero("✓", "정산이 예정됐어요", "구매 확정이 완료되어 등록된 계좌로 지급돼요.", Color(0xFFE8FAF5)) }
                    item { ProductSummary(35_000) }
                    item { InfoCard(listOf("낙찰 금액" to "35,000원", "플랫폼 수수료" to "-1,750원", "정산 예정 금액" to "33,250원", "지급 예정" to "2026.09.13"), "정산 상세") }
                    item { InfoCard(listOf("정산 계좌" to "우리은행 1002-***-123456", "예금주" to "김띱")) }
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

@Composable private fun ProductSummary(
    amount: Int,
    title: String = "빈티지 필름 카메라",
    orderId: String = "2026-0903"
) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE6E9EE), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(68.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("상품 이미지", color = Colors.Muted, fontSize = 9.sp) }
        Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("낙찰가 ${"%,d".format(amount)}원 · 주문 $orderId", color = Colors.Muted, fontSize = 11.sp)
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
