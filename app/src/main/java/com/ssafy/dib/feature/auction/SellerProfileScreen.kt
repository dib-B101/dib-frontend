package com.ssafy.dib.feature.auction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** Figma 01_Wireframe / 03H_Seller_Profile. */
@Composable
fun SellerProfileScreen(
    onBack: () -> Unit,
    onReviewsClick: () -> Unit,
    onListingsClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매자 프로필", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).background(Colors.Surface, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.seller), null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("seller01", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("★ 4.8    ·  거래 32회", color = Colors.Muted, fontSize = 13.sp)
                    }
                }
            }
            item { SectionTitle("판매 후기") }
            item {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp)).clickable(onClick = onReviewsClick).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("후기 목록", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("전체 후기 보기", color = Colors.Navy, fontSize = 12.sp)
                    }
                    Text("›", color = Colors.Muted, fontSize = 24.sp)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clickable(onClick = onListingsClick), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("판매 내역", Modifier.weight(1f))
                    Text("전체보기 ›", color = Colors.Navy, fontSize = 12.sp)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SaleCard("판매중", Modifier.weight(1f).clickable(onClick = onListingsClick))
                    SaleCard("판매완료", Modifier.weight(1f).clickable(onClick = onListingsClick))
                }
            }
            item { HorizontalDivider(color = Colors.Border) }
            item {
                Text(
                    "이 판매자 신고 · 차단",
                    Modifier.clickable(onClick = onReportClick).padding(vertical = 4.dp),
                    color = androidx.compose.ui.graphics.Color(0xFFAD3829),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
internal fun AuctionSubAppBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).background(Colors.Background).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Colors.Border)
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SaleCard(status: String, modifier: Modifier = Modifier) {
    Column(modifier.border(1.dp, Colors.Border, RoundedCornerShape(12.dp)).background(Colors.Background, RoundedCornerShape(12.dp))) {
        Box(Modifier.fillMaxWidth().height(94.dp).background(Colors.Surface, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
            Surface(color = if (status == "판매중") Color(0xFFEDF2FA) else Colors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Text(status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("판매 상품", Modifier.fillMaxWidth().padding(10.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
