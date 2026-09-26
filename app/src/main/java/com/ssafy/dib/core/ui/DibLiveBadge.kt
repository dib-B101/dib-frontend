package com.ssafy.dib.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 경매 상세와 홈 LIVE 이미지에 공통으로 쓰는 방송 배지. */
@Composable
fun DibLiveBadge(modifier: Modifier = Modifier, label: String = "실시간 경매") {
    Row(
        modifier.shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE43D4B))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Canvas(Modifier.size(12.dp)) {
            val stroke = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.White, radius = 1.5.dp.toPx(), center = center)
            for (radius in listOf(3.5.dp.toPx(), 5.5.dp.toPx())) {
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(center.x - radius, center.y - radius)
                drawArc(Color.White, 125f, 110f, false, topLeft, arcSize, style = stroke)
                drawArc(Color.White, -55f, 110f, false, topLeft, arcSize, style = stroke)
            }
        }
        Text(label, color = Color.White, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold)
    }
}
