package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.ui.theme.WireframeColors as Colors

data class SellerListing(
    val productId: String,
    val title: String,
    val thumbnailUrl: String?,
    val currentPrice: Int,
    val bidCount: Int,
    val status: String,
    // 상세 화면 라우트는 auctionId 를 받는다. 예전 데이터엔 없을 수 있어 null 이면 productId 로 간다
    val auctionId: String? = null
)

/** 프로필·판매 내역에서 진행 중 → 예정 → 종료 순으로 보여주기 위한 정렬 키 */
/**
 * 판매자 프로필에 보여줄 경매만 남긴다.
 *
 * 판매자 경매 API 는 상품 검수 상태를 주지 않아, 등록이 거절된 상품의 예정 경매도 "예정"으로 보였다.
 * 판매자 상품 API 는 검수를 통과한(REGISTERED·ON_AUCTION) 상품만 주므로, 진행·예정 경매는 그 목록에 있는 상품일 때만 보여준다.
 * 끝난 경매(ENDED 등)는 상품이 SOLD 로 바뀌어 그 목록에서 빠지므로 그대로 둔다.
 * 상품 목록을 못 받았으면(null) 거르지 않는다 — 판매 내역이 통째로 사라지는 것보다 낫다.
 */
internal fun <T> List<T>.visibleSellerAuctions(
    approvedProductIds: Set<String>?,
    status: (T) -> String,
    productId: (T) -> String
): List<T> {
    if (approvedProductIds == null) return this
    return filter { item ->
        status(item).uppercase() !in setOf("ACTIVE", "SCHEDULED") || productId(item) in approvedProductIds
    }
}

internal fun sellerListingOrder(status: String): Int = when (status) {
    "ACTIVE" -> 0
    "SCHEDULED" -> 1
    else -> 2
}

/** Figma 01_Wireframe / 03M_Seller_Reviews. 후기 API 가 아직 없어 빈 상태만 보여준다. */
@Composable
fun SellerReviewsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매 후기", onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("아직 후기 기능이 준비 중이에요", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "거래 후기가 공개되면 이곳에서 확인할 수 있어요.",
                Modifier.padding(top = 8.dp),
                color = Colors.Muted,
                fontSize = 12.sp
            )
        }
    }
}

/** Figma 01_Wireframe / 03N_Seller_Listings. */
@Composable
fun SellerListingsScreen(
    listings: List<SellerListing>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("전체", "진행중", "예정", "종료")
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    val all = listings.orEmpty()
    val visibleListings = all.filter {
        when (selectedFilter) {
            1 -> it.status == "ACTIVE"
            2 -> it.status == "SCHEDULED"
            3 -> it.status == "ENDED"
            else -> true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("판매 내역", onBack) }
    ) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Colors.Navy)
            }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                OutlinedButton(onRetry, Modifier.padding(top = 12.dp)) { Text("다시 불러오기") }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text("판매 상품 ${all.size}개", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                item {
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filters.forEachIndexed { index, label ->
                            FilterChip(label, selectedFilter == index) { selectedFilter = index }
                        }
                    }
                }
                if (visibleListings.isEmpty()) {
                    item {
                        Text(
                            if (all.isEmpty()) "아직 등록된 판매 상품이 없어요" else "해당 조건의 판매 상품이 없어요",
                            Modifier.padding(top = 40.dp),
                            color = Colors.Muted,
                            fontSize = 13.sp
                        )
                    }
                }
                items(visibleListings) { listing ->
                    SellerListingCard(listing) { onProductClick(listing.auctionId ?: listing.productId) }
                }
            }
        }
        }
    }
}

/** Figma 01_Wireframe / 03L2_Seller_Report. */
@Composable
fun SellerReportScreen(
    onBack: () -> Unit,
    submitted: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReportFormScreen(
        title = "판매자 신고",
        heading = "판매자를 신고하는 이유를 선택해주세요",
        subtitle = "판매자의 거래 및 이용 행동과 관련된 사유를 선택해주세요.",
        reasons = listOf("비매너·욕설 등 부적절한 언행", "거래 약속 불이행", "사기 또는 외부 거래 유도", "반복적인 허위·부적절 판매", "기타"),
        onBack = onBack,
        submitted = submitted,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        onSubmit = onSubmit,
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
private fun SellerListingCard(listing: SellerListing, onClick: () -> Unit) {
    val active = listing.status == "ACTIVE"
    val statusLabel = when (listing.status) {
        "ACTIVE" -> "경매중"
        "SCHEDULED" -> "예정"
        "ENDED" -> "종료"
        "CANCELED" -> "취소"
        else -> "대기"
    }
    Row(
        Modifier.fillMaxWidth().height(118.dp).background(Colors.Background, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DibNetworkImage(listing.thumbnailUrl, listing.title, Modifier.size(92.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listing.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("%,d원".format(listing.currentPrice), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("입찰 ${listing.bidCount}회", Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp)
                Surface(color = if (active) Colors.UrgentBackground else Colors.Surface, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        statusLabel,
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (active) Colors.Urgent else Colors.Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
