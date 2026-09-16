package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibDurationWheelPicker
import com.ssafy.dib.domain.auction.AuctionCommandResult
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun AuctionRegisterScreen(
    productId: String,
    result: AuctionCommandResult?,
    started: Boolean,
    cancelled: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onCreate: (Long, Long) -> Unit,
    onUpdate: (Long, Long) -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit,
    onOpenAuction: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startPriceText by rememberSaveable { mutableStateOf("") }
    var auctionTime by rememberSaveable { mutableLongStateOf(300L) }
    var editingCreatedAuction by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
    var validationRequested by rememberSaveable { mutableStateOf(false) }
    val startPrice = startPriceText.toLongOrNull() ?: 0L
    val startPriceValid = isValidAuctionStartPrice(startPrice)
    val auctionTimeValid = auctionTime >= 300L
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("일반 경매 등록", onBack) }
    ) { padding ->
        if (cancelled) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AuctionResultBadge("취소", success = false)
                Text("경매를 취소했어요", Modifier.padding(top = 20.dp), color = Colors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("상품은 다시 경매에 등록할 수 있어요.", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onFinish, Modifier.fillMaxWidth().padding(top = 28.dp).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(15.dp)) { Text("내 등록 상품으로 돌아가기", fontWeight = FontWeight.Bold) }
            }
        } else if (result != null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AuctionResultBadge(if (started) "시작" else "등록", success = true)
                Text(if (started) "경매를 시작했어요" else "경매 등록을 완료했어요", Modifier.padding(top = 20.dp), color = Colors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("경매 번호 ${result.auctionId}", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                if (!started) {
                    Text("등록 직후에는 예정 상태이며 판매자가 직접 시작할 수 있어요.", Modifier.padding(top = 18.dp), color = Colors.Muted, fontSize = 12.sp)
                    if (editingCreatedAuction) {
                        OutlinedTextField(startPriceText, { startPriceText = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth().padding(top = 18.dp), label = { Text("시작가") }, suffix = { Text("원") }, supportingText = { Text("최소 1,000원") }, isError = startPriceText.isNotBlank() && !startPriceValid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                        DibDurationWheelPicker(auctionTime, { auctionTime = it }, Modifier.padding(top = 8.dp))
                        if (!auctionTimeValid) Text("경매 시간은 5분 이상 설정해주세요.", color = Colors.Urgent, fontSize = 11.sp)
                        Button({ onUpdate(startPrice, auctionTime); editingCreatedAuction = false }, Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp), enabled = startPriceValid && auctionTimeValid && !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) { Text("변경사항 저장", fontWeight = FontWeight.Bold) }
                    }
                    Button(onStart, Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(15.dp)) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("지금 경매 시작", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton({ editingCreatedAuction = !editingCreatedAuction }, Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp), enabled = !isLoading, shape = RoundedCornerShape(15.dp)) { Text(if (editingCreatedAuction) "수정 닫기" else "경매 조건 수정", color = Colors.Navy, fontWeight = FontWeight.Bold) }
                    TextButton({ showCancelConfirmation = true }, Modifier.fillMaxWidth(), enabled = !isLoading) { Text("경매 취소", color = Colors.Urgent) }
                } else {
                    Button(onOpenAuction, Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(15.dp)) { Text("경매 상세 보기", fontWeight = FontWeight.Bold) }
                }
                errorMessage?.let { Text(it, Modifier.padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("경매 조건을\n설정해주세요", color = Colors.Text, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Text("승인된 상품 번호 $productId", color = Colors.Muted, fontSize = 13.sp)
                Text("시작가", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = startPriceText,
                    onValueChange = { startPriceText = it.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("원") },
                    supportingText = { Text(if (validationRequested && !startPriceValid) "시작가는 1,000원 이상 입력해주세요" else "최소 1,000원") },
                    isError = validationRequested && !startPriceValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Colors.Background,
                        unfocusedContainerColor = Colors.Background,
                        unfocusedBorderColor = Colors.Border,
                        focusedBorderColor = Colors.Navy
                    )
                )
                Text("경매 진행 시간", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                DibDurationWheelPicker(auctionTime, { auctionTime = it })
                if (validationRequested && !auctionTimeValid) Text("경매 시간은 5분 이상 설정해주세요.", color = Colors.Urgent, fontSize = 11.sp)
                Text("등록된 경매는 예정 상태로 생성돼요. 시작 전까지 조건을 수정하거나 취소할 수 있어요.", Modifier.fillMaxWidth().background(Colors.NavySoft, RoundedCornerShape(15.dp)).padding(16.dp), color = Colors.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (startPriceValid && auctionTimeValid) onCreate(startPrice, auctionTime) else validationRequested = true },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (startPriceValid && auctionTimeValid) Colors.Navy else Colors.Border,
                        contentColor = if (startPriceValid && auctionTimeValid) Color.White else Colors.Muted
                    )
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("경매 등록", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("예정 경매를 취소할까요?") },
            text = { Text("취소 후 상품은 다른 경매에 다시 등록할 수 있어요.") },
            confirmButton = { TextButton({ showCancelConfirmation = false; onCancel() }) { Text("경매 취소", color = Colors.Urgent) } },
            dismissButton = { TextButton({ showCancelConfirmation = false }) { Text("유지") } }
        )
    }
}

@Composable
private fun AuctionResultBadge(label: String, success: Boolean) {
    Surface(
        modifier = Modifier.size(78.dp),
        color = if (success) Colors.MintSoft else Colors.Surface,
        shape = androidx.compose.foundation.shape.CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (success) Colors.MintInk else Colors.Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun isValidAuctionStartPrice(startPrice: Long): Boolean = startPrice >= 1_000L
