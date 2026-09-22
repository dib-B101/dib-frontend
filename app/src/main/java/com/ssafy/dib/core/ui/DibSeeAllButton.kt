package com.ssafy.dib.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** "전체 보기 ›" 류 버튼. 홈 섹션·판매자 프로필 등 어디서나 같은 크기·색으로 보이게 한 곳에 둔다. */
@Composable
fun DibSeeAllButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "전체 보기") {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Colors.Navy, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(Colors.Navy))
    }
}
