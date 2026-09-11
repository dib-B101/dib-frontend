package com.ssafy.dib.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

enum class DibContentView { Grid, List }

@Composable
fun DibViewModeToggle(
    selected: DibContentView,
    onSelected: (DibContentView) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Colors.Surface, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DibContentView.entries.forEach { mode ->
            val active = mode == selected
            Text(
                text = if (mode == DibContentView.Grid) "▦ 카드" else "☰ 목록",
                modifier = Modifier
                    .background(if (active) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelected(mode) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                color = if (active) Colors.Navy else Colors.Muted,
                fontSize = 11.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
