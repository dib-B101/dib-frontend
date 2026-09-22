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
import androidx.compose.ui.draw.clip
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
    sellerNickname: String?,
    sellerRating: Double?,
    sellerReviewCount: Int?,
    sellerTradeCount: Int?,
    activeCount: Int? = null,
    endedCount: Int? = null,
    showSampleContent: Boolean,
    onBack: () -> Unit,
    onReviewsClick: () -> Unit,
    onListingsClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매자 프로필", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(68.dp).background(Colors.NavySoft, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.seller), null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (showSampleContent) "seller01" else sellerNickname?.takeIf(String::isNotBlank) ?: "판매자",
                            color=Colors.Text,fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val sellerMeta = if (showSampleContent) {
                            "★ 4.8    ·  거래 32회"
                        } else {
                            listOfNotNull(
                                sellerRating
                                    ?.takeIf { (sellerReviewCount ?: 0) > 0 }
                                    ?.let { "★ %.1f (%d)".format(it, sellerReviewCount ?: 0) },
                                sellerTradeCount?.let { "거래 ${it}회" }
                            ).joinToString("    ·  ").ifBlank { "판매자 정보" }
                        }
                        Text(sellerMeta, color = Colors.Muted, fontSize = 13.sp)
                    }
                }
            }
            if (showSampleContent) {
                item { SectionTitle("판매 후기") }
                item {
                    Row(
                        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).clickable(onClick = onReviewsClick).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("후기 목록", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("전체 후기 보기", color = Colors.Navy, fontSize = 12.sp)
                        }
                        Image(painterResource(R.drawable.chevron_right),null,Modifier.size(18.dp),colorFilter=ColorFilter.tint(Colors.Muted))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onListingsClick), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("판매 내역", Modifier.weight(1f))
                        Row(verticalAlignment=Alignment.CenterVertically){Text("전체보기", color = Colors.Navy, fontSize = 12.sp);Image(painterResource(R.drawable.chevron_right),null,Modifier.size(14.dp),colorFilter=ColorFilter.tint(Colors.Navy))}
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SaleCard("판매중", null, Modifier.weight(1f).clickable(onClick = onListingsClick))
                        SaleCard("판매완료", null, Modifier.weight(1f).clickable(onClick = onListingsClick))
                    }
                }
            } else {
                item { SectionTitle("판매 후기") }
                item {
                    Row(
                        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).clickable(onClick = onReviewsClick).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("판매자 평점", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if ((sellerReviewCount ?: 0) > 0)
                                    "별점 %.1f · 평가 %d건".format(sellerRating ?: 0.0, sellerReviewCount ?: 0)
                                else "아직 받은 평가가 없어요",
                                color = Colors.Muted, fontSize = 12.sp
                            )
                        }
                        Image(painterResource(R.drawable.chevron_right),null,Modifier.size(18.dp),colorFilter=ColorFilter.tint(Colors.Muted))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onListingsClick), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("판매 내역", Modifier.weight(1f))
                        Row(verticalAlignment=Alignment.CenterVertically){Text("전체보기", color = Colors.Navy, fontSize = 12.sp);Image(painterResource(R.drawable.chevron_right),null,Modifier.size(14.dp),colorFilter=ColorFilter.tint(Colors.Navy))}
                    }
                }
                // 건수를 못 받아왔으면 카드를 그리지 않는다 (가짜 숫자를 남기지 않는다)
                if (activeCount != null || endedCount != null) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            activeCount?.let { SaleCard("판매중", it, Modifier.weight(1f).clickable(onClick = onListingsClick)) }
                            endedCount?.let { SaleCard("판매완료", it, Modifier.weight(1f).clickable(onClick = onListingsClick)) }
                        }
                    }
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
    Row(Modifier.fillMaxWidth().height(56.dp).background(Colors.Background).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun SaleCard(status: String, count: Int?, modifier: Modifier = Modifier) {
    Column(modifier.background(Colors.Background, RoundedCornerShape(14.dp))) {
        Box(Modifier.fillMaxWidth().height(94.dp).background(Colors.Surface, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
            // Surface 는 onClick 이 없어도 뒤로 터치를 안 넘긴다. 이 카드는 통째로 clickable 인데
            // 배지가 그 위를 덮고 있어서 배지를 누르면 아무 일도 안 일어났다. Box 로 바꾼다
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (status == "판매중") Color(0xFFEDF2FA) else Colors.Surface)
            ) {
                Text(status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(count?.let { "${it}건" } ?: "판매 상품", Modifier.fillMaxWidth().padding(10.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
