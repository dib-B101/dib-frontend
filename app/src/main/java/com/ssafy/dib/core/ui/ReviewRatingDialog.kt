package com.ssafy.dib.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/**
 * 거래 평가 — 별 0~5 개만 받는다. 코멘트는 받지 않기로 했다.
 *
 * 0 개도 유효한 점수다. "아직 안 고름" 과 구분하려고 초기값을 -1 로 두고,
 * 별을 한 번이라도 눌러야 확인 버튼이 열린다.
 */
@Composable
fun ReviewRatingDialog(
    productTitle: String,
    submitting: Boolean,
    errorMessage: String?,
    onSubmit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by rememberSaveable { mutableIntStateOf(-1) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("판매자는 어떠셨나요?") },
        text = {
            Column {
                Text(
                    "‘$productTitle’ 거래를 별점으로 평가해 주세요.",
                    fontSize = 13.sp,
                    color = Colors.Muted
                )
                Row(
                    Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (rating >= star) "★" else "☆",
                            fontSize = 30.sp,
                            color = if (rating >= star) Colors.Live else Colors.Border,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = !submitting) { rating = star }
                                .padding(2.dp)
                        )
                    }
                }
                // 별 하나도 아까운 거래가 있다. 0 점을 누를 방법이 없으면 1 점이 최저가 되어버린다
                TextButton(
                    onClick = { rating = 0 },
                    enabled = !submitting,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        if (rating == 0) "0점 선택됨" else "0점 주기",
                        fontSize = 11.sp,
                        color = if (rating == 0) Colors.Live else Colors.Muted,
                        fontWeight = if (rating == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
                errorMessage?.let {
                    Text(it, color = Colors.Urgent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (rating >= 0) onSubmit(rating) },
                enabled = rating >= 0 && !submitting
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Colors.Navy, strokeWidth = 2.dp)
                } else {
                    Text("평가 보내기", color = Colors.Navy, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("나중에") }
        }
    )
}

/** 판매자 평점 표시. 후기가 없으면 아무것도 그리지 않는다 — 가짜 숫자를 만들지 않기 위해서다. */
@Composable
fun SellerRatingLabel(
    rating: Double?,
    reviewCount: Int?,
    modifier: Modifier = Modifier,
    fontSize: Int = 12
) {
    if (rating == null || (reviewCount ?: 0) <= 0) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("★", color = Colors.Live, fontSize = fontSize.sp)
        Text(
            " ${String.format("%.1f", rating)} (${reviewCount})",
            color = Colors.Muted,
            fontSize = fontSize.sp
        )
    }
}
