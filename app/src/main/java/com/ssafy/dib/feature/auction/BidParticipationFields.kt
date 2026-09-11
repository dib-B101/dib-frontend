package com.ssafy.dib.feature.auction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

data class RegisteredPaymentMethod(
    val id: String,
    val label: String,
    val description: String
)

data class RegisteredBidAddress(
    val id: String,
    val label: String,
    val description: String
)

data class BidSubmission(
    val amount: Int,
    val paymentMethodId: String,
    val addressId: String
)

val samplePaymentMethods = listOf(
    RegisteredPaymentMethod("card-1234", "신한카드 ·••• 1234", "기본 결제수단"),
    RegisteredPaymentMethod("card-5678", "우리카드 ·••• 5678", "등록 결제수단")
)

val sampleBidAddresses = listOf(
    RegisteredBidAddress("address-home", "집", "부산광역시 동래구 중앙대로 000"),
    RegisteredBidAddress("address-office", "회사", "부산광역시 부산진구 중앙대로 000")
)

@Composable
fun BidParticipationFields(
    selectedPaymentMethodId: String,
    onPaymentMethodSelected: (String) -> Unit,
    selectedAddressId: String,
    onAddressSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("자동 결제수단", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        samplePaymentMethods.forEach { method ->
            SelectionRow(
                title = method.label,
                description = method.description,
                selected = method.id == selectedPaymentMethodId,
                onClick = { onPaymentMethodSelected(method.id) }
            )
        }
        Text("낙찰 배송지", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        sampleBidAddresses.forEach { address ->
            SelectionRow(
                title = address.label,
                description = address.description,
                selected = address.id == selectedAddressId,
                onClick = { onAddressSelected(address.id) }
            )
        }
        Text(
            "낙찰되면 선택한 결제수단으로 낙찰가 전액을 자동 결제하고, 선택한 배송지를 주문에 저장해요.",
            color = Colors.Muted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SelectionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFFE8FAF5) else Color.White,
        border = BorderStroke(1.dp, if (selected) Color(0xFF61D1B2) else Colors.Border)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (selected) "●" else "○", color = if (selected) Colors.Navy else Colors.Muted)
            Column(Modifier.weight(1f)) {
                Text(title, color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Colors.Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
