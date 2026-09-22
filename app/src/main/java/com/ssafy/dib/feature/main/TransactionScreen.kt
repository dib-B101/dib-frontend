package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import com.ssafy.dib.R
import com.ssafy.dib.core.time.formatServerTime
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.domain.order.OrderShipment
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.order.OrderShippingAddress
import com.ssafy.dib.domain.order.ShippingCarrier
import com.ssafy.dib.domain.payment.Payment

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
    paymentLoading: Boolean,
    paymentError: String?,
    completedPayment: Payment?,
    completedPaymentLoading: Boolean,
    completedPaymentError: String?,
    shipment: OrderShipment?,
    shipmentLoading: Boolean,
    shipmentError: String?,
    shippingAddress: OrderShippingAddress?,
    shippingAddressLoading: Boolean,
    shippingAddressError: String?,
    addressSubmitting: Boolean,
    addressSubmitError: String?,
    savedAddresses: List<com.ssafy.dib.domain.member.MemberAddress>,
    onSubmitShippingAddress: (com.ssafy.dib.domain.order.OrderAddressInput) -> Unit,
    shippingCarriers: List<ShippingCarrier>?,
    shippingCarriersLoading: Boolean,
    shippingCarriersError: String?,
    onRetryPayment: () -> Unit,
    onManagePaymentMethod: () -> Unit,
    onRegisterShipment: (String, String) -> Unit,
    onShippingCarriersRetry: () -> Unit,
    onRefreshShipment: () -> Unit,
    onRetry: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenChat: () -> Unit,
    reportSubmitting: Boolean = false,
    reportError: String? = null,
    reportCompleted: Boolean = false,
    onReportOrder: (String, String) -> Unit = { _, _ -> },
    onDismissReport: () -> Unit = {},
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
            paymentLoading = paymentLoading,
            paymentError = paymentError,
            completedPayment = completedPayment,
            completedPaymentLoading = completedPaymentLoading,
            completedPaymentError = completedPaymentError,
            shipment = shipment,
            shipmentLoading = shipmentLoading,
            shipmentError = shipmentError,
            shippingAddress = shippingAddress,
            shippingAddressLoading = shippingAddressLoading,
            shippingAddressError = shippingAddressError,
            addressSubmitting = addressSubmitting,
            addressSubmitError = addressSubmitError,
            savedAddresses = savedAddresses,
            onSubmitShippingAddress = onSubmitShippingAddress,
            shippingCarriers = shippingCarriers,
            shippingCarriersLoading = shippingCarriersLoading,
            shippingCarriersError = shippingCarriersError,
            onRetryPayment = onRetryPayment,
            onManagePaymentMethod = onManagePaymentMethod,
            onRegisterShipment = onRegisterShipment,
            onShippingCarriersRetry = onShippingCarriersRetry,
            onRefreshShipment = onRefreshShipment,
            onRetry = onRetry,
            onConfirmPurchase = onConfirmPurchase,
            onOpenReview = onOpenReview,
            onOpenChat = onOpenChat,
            reportSubmitting = reportSubmitting,
            reportError = reportError,
            reportCompleted = reportCompleted,
            onReportOrder = onReportOrder,
            onDismissReport = onDismissReport,
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
    paymentLoading: Boolean,
    paymentError: String?,
    completedPayment: Payment?,
    completedPaymentLoading: Boolean,
    completedPaymentError: String?,
    shipment: OrderShipment?,
    shipmentLoading: Boolean,
    shipmentError: String?,
    shippingAddress: OrderShippingAddress?,
    shippingAddressLoading: Boolean,
    shippingAddressError: String?,
    addressSubmitting: Boolean,
    addressSubmitError: String?,
    savedAddresses: List<com.ssafy.dib.domain.member.MemberAddress>,
    onSubmitShippingAddress: (com.ssafy.dib.domain.order.OrderAddressInput) -> Unit,
    shippingCarriers: List<ShippingCarrier>?,
    shippingCarriersLoading: Boolean,
    shippingCarriersError: String?,
    onRetryPayment: () -> Unit,
    onManagePaymentMethod: () -> Unit,
    onRegisterShipment: (String, String) -> Unit,
    onShippingCarriersRetry: () -> Unit,
    onRefreshShipment: () -> Unit,
    onRetry: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenChat: () -> Unit,
    reportSubmitting: Boolean,
    reportError: String?,
    reportCompleted: Boolean,
    onReportOrder: (String, String) -> Unit,
    onDismissReport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable(order?.orderId) { mutableStateOf(false) }
    var showAddressInput by rememberSaveable { mutableStateOf(false) }
    var trackingNumber by rememberSaveable(order?.orderId) { mutableStateOf("") }
    var selectedCarrier by rememberSaveable(order?.orderId) { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TransactionAppBar(if (role == "seller") "판매 거래 상세" else "구매 거래 상세", onBack)
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp),
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
                        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                        OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
                    }
                }
                order != null -> {
                    val presentation = orderPresentation(order.status, role == "seller")
                    // 서버가 신고 접수 시 heldAt 을 채우고, 그 동안 구매확정·송장등록을 409(ORDER_ON_HOLD)로 막는다
                    val onHold = order.heldAt != null
                    if (onHold) item { OrderHoldBanner(order.heldAt) }
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
                    if (completedPaymentLoading) item { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Colors.Mint, strokeWidth = 2.dp) } }
                    completedPaymentError?.let { message -> item { Text(message, color = Colors.Urgent, fontSize = 12.sp) } }
                    completedPayment?.let { payment ->
                        item {
                            InfoCard(
                                listOf(
                                    "결제 수단" to if (payment.type == "TRANSFER") "계좌이체" else "카드",
                                    "결제 금액" to "${"%,d".format(payment.amount)}원",
                                    "결제 일시" to (formatServerTime(payment.paidAt) ?: "확인 중")
                                ),
                                "결제 정보"
                            )
                        }
                        payment.receiptUrl?.takeIf(String::isNotBlank)?.let { receiptUrl ->
                            item { OutlinedButton(onClick = { runCatching { uriHandler.openUri(receiptUrl) } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("결제 영수증 보기", color = Colors.Navy, fontWeight = FontWeight.Bold) } }
                        }
                    }
                    if (shippingAddressLoading) item { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Colors.Mint, strokeWidth = 2.dp) } }
                    shippingAddressError?.let { message -> item { Text(message, color = Colors.Urgent, fontSize = 12.sp) } }
                    shippingAddress?.takeIf { it.isRegistered }?.let { destination ->
                        item {
                            InfoCard(
                                listOf(
                                    "받는 사람" to destination.name,
                                    "연락처" to (destination.phoneNumber ?: "-"),
                                    "우편번호" to destination.postalCode.ifBlank { "-" },
                                    "주소" to destination.address
                                ),
                                "배송지"
                            )
                        }
                    }
                    // 배송지는 결제(PAID) 된 뒤 구매자가 한 번 입력한다. 이게 없으면 판매자가 송장을 못 넣는다
                    if (order.status.uppercase() == "PAID" && shippingAddress?.isRegistered != true && !shippingAddressLoading) {
                        if (role == "seller") {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(16.dp)).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("구매자가 배송지를 입력하기 전이에요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("배송지가 등록되어야 송장을 넣을 수 있어요. 거래 채팅으로 안내해보세요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        } else {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(16.dp)).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("배송지를 입력해주세요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("배송지를 등록해야 판매자가 상품을 발송할 수 있어요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                            item {
                                Button(
                                    onClick = { showAddressInput = true },
                                    enabled = !addressSubmitting,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) {
                                    if (addressSubmitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Text("배송지 입력", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            addressSubmitError?.let { message -> item { Text(message, color = Colors.Urgent, fontSize = 12.sp) } }
                        }
                    }
                    if (order.status.uppercase() !in setOf("PENDING", "CANCELLED", "CANCELED", "REFUNDED")) {
                        item { OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("거래 채팅", color = Colors.Navy, fontWeight = FontWeight.Bold) } }
                    }
                    if (order.status.uppercase() !in setOf("PENDING", "CONFIRMED", "CANCELLED", "CANCELED", "REFUNDED")) {
                        item {
                            OutlinedButton(
                                onClick = { showReport = true },
                                enabled = !onHold && !reportSubmitting,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (onHold) "신고 접수됨" else "거래 신고", color = if (onHold) Colors.Muted else Colors.Urgent, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!onHold) item { Text("상품 상태나 미발송, 거래 채팅에서 문제가 있었다면 신고해주세요.", color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp) }
                    }
                    confirmationError?.let { message ->
                        item { Text(message, color = Colors.Urgent, fontSize = 12.sp) }
                    }
                    shipmentError?.let { message ->
                        item { Text(message, color = Colors.Urgent, fontSize = 12.sp) }
                    }
                    if (role != "seller" && order.status.uppercase() == "PENDING") {
                        paymentError?.let { message ->
                            item { Text(message, color = Colors.Urgent, fontSize = 12.sp) }
                        }
                        item {
                            Column(
                                Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(16.dp)).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("자동결제를 완료하지 못했어요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("등록한 카드 상태를 확인한 뒤 낙찰 금액 결제를 다시 요청해주세요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                // 기한이 지나면 주문이 자동 취소되므로 마감 시각을 그대로 노출한다
                                formatServerTime(order.paymentDue)?.let { deadline ->
                                    Text("결제 기한: $deadline", color = Colors.Urgent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = onRetryPayment,
                                enabled = !paymentLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                            ) {
                                if (paymentLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("등록 카드로 재결제", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item { TextButton(onClick = onManagePaymentMethod, modifier = Modifier.fillMaxWidth()) { Text("결제수단 관리", color = Colors.Navy) } }
                    }
                    if (role != "seller" && order.status.uppercase() in setOf("DELIEVERED", "DELIVERED")) {
                        item {
                            Button(
                                onClick = { showConfirm = true },
                                enabled = !confirmationLoading && !onHold,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                            ) {
                                if (confirmationLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("구매 확정", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onHold) item { Text(ORDER_HOLD_BLOCK_MESSAGE, color = Colors.Urgent, fontSize = 12.sp, lineHeight = 18.sp) }
                    }
                    // 거래가 끝난 뒤에만 평가할 수 있다. 서버도 CONFIRMED 가 아니면 거절한다
                    if (role != "seller" && order.status.uppercase() == "CONFIRMED") {
                        item {
                            if (order.myRating != null) {
                                Column(
                                    Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("내가 남긴 평가", color = Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        (1..5).joinToString("") { if (order.myRating >= it) "★" else "☆" },
                                        color = Colors.Live,
                                        fontSize = 20.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onOpenReview,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) {
                                    Text("판매자 평가하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (role == "seller" && order.status.uppercase() in setOf("PAID", "PREPARING")) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("배송 정보 등록", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("실제 발송을 완료한 뒤 택배사와 송장번호를 입력해주세요.", color = Colors.Muted, fontSize = 12.sp)
                                Text("택배사", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                when {
                                    shippingCarriersLoading -> CircularProgressIndicator(Modifier.size(20.dp), color = Colors.Navy, strokeWidth = 2.dp)
                                    shippingCarriersError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(shippingCarriersError, Modifier.weight(1f), color = Colors.Urgent, fontSize = 11.sp)
                                        TextButton(onClick = onShippingCarriersRetry) { Text("재시도") }
                                    }
                                    shippingCarriers.isNullOrEmpty() -> Text("선택할 수 있는 택배사가 없어요.", color = Colors.Muted, fontSize = 11.sp)
                                    else -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        shippingCarriers.forEach { carrier ->
                                            FilterChip(
                                                selected = selectedCarrier == carrier.code,
                                                onClick = { selectedCarrier = carrier.code },
                                                label = { Text(carrier.name, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = trackingNumber,
                                    onValueChange = { trackingNumber = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("송장번호") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = { onRegisterShipment(selectedCarrier, trackingNumber.trim()) },
                                enabled = selectedCarrier.isNotBlank() && trackingNumber.isNotBlank() && !shipmentLoading && !onHold,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                            ) {
                                if (shipmentLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("배송 정보 등록", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onHold) item { Text(ORDER_HOLD_BLOCK_MESSAGE, color = Colors.Urgent, fontSize = 12.sp, lineHeight = 18.sp) }
                    }
                    if (order.status.uppercase() in setOf("SHIPPED", "DELIEVERED", "DELIVERED") && shipment != null) {
                        item {
                            InfoCard(
                                listOf(
                                    "택배사" to (shippingCarriers?.firstOrNull { it.code == shipment.carrier }?.name ?: shipment.carrier ?: "확인 중"),
                                    "송장번호" to shipment.trackingNumber,
                                    "배송 상태" to shipmentStatusLabel(shipment.carrierStatus ?: shipment.status),
                                    "조회 상태" to if (shipment.isStale) "최근 저장 정보" else "최신 정보"
                                ),
                                "배송 정보"
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = onRefreshShipment,
                                enabled = !shipmentLoading,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (shipmentLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Colors.Navy, strokeWidth = 2.dp)
                                else Text("배송 상태 새로고침", color = Colors.Navy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    // 서버는 구매 확정 전까지 settlement 블록을 null 로 준다
                    val settlement = order.settlement
                    if (role == "seller" && settlement != null) {
                        item {
                            InfoCard(
                                listOf(
                                    "정산 예정 금액" to (settlement.netAmount?.let { amount -> "${"%,d".format(amount)}원" } ?: "정산 준비 중"),
                                    "지급 예정일" to (formatServerTime(settlement.payoutAt) ?: "지급 일정 확인 중")
                                ),
                                "정산 정보"
                            )
                        }
                    }
                }
            }
        }
    }
    if (showAddressInput) {
        OrderAddressDialog(
            submitting = addressSubmitting,
            errorMessage = addressSubmitError,
            savedAddresses = savedAddresses,
            onSubmit = { input -> showAddressInput = false; onSubmitShippingAddress(input) },
            onDismiss = { showAddressInput = false }
        )
    }
    if (showReport) {
        OrderReportDialog(
            submitting = reportSubmitting,
            errorMessage = reportError,
            completed = reportCompleted,
            onSubmit = onReportOrder,
            onDismiss = { showReport = false; onDismissReport() }
        )
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

internal const val ORDER_HOLD_BLOCK_MESSAGE =
    "신고 처리 중인 거래예요. 처리가 끝나면 이어서 진행할 수 있어요."

@Composable
private fun OrderHoldBanner(heldAt: String?) {
    Column(
        Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "신고 처리 중",
            Modifier.background(Colors.Urgent, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text("이 거래는 신고가 접수돼 보류됐어요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(ORDER_HOLD_BLOCK_MESSAGE, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        formatServerTime(heldAt)?.let { at ->
            Text("보류 시작 " + at, color = Colors.Urgent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private const val ORDER_REPORT_TYPE_ORDER = "ORDER"
private const val ORDER_REPORT_TYPE_CHATTING = "CHATTING"
private const val ORDER_REPORT_DETAIL_MIN = 10
private const val ORDER_REPORT_CONTENT_MAX = 500

private val orderProblemReasons = listOf(
    "상품이 설명과 달라요",
    "상품이 파손·불량 상태예요",
    "판매자가 상품을 보내지 않아요",
    "주문한 것과 다른 상품이 왔어요",
    "기타"
)

private val orderChattingReasons = listOf(
    "비매너·욕설 등 부적절한 언행",
    "외부 거래·직거래 유도",
    "거래 약속 불이행",
    "기타"
)

private fun orderReportReasonsFor(type: String): List<String> =
    if (type == ORDER_REPORT_TYPE_CHATTING) orderChattingReasons else orderProblemReasons

// 상세 내용을 사유 뒤에 붙여 보내므로 사유 길이를 뺀 만큼만 입력할 수 있다
private fun orderReportDetailLimitFor(reason: String): Int =
    (ORDER_REPORT_CONTENT_MAX - ("[신고 사유] " + reason + "\n[상세 내용] ").length).coerceAtLeast(0)

private fun buildOrderReportContent(reason: String, detail: String): String =
    ("[신고 사유] " + reason + "\n[상세 내용] " + detail.trim()).take(ORDER_REPORT_CONTENT_MAX)

@Composable
private fun OrderReportDialog(
    submitting: Boolean,
    errorMessage: String?,
    completed: Boolean,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var reportType by rememberSaveable { mutableStateOf(ORDER_REPORT_TYPE_ORDER) }
    var reason by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    val reasons = orderReportReasonsFor(reportType)
    val detailLimit = orderReportDetailLimitFor(reason)
    val detailValid = detail.trim().length >= ORDER_REPORT_DETAIL_MIN
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(if (completed) "신고가 접수됐어요" else "거래 신고") },
        text = {
            if (completed) {
                Text("검토 후 필요한 조치를 진행할게요. 처리가 끝날 때까지 이 거래는 보류돼요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            } else {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("무엇에 대한 신고인가요?", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        ORDER_REPORT_TYPE_ORDER to "거래 자체 문제 (상품 상태·미발송 등)",
                        ORDER_REPORT_TYPE_CHATTING to "거래 채팅에서의 문제"
                    ).forEach { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !submitting) {
                                if (reportType != value) {
                                    reportType = value
                                    reason = ""
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = reportType == value, onClick = null, enabled = !submitting)
                            Text(label, color = Colors.Text, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = Colors.Border)
                    Text("신고 이유를 선택해주세요", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    reasons.forEach { candidate ->
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !submitting) {
                                reason = candidate
                                detail = detail.take(orderReportDetailLimitFor(candidate))
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = reason == candidate, onClick = null, enabled = !submitting)
                            Text(candidate, color = Colors.Text, fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it.take(detailLimit) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !submitting && reason.isNotBlank(),
                        label = { Text("상세 내용") },
                        placeholder = { Text("상황을 자세히 알려주세요.") },
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                if (detailValid) "${detail.length}/${detailLimit}자" else "${ORDER_REPORT_DETAIL_MIN}자 이상 입력해주세요",
                                color = if (detailValid) Colors.Muted else Colors.Urgent,
                                fontSize = 11.sp
                            )
                        }
                    )
                    Text("접수되면 관리자 확인이 끝날 때까지 이 거래는 보류되고, 구매확정과 송장등록이 막혀요.", color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    Text("허위 신고 또는 반복적인 악의적 신고는 서비스 이용에 제한이 있을 수 있어요.", color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    errorMessage?.let { message -> Text(message, color = Colors.Urgent, fontSize = 11.sp) }
                }
            }
        },
        confirmButton = {
            if (completed) {
                TextButton(onClick = onDismiss) { Text("확인") }
            } else {
                TextButton(
                    onClick = { onSubmit(buildOrderReportContent(reason, detail), reportType) },
                    enabled = reason.isNotBlank() && detailValid && !submitting
                ) { Text(if (submitting) "접수 중" else "신고하기") }
            }
        },
        dismissButton = {
            if (!completed) TextButton(onClick = onDismiss, enabled = !submitting) { Text("취소") }
        }
    )
}

private fun shipmentStatusLabel(status: String): String = when (status.uppercase()) {
    "PREPARING" -> "배송 준비"
    "SHIPPED" -> "배송 중"
    "DELIEVERED", "DELIVERED" -> "배송 완료"
    else -> status
}

private data class OrderPresentation(
    val statusLabel: String,
    val icon: String,
    val title: String,
    val description: String,
    val background: Color
)

private fun orderPresentation(status: String, seller: Boolean): OrderPresentation = when (status.uppercase()) {
    "PENDING" -> OrderPresentation("결제 대기", "결제", if (seller) "구매자의 결제를 기다리고 있어요" else "결제가 필요해요", "결제가 완료되면 거래가 시작돼요.", Color(0xFFFFEEE8))
    "PAID", "PREPARING" -> OrderPresentation("발송 준비 중", "준비", if (seller) "상품을 발송해주세요" else "판매자가 상품을 준비 중이에요", "배송 정보가 등록되면 바로 알려드릴게요.", Color(0xFFF1F5FA))
    "SHIPPED" -> OrderPresentation("배송 중", "✓", "상품이 배송되고 있어요", "배송 완료 후 상품 상태를 확인해주세요.", Color(0xFFF1FAF7))
    "DELIEVERED", "DELIVERED" -> OrderPresentation("배송 완료", "도착", if (seller) "구매 확정을 기다리고 있어요" else "상품을 받으셨나요?", "상품 상태를 확인한 뒤 구매를 확정해주세요.", Color(0xFFF1FAF7))
    "CONFIRMED" -> OrderPresentation(if (seller) "판매 완료" else "구매 완료", "✓", "거래가 완료됐어요", "안전하게 거래가 마무리됐어요.", Color(0xFFE8FAF5))
    "CANCELLED", "CANCELED" -> OrderPresentation("거래 취소", "!", "거래가 취소됐어요", "상세 사유는 고객센터에서 확인할 수 있어요.", Color(0xFFF1F3F5))
    "REFUNDED" -> OrderPresentation("환불 완료", "✓", "환불이 완료됐어요", "결제수단의 환불 내역을 확인해주세요.", Color(0xFFF1F3F5))
    else -> OrderPresentation(status, "거래", "거래가 진행 중이에요", "최신 거래 상태를 확인해주세요.", Color(0xFFF1F5FA))
}

@Composable
private fun SampleTransactionScreen(role: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    if (role == "seller") {
        SellerTransactionScreen(onBack, modifier)
        return
    }
    var step by rememberSaveable { mutableStateOf(TransactionStep.PaymentRequired) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val amount = 58_000
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { TransactionAppBar(if (step in listOf(TransactionStep.Paying, TransactionStep.PaymentFailed, TransactionStep.PaymentSuccess)) "낙찰 결제" else "거래 상세", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                TransactionStep.PaymentRequired -> {
                    item { StateHeader("재결제 필요", 1) }
                    item { StatusHero("!", "자동 결제를 완료하지 못했어요", "등록한 카드를 확인한 뒤 기한 내 다시 결제해주세요.", Color(0xFFFFEEE8)) }
                    item { ProductSummary(amount) }
                    item {
                        InfoCard(listOf("거래 상대" to "dib_user24", "배송지" to "서울 마포구 ·•••", "자동 결제" to "승인 실패"))
                    }
                    item { PrimaryButton("등록 카드로 재결제") { step = TransactionStep.Paying } }
                }
                TransactionStep.Paying -> {
                    item { Text("낙찰을 축하해요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Text("결제가 완료되면 판매자와의 거래 채팅이 활성화돼요.", color = Colors.Muted, fontSize = 11.sp) }
                    item { ProductSummary(amount) }
                    item { InfoCard(listOf("결제수단" to "등록 카드", "결제 금액" to "${"%,d".format(amount)}원")) }
                    item { PrimaryButton("${"%,d".format(amount)}원 재결제하기") { step = TransactionStep.PaymentSuccess } }
                }
                TransactionStep.PaymentFailed -> {
                    item { StatusHero("!", "결제를 완료하지 못했어요", "결제 승인 중 문제가 발생했어요. 결제 수단을 확인한 뒤 다시 시도해주세요.", Color(0xFFFFE9E9)) }
                    item { InfoCard(listOf("실패 사유" to "카드 승인 실패", "결제 상태" to "결제되지 않음")) }
                    item { PrimaryButton("다시 결제하기") { step = TransactionStep.Paying } }
                    item { TextButton({ step = TransactionStep.PaymentRequired }, Modifier.fillMaxWidth()) { Text("결제수단 관리", color = Colors.Navy) } }
                }
                TransactionStep.PaymentSuccess -> {
                    item { StatusHero("✓", "결제가 완료됐어요", "판매자와의 거래 채팅이 열렸어요. 배송·수령 방법을 협의해주세요.", Color(0xFFE8FAF5)) }
                    item { InfoCard(listOf("결제 금액" to "58,000원", "결제 방식" to "등록 카드")) }
                    item { PrimaryButton("거래 시작") { step = TransactionStep.Preparing } }
                }
                TransactionStep.Preparing -> {
                    item { StateHeader("발송 준비 중", 1) }
                    item { StatusHero("준비", "판매자가 상품을 준비 중이에요", "발송되면 운송장과 배송 현황을 알려드릴게요.", Color(0xFFF1F5FA)) }
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
                    item { StatusHero("도착", "상품을 받으셨나요?", "상품 상태를 확인한 뒤 구매를 확정해주세요.", Color(0xFFF1FAF7)) }
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
                    item { InfoCard(listOf("결제 금액" to "58,000원", "결제 방식" to "등록 카드", "거래 상태" to "구매 확정"), "거래 정보") }
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
    var tracking by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFFAFBFC),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { TransactionAppBar(if (step == SellerStep.TrackingInput) "배송 정보 입력" else if (step == SellerStep.Settlement) "정산 상세" else "판매 거래 상세", onBack) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
                SellerStep.ShippingRequired -> {
                    item { StateHeader("발송 필요", 1) }
                    item { StatusHero("준비", "구매자가 결제를 완료했어요", "상품을 포장하고 배송 정보를 등록해주세요.", Color(0xFFFFEEE8)) }
                    item { ProductSummary(35_000) }
                    item { InfoCard(listOf("구매자" to "dib_user24", "배송지" to "서울 마포구 ·•••", "발송 기한" to "1일 남음")) }
                    item { PrimaryButton("배송 정보 입력하기") { step = SellerStep.TrackingInput } }
                }
                SellerStep.TrackingInput -> {
                    item { ProductSummary(35_000) }
                    item { Text("실제 발송을 완료한 뒤 송장번호를 입력해주세요. 택배사는 배송 조회 시 자동으로 확인해요.", color = Colors.Muted, fontSize = 12.sp) }
                    item { OutlinedTextField(tracking, { tracking = it }, Modifier.fillMaxWidth(), label = { Text("송장번호") }, placeholder = { Text("예: 1234-5678-9012") }, singleLine = true, shape = RoundedCornerShape(12.dp)) }
                    item { Text("등록하면 구매자에게 배송 알림이 전송돼요", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(14.dp), color = Color(0xFF27806E), fontSize = 12.sp) }
                    item { Button({ step = SellerStep.Shipping }, Modifier.fillMaxWidth().height(52.dp), enabled = tracking.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("배송 정보 등록", fontWeight = FontWeight.Bold) } }
                }
                SellerStep.Shipping -> {
                    item { StateHeader("배송 중", 2) }
                    item { StatusHero("✓", "배송 정보가 등록됐어요", "구매자가 상품을 확인하면 정산이 시작돼요.", Color(0xFFE8FAF5)) }
                    item { InfoCard(listOf("택배사" to "배송 조회 시 자동 확인", "송장번호" to tracking.trim(), "구매 확정" to "대기 중"), "배송 정보") }
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
        Modifier.fillMaxWidth().height(184.dp).background(color, RoundedCornerShape(20.dp)).padding(horizontal=24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(52.dp).background(Colors.Background.copy(alpha=.82f),CircleShape),contentAlignment=Alignment.Center){Text(icon, color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)}
        Spacer(Modifier.height(16.dp))
        Text(title, color = Colors.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(body, Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 12.sp,lineHeight=18.sp)
    }
}

@Composable private fun ProductSummary(
    amount: Int,
    title: String = "빈티지 필름 카메라",
    orderId: String = "2026-0903"
) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).background(Colors.Background, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(68.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("상품 이미지", color = Colors.Muted, fontSize = 9.sp) }
        Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        if (title != null) { Text(title,color=Colors.Text,fontSize = 15.sp, fontWeight = FontWeight.Bold); HorizontalDivider(color = Colors.Border) }
        rows.forEach { (label, value) -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) { Text(label, Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp); Text(value,Modifier.weight(1.35f), color = Colors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable private fun TransactionAppBar(title:String,onBack:()->Unit){Column(Modifier.background(Colors.Background)){Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Image(painterResource(R.drawable.back),"뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))};Text(title,color=Colors.Text,fontSize=17.sp,fontWeight=FontWeight.Bold)};HorizontalDivider(color=Colors.Border)}}

@Composable private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
