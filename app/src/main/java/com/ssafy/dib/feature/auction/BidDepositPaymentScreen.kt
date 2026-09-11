package com.ssafy.dib.feature.auction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.ssafy.dib.domain.auction.BidDeposit
import com.ssafy.dib.feature.home.allHomeAuctions
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private enum class DepositPaymentState { Form, Processing, AwaitingApproval, Success, Failed }

/** Figma 01_Wireframe / 04A~04E Bid Deposit Payment states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidDepositPaymentScreen(
    auctionId: String,
    bidAmount: Int,
    preparedDeposit: BidDeposit?,
    isProcessing: Boolean,
    errorMessage: String?,
    onPrepare: (String) -> Unit,
    onCheckStatus: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onReturnToAuction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val product = allHomeAuctions.firstOrNull { it.id == auctionId }
    var selectedPaymentMethod by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var showMethodSheet by rememberSaveable { mutableStateOf(false) }
    val state = when {
        isProcessing -> DepositPaymentState.Processing
        preparedDeposit?.status == "PAID" -> DepositPaymentState.Success
        errorMessage != null -> DepositPaymentState.Failed
        preparedDeposit != null -> DepositPaymentState.AwaitingApproval
        else -> DepositPaymentState.Form
    }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(preparedDeposit?.bidDepositId, preparedDeposit?.paymentUrl) {
        preparedDeposit?.paymentUrl?.let { url -> runCatching { uriHandler.openUri(url) } }
    }

    BackHandler(enabled = state != DepositPaymentState.Form) {
        when (state) {
            DepositPaymentState.Success -> onReturnToAuction()
            DepositPaymentState.AwaitingApproval, DepositPaymentState.Failed -> onReset()
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = if (state == DepositPaymentState.Form) Colors.Background else Color(0xFFFAFBFC),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuctionSubAppBar("입찰 보증금 결제") {
                when (state) {
                    DepositPaymentState.Form -> onBack()
                    DepositPaymentState.Processing -> Unit
                    DepositPaymentState.Success -> onReturnToAuction()
                    DepositPaymentState.AwaitingApproval, DepositPaymentState.Failed -> onReset()
                }
            }
        },
        bottomBar = {
            if (state == DepositPaymentState.Form) {
                Column(Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { onPrepare("CARD") },
                        enabled = selectedPaymentMethod.isNotBlank() && agreed,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                    ) {
                        Text(
                            if (selectedPaymentMethod.isBlank()) "결제수단 선택 후 계속" else "보증금 준비하고 결제하기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("결제가 완료되면 입찰이 바로 접수돼요", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 10.sp)
                }
            }
        }
    ) { padding ->
        when (state) {
            DepositPaymentState.Form -> DepositPaymentForm(
                productName = product?.name ?: "선택한 경매",
                bidAmount = bidAmount,
                remainingSeconds = product?.remainingSeconds ?: 0,
                paymentMethod = selectedPaymentMethod,
                agreed = agreed,
                onMethodClick = { showMethodSheet = true },
                onAgreementChange = { agreed = it },
                modifier = Modifier.padding(padding)
            )
            DepositPaymentState.Processing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = Colors.Navy)
                    Text("보증금을 결제하고 있어요", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("잠시만 기다려주세요.", color = Colors.Muted, fontSize = 13.sp)
                }
            }
            DepositPaymentState.AwaitingApproval -> DepositPaymentAwaitingApproval(
                deposit = requireNotNull(preparedDeposit),
                onCheckStatus = onCheckStatus,
                onChangeMethod = onReset,
                modifier = Modifier.padding(padding)
            )
            DepositPaymentState.Success -> DepositPaymentSuccess(
                productName = product?.name ?: "선택한 경매",
                bidAmount = bidAmount,
                depositAmount = preparedDeposit?.amount ?: maxOf(1_000L, bidAmount.toLong() / 10L),
                onReturn = onReturnToAuction,
                modifier = Modifier.padding(padding)
            )
            DepositPaymentState.Failed -> DepositPaymentFailure(
                method = selectedPaymentMethod,
                reason = errorMessage.orEmpty(),
                onRetry = { onReset() },
                onChangeMethod = { onReset(); showMethodSheet = true },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showMethodSheet) {
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }, containerColor = Colors.Background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("결제수단 선택", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf("등록된 카드 ·••• 1234" to "기본 결제수단", "다른 카드로 결제" to "결제 단계에서 카드 선택").forEach { (method, description) ->
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).border(1.dp, Colors.Border, RoundedCornerShape(12.dp))
                            .clickable { selectedPaymentMethod = method; showMethodSheet = false }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (selectedPaymentMethod == method) "●" else "○", color = Colors.Navy, fontSize = 18.sp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(method, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(description, color = Colors.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepositPaymentForm(
    productName: String,
    bidAmount: Int,
    remainingSeconds: Int,
    paymentMethod: String,
    agreed: Boolean,
    onMethodClick: () -> Unit,
    onAgreementChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().height(92.dp).background(Colors.Surface, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Box(Modifier.size(68.dp).background(Colors.Border, RoundedCornerShape(10.dp)))
            Column(Modifier.fillMaxHeight().padding(start = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(productName, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                Text("입찰 금액 ${"%,d".format(bidAmount)}원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 15.sp)
                Text("마감까지 ${formatClock(remainingSeconds)}", color = Colors.Urgent, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Column(Modifier.fillMaxWidth().height(112.dp).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("이번 경매 보증금", color = Colors.Muted, fontSize = 12.sp, lineHeight = 14.sp)
            Text("${"%,d".format(maxOf(1_000L, bidAmount.toLong() / 10L))}원", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
            Text("최초 입찰가의 10% · 최소 1,000원", color = Colors.Muted, fontSize = 11.sp, lineHeight = 13.sp)
        }
        Text("결제수단", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().height(64.dp).border(1.dp, Colors.Border, RoundedCornerShape(12.dp))
                .clickable(onClick = onMethodClick).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▣", color = if (paymentMethod.isNotBlank()) Colors.Navy else Colors.Muted, fontSize = 22.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(paymentMethod.ifBlank { "결제수단을 선택해주세요" }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(if (paymentMethod.isNotBlank()) "결제수단 변경 가능" else "카드 · 간편결제", color = Colors.Muted, fontSize = 10.sp)
            }
            Text("›", color = Colors.Muted, fontSize = 24.sp)
        }
        Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("보증금은 이렇게 처리돼요", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("• 첫 입찰 시 한 번만 결제해요\n• 재입찰에는 추가 보증금이 없어요\n• 패찰 시 경매 종료 후 자동 반환돼요\n• 낙찰 시 거래·취소 정책에 따라 처리돼요", color = Colors.Muted, fontSize = 11.sp, lineHeight = 20.sp)
        }
        Row(Modifier.fillMaxWidth().height(44.dp).clickable { onAgreementChange(!agreed) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreed, onCheckedChange = onAgreementChange, colors = CheckboxDefaults.colors(checkedColor = Colors.MintInk))
            Text("보증금 결제·반환 정책에 동의해요", fontSize = 12.sp)
        }
    }
}

@Composable
private fun DepositPaymentAwaitingApproval(
    deposit: BidDeposit,
    onCheckStatus: () -> Unit,
    onChangeMethod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(104.dp))
        CircularProgressIndicator(color = Colors.Navy)
        Text("결제 승인을 기다리고 있어요", Modifier.padding(top = 28.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            if (deposit.paymentUrl != null) "열린 결제 페이지에서 결제를 마친 뒤\n아래 버튼으로 승인 상태를 확인해주세요."
            else "결제 요청은 준비됐지만 결제 페이지 주소가 없어요.\nPG 설정을 확인한 뒤 승인 상태를 확인해주세요.",
            Modifier.padding(top = 14.dp),
            color = Colors.Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        Column(Modifier.fillMaxWidth().padding(top = 36.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("결제 대기 정보", color = Colors.Muted, fontSize = 11.sp)
            Text("보증금 ${"%,d".format(deposit.amount ?: 0)}원", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("상태 ${deposit.status.ifBlank { "PENDING" }}", color = Colors.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(onCheckStatus, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("결제 승인 상태 확인", fontWeight = FontWeight.Bold) }
        TextButton(onChangeMethod) { Text("결제수단 다시 선택", color = Colors.Navy) }
    }
}

@Composable
private fun DepositPaymentFailure(method: String, reason: String, onRetry: () -> Unit, onChangeMethod: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(104.dp))
        Box(Modifier.size(88.dp).background(Color(0xFFFFE9E9), CircleShape), contentAlignment = Alignment.Center) {
            Text("!", color = Color(0xFFF5636E), fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
        Text("결제를 완료하지 못했어요", Modifier.padding(top = 28.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(reason.ifBlank { "결제 요청을 처리하지 못했습니다.\n결제수단을 확인한 뒤 다시 시도해주세요." }, Modifier.padding(top = 14.dp), color = Colors.Muted, fontSize = 14.sp, lineHeight = 22.sp)
        Column(Modifier.fillMaxWidth().padding(top = 36.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("실패 사유", color = Colors.Muted, fontSize = 11.sp)
            Text("결제 요청 실패", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("결제수단 · $method", color = Colors.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(onRetry, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("다시 결제하기", fontWeight = FontWeight.Bold) }
        TextButton(onChangeMethod) { Text("다른 결제수단 선택", color = Colors.Navy) }
    }
}

@Composable
private fun DepositPaymentSuccess(productName: String, bidAmount: Int, depositAmount: Long, onReturn: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(120.dp))
        Box(Modifier.size(88.dp).background(Colors.Mint, CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = Colors.MintInk, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
        Text("입찰이 접수됐어요", Modifier.padding(top = 28.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("보증금 ${"%,d".format(depositAmount)}원 결제가 완료됐어요.\n현재 입찰가는 ${"%,d".format(bidAmount)}원입니다.", Modifier.padding(top = 14.dp), color = Colors.Muted, fontSize = 14.sp, lineHeight = 22.sp)
        Column(Modifier.fillMaxWidth().padding(top = 48.dp).background(Color(0xFFF2F6FB), RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$productName · 입찰 완료", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("보증금은 패찰 시 반환돼요", color = Colors.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onReturn, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
            Text("경매로 돌아가기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}
