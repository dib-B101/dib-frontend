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
            Text("상품을 먼저 등록하면 일반 경매와 Live에 사용할 수 있어요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(2.dp))
            CreateMenuItem(
                icon = R.drawable.nav_register_full,
                title = "상품 등록",
                description = "사진과 상품 정보를 등록하고 검수 대기",
                badge = "처음이라면 여기부터",
                onClick = onProductRegister
            )
            CreateMenuItem(
                icon = R.drawable.timer_outline,
                title = "일반 경매 만들기",
                description = "승인된 상품을 선택하고 가격과 기간 설정",
                badge = "승인 상품 필요",
                onClick = onAuctionRegister
            )
            CreateMenuItem(
                icon = R.drawable.nav_feed_full,
                title = "Live 방송 준비",
                description = "방송을 예약하고 상품을 최대 10개 편성",
                badge = "방송 시작 전 준비",
                onClick = onLivePrepare
            )
        }
    }
}

@Composable
private fun CreateMenuItem(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    badge: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).background(Colors.NavySoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(23.dp), colorFilter = ColorFilter.tint(Colors.Navy))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(badge, Modifier.background(Colors.MintSoft, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp), color = Colors.MintInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(description, color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Image(painterResource(R.drawable.chevron_right), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(Colors.Muted))
    }
}
