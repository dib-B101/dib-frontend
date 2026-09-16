package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductDetail
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** A product-only detail used when an API response does not include an auction identifier. */
@Composable
fun ProductOverviewScreen(
    product: ProductDetail?,
    loading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSellerClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentColor = Colors.Text,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Navy)) }
                Text("상품 정보", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        when {
            loading && product == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Colors.Navy)
            }
            product == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(errorMessage ?: "상품 정보를 불러오지 못했어요.", color = Colors.Muted, fontSize = 13.sp)
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("다시 불러오기") }
                }
            }
            else -> ProductOverviewContent(product, onSellerClick, onImageClick, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProductOverviewContent(
    product: ProductDetail,
    onSellerClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val images = product.imageUrls.ifEmpty { listOfNotNull(product.thumbnailUrl) }
    val pageCount = images.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    LazyColumn(modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Colors.Surface), contentAlignment = Alignment.Center) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    DibNetworkImage(
                        images.getOrNull(page),
                        product.title,
                        Modifier.fillMaxSize().clickable { onImageClick(page) }
                    )
                }
                if (pageCount > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / $pageCount",
                        Modifier.align(Alignment.BottomEnd).padding(14.dp).background(Colors.Navy.copy(alpha = .72f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Colors.Background,
                        fontSize = 11.sp
                    )
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(productConditionLabel(product.condition), Modifier.background(Colors.Mint.copy(alpha = .2f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp), color = Colors.MintInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(productStatusLabel(product.status), color = Colors.Muted, fontSize = 11.sp)
                }
                Text(product.title, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = Colors.Navy)
                product.marketPrice?.let { Text("시세 ${"%,d".format(it)}원", color = Colors.Muted, fontSize = 13.sp) }
                HorizontalDivider(color = Colors.Border)
                Text("상품 설명", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(product.description.ifBlank { "등록된 상품 설명이 없어요." }, color = Colors.Text, fontSize = 14.sp, lineHeight = 22.sp)
                product.modelName?.let { ProductAttribute("모델명", it) }
                product.releaseYear?.let { ProductAttribute("출시 연도", "${it}년") }
            }
        }
        if (product.memberId.isNotBlank()) {
            item {
                HorizontalDivider(color = Colors.Border)
                Row(
                    Modifier.fillMaxWidth().clickable { onSellerClick(product.memberId) }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Colors.Mint.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                        Text(product.sellerNickname?.take(1) ?: "판", color = Colors.Navy, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.sellerNickname ?: "판매자", fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(product.sellerRating?.let { "평점 $it" }, product.sellerTradeCount?.let { "거래 ${it}회" }).joinToString(" · ").ifBlank { "판매자 정보 보기" }, color = Colors.Muted, fontSize = 11.sp)
                    }
                    Image(painterResource(R.drawable.chevron_right),null,Modifier.size(18.dp),colorFilter=ColorFilter.tint(Colors.Muted))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProductAttribute(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(92.dp), color = Colors.Muted, fontSize = 13.sp)
        Text(value, color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun productConditionLabel(condition: String): String = when (condition.uppercase()) {
    "NEW" -> "새 상품"
    "LIKE_NEW" -> "거의 새 상품"
    "GOOD" -> "좋음"
    "NORMAL" -> "보통"
    "BAD", "FAIR" -> "사용감 있음"
    else -> condition.ifBlank { "상태 미정" }
}

private fun productStatusLabel(status: String): String = when (status.uppercase()) {
    "REGISTERED" -> "등록 완료"
    "PENDING" -> "검수 중"
    "REJECTED" -> "등록 반려"
    "SOLD" -> "판매 완료"
    else -> status
}
