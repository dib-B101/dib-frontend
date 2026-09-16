package com.ssafy.dib.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class CategoryItem(val id: String, val badge: String, val name: String, val tint: Color, val surface: Color)

private val categories = listOf(
    CategoryItem("1", "D", "디지털기기", Colors.MintInk, Colors.MintSoft),
    CategoryItem("2", "L", "생활가전", Colors.Urgent, Colors.UrgentBackground),
    CategoryItem("3", "H", "가구·인테리어", Colors.Navy, Colors.NavySoft),
    CategoryItem("4", "S", "스포츠·레저", Colors.MintInk, Colors.MintSoft),
    CategoryItem("5", "F", "패션·잡화", Colors.Urgent, Colors.UrgentBackground),
    CategoryItem("6", "B", "뷰티", Colors.Navy, Colors.NavySoft),
    CategoryItem("7", "G", "취미·게임", Colors.MintInk, Colors.MintSoft),
    CategoryItem("8", "A", "예술·창작", Colors.Urgent, Colors.UrgentBackground)
)

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    remoteCategories: List<ProductCategory>?,
    remoteAuctions: List<HomeAuction>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCategories = remoteCategories?.mapIndexed { index, category ->
        val style = categories[index % categories.size]
        CategoryItem(category.categoryId, category.name.take(1), category.name, style.tint, style.surface)
    } ?: categories
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.background(Colors.Background)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedId != null) {
                    IconButton(onClick = { selectedId = null; selectedName = null }) {
                        Image(painterResource(R.drawable.back), "카테고리 목록으로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
                    }
                    Text(selectedName.orEmpty(), Modifier.weight(1f), color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                } else {
                    IconButton(onClick = onBack) {
                        Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
                    }
                    Text("카테고리", Modifier.weight(1f), color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNotificationsClick) {
                    Image(painterResource(R.drawable.notification), "알림", Modifier.size(24.dp), colorFilter = ColorFilter.tint(Colors.Text))
                }
            }
            HorizontalDivider(color = Colors.Border)
            }
        },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(50.dp).background(Colors.Search, RoundedCornerShape(16.dp))
                    .clickable(onClick = onSearchClick).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.search_full), "검색", Modifier.size(20.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                Text("상품과 작가를 검색해보세요", Modifier.padding(start = 10.dp), color = Colors.Muted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(if(selectedId == null) "전체 카테고리" else "진행 중인 경매", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (selectedId == null) Text("관심 있는 분야의 경매를 둘러보세요", Modifier.padding(top = 4.dp), color = Colors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            if (selectedId == null) {
                if (visibleCategories.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("카테고리를 불러올 수 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("검색에서 상품명으로 경매를 찾아보세요", color = Colors.Muted, fontSize = 12.sp)
                            OutlinedButton(onClick = onSearchClick) { Text("검색하기") }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(visibleCategories) { category ->
                            Column(
                                Modifier.height(96.dp).clickable { selectedId = category.id; selectedName = category.name; onCategorySelected(category.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(Modifier.size(56.dp).background(category.surface, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(category.badge, color = category.tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(category.name, Modifier.padding(top = 9.dp), color = Colors.Text, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                if (isLoading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
                    return@Column
                }
                if (errorMessage != null) {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") } }
                    return@Column
                }
                val visibleAuctions = remoteAuctions ?: allHomeAuctions.distinctBy(HomeAuction::id).filter { it.category.contains(selectedName.orEmpty().take(2)) }.ifEmpty { allHomeAuctions.distinctBy(HomeAuction::id).take(6) }
                Text("진행 중인 경매 ${visibleAuctions.size}개", color = Colors.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                if (visibleAuctions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("진행 중인 경매가 없어요.", color = Colors.Muted, fontSize = 13.sp) }
                    return@Column
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleAuctions) { auction ->
                        Column(
                            Modifier.background(Colors.Background, RoundedCornerShape(14.dp)).clickable { onProductClick(auction.id) }.padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.fillMaxWidth().height(132.dp))
                            Text(auction.name, Modifier.padding(horizontal = 10.dp), color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${auction.priceLabel} · ${remainingTimeLabel(auction.remainingSeconds)} 남음", Modifier.padding(horizontal = 10.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (hasNext || isLoadingMore || loadMoreError != null) {
                        item(key = "category-load-more", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(visibleAuctions.size, hasNext, isLoadingMore, loadMoreError) {
                                if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                            }
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                when {
                                    isLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                                    loadMoreError != null -> {
                                        Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                        OutlinedButton(onClick = onLoadMore, modifier = Modifier.padding(top = 6.dp)) { Text("더 불러오기") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
