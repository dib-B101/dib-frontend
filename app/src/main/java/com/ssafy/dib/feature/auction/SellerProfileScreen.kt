package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibProfileAvatar
import com.ssafy.dib.core.ui.DibReportButton
import com.ssafy.dib.core.ui.DibSeeAllButton
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.ui.theme.WireframeColors as Colors

// 프로필 화면의 판매 내역 줄에는 이만큼만 보여주고, 나머지는 전체 보기로 넘긴다
private const val PROFILE_LISTING_LIMIT = 5

private val sampleSellerListings = listOf(
    SellerListing("sample-1", "빈티지 필름 카메라", null, 34_500, 5, "ACTIVE"),
    SellerListing("sample-2", "달빛 유약 머그컵", null, 30_000, 0, "SCHEDULED"),
    SellerListing("sample-3", "무선 헤드폰", null, 52_000, 7, "ENDED")
)

/** Figma 01_Wireframe / 03H_Seller_Profile. */
@Composable
fun SellerProfileScreen(
    sellerNickname: String?,
    sellerProfileImageUrl: String?,
    sellerRating: Double?,
    sellerReviewCount: Int?,
    sellerTradeCount: Int?,
    // null 이면 아직 불러오는 중. 빈 목록이면 판매 내역이 없는 것
    listings: List<SellerListing>?,
    showSampleContent: Boolean,
    onBack: () -> Unit,
    onReviewsClick: () -> Unit,
    onListingsClick: () -> Unit,
    onProductClick: (SellerListing) -> Unit,
    onReportClick: () -> Unit,
    // 내 프로필이면 신고 버튼을 감춘다. 서버도 SELF_REPORT_NOT_ALLOWED 로 막지만 버튼이 보이면 눌러보고 나서야 안다
    isOwnProfile: Boolean = false,
    modifier: Modifier = Modifier
) {
    val nickname = if (showSampleContent) "seller01" else sellerNickname?.takeIf(String::isNotBlank) ?: "판매자"
    val tradeCount = if (showSampleContent) 32 else sellerTradeCount
    val rating = if (showSampleContent) 4.8 else sellerRating
    val reviewCount = if (showSampleContent) 12 else sellerReviewCount
    val visibleListings = if (showSampleContent) sampleSellerListings else listings
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매자 프로필", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp)
        ) {
            item { SellerHeaderCard(nickname, sellerProfileImageUrl, tradeCount, rating, reviewCount) }
            item { SellerReviewsCard(rating, reviewCount, onReviewsClick) }
            item { SellerListingsSection(visibleListings, onListingsClick, onProductClick) }
            if (!isOwnProfile) item { DibReportButton("이 판매자 신고 · 차단", onClick = onReportClick, Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
internal fun AuctionSubAppBar(title: String, onBack: () -> Unit) {
    DibSubAppBar(title, onBack)
}

/** 프로필 사진·닉네임·거래 횟수·평점을 카드 하나에 모은다 */
@Composable
private fun SellerHeaderCard(nickname: String, profileImageUrl: String?, tradeCount: Int?, rating: Double?, reviewCount: Int?) {
    ProfileCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DibProfileAvatar(profileImageUrl, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(nickname, color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("거래 ${tradeCount ?: 0}회")
                    // 후기가 0건이면 평점을 만들어 보여주지 않는다
                    if (rating != null && (reviewCount ?: 0) > 0) StatChip("★ %.1f (%d)".format(rating, reviewCount ?: 0), highlight = true)
                    else StatChip("후기 없음")
                }
            }
        }
    }
}

/** 판매 후기 카드. 후기 목록 API 가 아직 없어 평점 요약만 보여주고, 전체 보기는 후기 화면으로 보낸다 */
@Composable
private fun SellerReviewsCard(rating: Double?, reviewCount: Int?, onReviewsClick: () -> Unit) {
    ProfileCard {
        SectionRow("판매 후기", onReviewsClick)
        if (rating != null && (reviewCount ?: 0) > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("%.1f".format(rating), color = Colors.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((1..5).joinToString("") { if (rating >= it - 0.25) "★" else "☆" }, color = Colors.Star, fontSize = 16.sp)
                    Text("후기 ${reviewCount}건", color = Colors.Muted, fontSize = 12.sp)
                }
            }
        } else {
            Text("아직 받은 후기가 없어요", color = Colors.Muted, fontSize = 13.sp)
        }
    }
}

/** 판매 내역. 가로 스크롤로 최대 5개, 나머지는 전체 보기 */
@Composable
private fun SellerListingsSection(listings: List<SellerListing>?, onListingsClick: () -> Unit, onProductClick: (SellerListing) -> Unit) {
    ProfileCard {
        SectionRow("판매 내역", onListingsClick)
        when {
            listings == null -> Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Colors.Navy, strokeWidth = 2.dp)
            }
            listings.isEmpty() -> Text("아직 판매 내역이 없어요", color = Colors.Muted, fontSize = 13.sp)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listings.take(PROFILE_LISTING_LIMIT), key = { it.auctionId ?: it.productId }) { listing ->
                    SellerListingTile(listing) { onProductClick(listing) }
                }
            }
        }
    }
}

@Composable
private fun SellerListingTile(listing: SellerListing, onClick: () -> Unit) {
    Column(
        Modifier.width(128.dp).clip(RoundedCornerShape(14.dp)).background(Colors.Canvas).clickable(onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Colors.Image)) {
            DibNetworkImage(listing.thumbnailUrl, listing.title, Modifier.fillMaxSize())
            ListingStatusBadge(listing.status, Modifier.align(Alignment.TopStart).padding(5.dp))
        }
        Text(listing.title, color = Colors.Text, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("%,d원".format(listing.currentPrice), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("입찰 ${listing.bidCount}회", color = Colors.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ListingStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, ink, surface) = when (status) {
        "ACTIVE" -> Triple("경매중", Colors.Urgent, Colors.UrgentBackground)
        "SCHEDULED" -> Triple("예정", Colors.Navy, Colors.NavySoft)
        "ENDED" -> Triple("종료", Colors.Muted, Colors.Surface)
        else -> Triple("취소", Colors.Muted, Colors.Surface)
    }
    Surface(modifier, color = surface, shape = RoundedCornerShape(7.dp)) {
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionRow(title: String, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        DibSeeAllButton(onSeeAll)
    }
}

@Composable
private fun StatChip(text: String, highlight: Boolean = false) {
    Surface(color = if (highlight) Colors.NavySoft else Colors.Surface, shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (highlight) Colors.Navy else Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(18.dp)).border(1.dp, Colors.Border, RoundedCornerShape(18.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
