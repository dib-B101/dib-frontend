package com.ssafy.dib.feature.auction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

data class BidSubmission(val amount: Int)

@Composable
fun BidDepositStatusNotice(depositPaid: Boolean, depositAmount: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = Colors.Search, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                if (depositPaid) "보증금 결제 완료" else "첫 입찰 보증금 ${"%,d".format(depositAmount)}원",
                color = Colors.Navy,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (depositPaid) "이 경매에서는 추가 결제 없이 재입찰할 수 있어요."
                else "다음 단계에서 보증금을 결제한 뒤 입찰을 진행해요. 패찰 시 보증금은 반환돼요.",
                modifier = Modifier.padding(top = 4.dp),
                color = Colors.MintInk,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
    }
}
