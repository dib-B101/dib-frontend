package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class SellerReview(val buyer: String, val age: String, val type: String, val body: String)
private data class SellerListing(val id: String, val name: String, val price: Int, val active: Boolean)

private val reviews = listOf(
    SellerReview("구매자A", "2일 전", "구매 후기", "상품 상태가 설명과 같고 포장도 꼼꼼했어요."),
    SellerReview("구매자B", "5일 전", "판매 후기", "응답이 빠르고 약속 시간을 잘 지켜주셨어요."),
    SellerReview("구매자C", "1주 전", "구매 후기", "배송이 빠르고 상품도 만족스러워요."),
    SellerReview("구매자D", "2주 전", "판매 후기", "친절하고 안전하게 거래했어요.")
)

private val listings = listOf(
    SellerListing("camera", "빈티지 필름 카메라", 34_500, true),
    SellerListing("headphones", "무선 헤드폰", 52_000, true),
    SellerListing("cross-bag", "가죽 크로스백", 28_500, false),
    SellerListing("retro-console", "레트로 게임기", 63_000, false)
)

/** Figma 01_Wireframe / 03M_Seller_Reviews. */
@Composable
fun SellerReviewsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf("전체", "구매 후기", "판매 후기")
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    val visibleReviews = reviews.filter { selectedFilter == 0 || it.type == filters[selectedFilter] }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매 후기", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("★ 4.8  ·  후기 32개", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item { Text("최근 거래 후기를 확인해보세요.", color = Colors.Muted, fontSize = 12.sp) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEachIndexed { index, label ->
                        FilterChip(label, selectedFilter == index) { selectedFilter = index }
                    }
                }
            }
            items(visibleReviews) { review -> ReviewItem(review) }
        }
    }
}

/** Figma 01_Wireframe / 03N_Seller_Listings. */
@Composable
fun SellerListingsScreen(onBack: () -> Unit, onProductClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf("전체", "경매중", "종료")
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    val visibleListings = listings.filter {
        selectedFilter == 0 || (selectedFilter == 1 && it.active) || (selectedFilter == 2 && !it.active)
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매 내역", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("판매 상품 12개", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEachIndexed { index, label ->
                        FilterChip(label, selectedFilter == index) { selectedFilter = index }
                    }
                }
            }
            items(visibleListings) { listing ->
                SellerListingCard(listing) { onProductClick(listing.id) }
            }
        }
    }
}

/** Figma 01_Wireframe / 03L2_Seller_Report. */
@Composable
fun SellerReportScreen(onBack: () -> Unit, onSubmitted: () -> Unit, modifier: Modifier = Modifier) {
    ReportFormScreen(
        title = "판매자 신고",
        heading = "판매자를 신고하는 이유를 선택해주세요",
        subtitle = "판매자의 거래 및 이용 행동과 관련된 사유를 선택해주세요.",
        reasons = listOf("비매너·욕설 등 부적절한 언행", "거래 약속 불이행", "사기 또는 외부 거래 유도", "반복적인 허위·부적절 판매", "기타"),
        onBack = onBack,
        onSubmitted = onSubmitted,
        modifier = modifier
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(32.dp).clickable(onClick = onClick),
        color = if (selected) Colors.Navy else Colors.Surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Colors.Background else Colors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReviewItem(review: SellerReview) {
    Row(Modifier.fillMaxWidth().height(88.dp).padding(top = 6.dp)) {
        Box(Modifier.size(36.dp).background(Colors.Surface, CircleShape))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(review.buyer, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(review.age, color = Colors.Muted, fontSize = 11.sp)
            }
            Text("★★★★★", fontSize = 12.sp)
            Text(review.body, color = Colors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SellerListingCard(listing: SellerListing, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(116.dp)
            .border(1.dp, Colors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(92.dp).background(Colors.Border, RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listing.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("%,d원".format(listing.price), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (listing.active) Text("3분 24초", Modifier.weight(1f), color = Colors.Urgent, fontSize = 12.sp)
                else Spacer(Modifier.weight(1f))
                Surface(color = if (listing.active) Colors.UrgentBackground else Colors.Surface, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        if (listing.active) "경매중" else "종료",
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (listing.active) Colors.Urgent else Colors.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
