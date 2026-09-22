package com.ssafy.dib.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** 하단 탭 가운데 + 버튼의 등록 메뉴. 설명·배지 없이 제목만 둔다 — 각 흐름의 안내는 들어간 화면이 한다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DibCreateMenuSheet(
    onDismiss: () -> Unit,
    onProductRegister: () -> Unit,
    onAuctionRegister: () -> Unit,
    onLivePrepare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Colors.Canvas,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(36.dp, 4.dp).background(Colors.Border, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
            Text("무엇을 시작할까요?", Modifier.padding(top = 8.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            CreateMenuItem(R.drawable.nav_register_full, "상품 등록", onProductRegister)
            CreateMenuItem(R.drawable.timer_outline, "일반 경매 시작", onAuctionRegister)
            CreateMenuItem(R.drawable.nav_feed_full, "Live 방송 준비", onLivePrepare)
        }
    }
}

@Composable
private fun CreateMenuItem(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).background(Colors.NavySoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(23.dp), colorFilter = ColorFilter.tint(Colors.Navy))
        }
        Text(title, Modifier.weight(1f), color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(Colors.Muted))
    }
}
