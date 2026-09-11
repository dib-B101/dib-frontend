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
import com.ssafy.dib.domain.auction.AuctionCommandResult
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun AuctionRegisterScreen(
    productId: String,
    result: AuctionCommandResult?,
    started: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onCreate: (Long, Long) -> Unit,
    onStart: () -> Unit,
    onOpenAuction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startPriceText by rememberSaveable { mutableStateOf("") }
    var auctionTime by rememberSaveable { mutableLongStateOf(3_600L) }
    val startPrice = startPriceText.toLongOrNull() ?: 0L
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(52.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(52.dp).clickable(onClick = onBack).wrapContentSize(), fontSize = 24.sp)
                Text("일반 경매 등록", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        if (result != null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (started) "✓" else "▣", color = Colors.MintInk, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(if (started) "경매를 시작했어요" else "경매 등록을 완료했어요", Modifier.padding(top = 16.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("경매 번호 ${result.auctionId}", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                if (!started) {
                    Text("등록 직후에는 예정 상태이며 판매자가 직접 시작할 수 있어요.", Modifier.padding(top = 18.dp), color = Colors.Muted, fontSize = 12.sp)
                    Button(onStart, Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("지금 경매 시작", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onOpenAuction, Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) { Text("경매 상세 보기", fontWeight = FontWeight.Bold) }
                }
                errorMessage?.let { Text(it, Modifier.padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("승인된 상품으로 경매를 등록해요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("상품 번호 $productId", color = Colors.Muted, fontSize = 12.sp)
                Text("시작가", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(startPriceText, { startPriceText = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), suffix = { Text("원") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                Text("경매 진행 시간", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1L to "1시간", 3L to "3시간", 6L to "6시간").forEach { (hours, label) ->
                        FilterChip(selected = auctionTime == hours * 3_600, onClick = { auctionTime = hours * 3_600 }, label = { Text(label) })
                    }
                }
                Text("등록된 경매는 예정 상태로 생성되며 시작 전까지만 조건을 수정하거나 취소할 수 있어요.", Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(16.dp), color = Colors.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onCreate(startPrice, auctionTime) }, enabled = startPrice > 0 && !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("경매 등록", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
