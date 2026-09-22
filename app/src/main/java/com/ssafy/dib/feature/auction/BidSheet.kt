package com.ssafy.dib.feature.auction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

// 입찰 직전에 한 번 보여주는 안내. 상세 화면 본문에 따로 두지 않고 시트에서만 보여주며, 라이브 입찰 시트도 같은 문장을 쓴다
internal val BID_NOTICES = listOf(
    "입찰한 금액은 취소할 수 없어요.",
    "종료 15초 이내에 새 입찰이 들어오면 경매가 15초로 연장돼요.",
    "낙찰되면 등록된 카드로 낙찰가 전액이 자동 결제돼요.",
    "결제에 실패하면 거래 상세 내역에서 다시 결제할 수 있어요."
)

/** 입찰 금액 입력에 천 단위 쉼표를 보여준다. 상태에는 숫자만 두고 화면에만 쉼표를 그린다. */
internal class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.length <= 3) return TransformedText(text, OffsetMapping.Identity)
        val formatted = digits.reversed().chunked(3).joinToString(",").reversed()
        // 원본 offset 앞에 들어간 쉼표 개수. 쉼표는 오른쪽에서 세 자리마다(왼쪽 기준 i 번째 앞, (len - i) % 3 == 0) 들어간다
        fun commasBefore(offset: Int): Int = (1 until offset.coerceIn(0, digits.length)).count { (digits.length - it) % 3 == 0 }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, digits.length) + commasBefore(offset)
            override fun transformedToOriginal(offset: Int): Int {
                var original = 0
                while (original < digits.length && original + 1 + commasBefore(original + 1) <= offset) original++
                return original
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/**
 * 입찰 금액 입력 시트. 상품 상세와 홈 마감 임박 카드가 같은 시트를 쓴다.
 * 끝자리가 10원 단위가 아니면 막지 않고 올려서 그 금액으로 입찰한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionBidSheet(
    productName: String,
    currentPrice: Int,
    bidCount: Int,
    submissionError: String,
    onDismiss: () -> Unit,
    onContinue: (BidSubmission) -> Unit
) {
    val minimum = minimumBidAmount(currentPrice, bidCount)
    var amountText by rememberSaveable { mutableStateOf(minimum.toString()) }
    val typedAmount = amountText.toIntOrNull() ?: 0
    val amount = roundUpToBidUnit(typedAmount)
    val snapped = typedAmount > 0 && amount != typedAmount
    val valid = typedAmount > 0 && isValidBidAmount(amount, minimum)
    val thousands = remember { ThousandsSeparatorTransformation() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Colors.Border) }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(productName, color = Colors.Text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("현재가 ${"%,d".format(currentPrice)}원", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Image(painterResource(R.drawable.close), "닫기", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                }
            }
            if (submissionError.isNotBlank()) {
                Text(submissionError, Modifier.fillMaxWidth().background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp), color = Color(0xFFD1381F), fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
            }
            // 최소 금액은 라벨 바로 옆에 붙인다. 오른쪽 끝에 두면 라벨과 따로 놀았다
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("입찰 금액", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${"%,d".format(minimum)}원 이상", color = Colors.Muted, fontSize = 12.sp)
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(9) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("원", fontWeight = FontWeight.Bold) },
                isError = amountText.isNotEmpty() && !valid,
                supportingText = when {
                    amountText.isNotEmpty() && !valid -> {{ Text("최소 금액 이상으로 입력해주세요") }}
                    snapped -> {{ Text("10원 단위로 올려 ${"%,d".format(amount)}원으로 입찰돼요") }}
                    else -> null
                },
                visualTransformation = thousands,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(color = Colors.Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Colors.Navy, unfocusedBorderColor = Colors.Navy)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1_000, 5_000, 10_000).forEach { increment ->
                    Button(
                        onClick = { amountText = steppedBidAmount(amountText.toIntOrNull() ?: minimum, increment, minimum).toString() },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Surface, contentColor = Colors.Navy),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+%,d원".format(increment), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("입찰 전에 확인해 주세요", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                BID_NOTICES.forEach { notice -> Text("• $notice", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp) }
            }
            Button(
                onClick = { onContinue(BidSubmission(amount)) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
            ) {
                Text("${"%,d".format(amount)}원 입찰하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
