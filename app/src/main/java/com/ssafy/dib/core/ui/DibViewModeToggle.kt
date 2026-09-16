package com.ssafy.dib.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
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
            Surface(
                onClick = { onSelected(mode) },
                color = if (active) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = if (active) 1.dp else 0.dp,
                modifier = Modifier.defaultMinSize(minHeight = 42.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Image(
                        painterResource(if (mode == DibContentView.Grid) R.drawable.grid_view else R.drawable.list_view),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(if (active) Colors.Navy else Colors.Muted)
                    )
                    Text(
                        text = if (mode == DibContentView.Grid) "카드" else "목록",
                        color = if (active) Colors.Navy else Colors.Muted,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
