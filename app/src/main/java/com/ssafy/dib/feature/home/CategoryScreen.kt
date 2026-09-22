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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibNotificationBell
import com.ssafy.dib.core.ui.DibSearchBar
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class CategoryItem(val id: String, @param:DrawableRes val icon: Int, val name: String, val tint: Color, val surface: Color, val order: Int)

// 비슷한 성격의 카테고리는 같은 색을 쓰고 붙어서 나오게 묶음 순서(order)를 둔다
private data class CategoryStyle(val order: Int, @param:DrawableRes val icon: Int, val tint: Color, val surface: Color)

private val TechInk = Colors.Navy
private val TechSoft = Colors.NavySoft
private val HomeInk = Color(0xFF8A5A2B)
private val HomeSoft = Color(0xFFF6EFE7)
private val FashionInk = Color(0xFFB53A6E)
private val FashionSoft = Color(0xFFFBEAF1)
private val ActiveInk = Colors.MintInk
private val ActiveSoft = Colors.MintSoft
private val HobbyInk = Color(0xFF6D4AC2)
private val HobbySoft = Color(0xFFF0EBFA)
private val KidsInk = Color(0xFFD98A16)
private val KidsSoft = Color(0xFFFFF4E0)
private val FoodInk = Colors.Urgent
private val FoodSoft = Colors.UrgentBackground
private val NatureInk = Color(0xFF2E8B57)
private val NatureSoft = Color(0xFFE9F6EE)
private val TicketInk = Color(0xFF1F7AB8)
private val TicketSoft = Color(0xFFE8F3FB)

private fun String.containsAny(vararg keywords: String) = keywords.any { contains(it) }

// 서버 카테고리 이름은 배포 DB(디지털기기·패션·잡화 …)와 시드(디지털·여성의류 …)가 조금씩 달라 키워드로 맞춘다.
// when 순서가 우선순위다: "생활가전" 은 가전으로, "유아도서" 는 유아로, "건강기능식품" 은 건강으로 간다
private fun categoryStyle(name: String): CategoryStyle = when {
    name.containsAny("디지털", "전자", "기기") -> CategoryStyle(0, R.drawable.category_digital, TechInk, TechSoft)
    name.containsAny("가전") -> CategoryStyle(1, R.drawable.category_home, TechInk, TechSoft)
    name.containsAny("가구", "인테리어") -> CategoryStyle(2, R.drawable.category_furniture, HomeInk, HomeSoft)
    name.containsAny("주방", "생활") -> CategoryStyle(3, R.drawable.category_kitchen, HomeInk, HomeSoft)
    name.containsAny("의류", "패션") -> CategoryStyle(4, R.drawable.category_fashion, FashionInk, FashionSoft)
    name.containsAny("잡화", "가방", "신발") -> CategoryStyle(5, R.drawable.category_bag, FashionInk, FashionSoft)
    name.containsAny("뷰티", "미용") -> CategoryStyle(6, R.drawable.category_beauty, FashionInk, FashionSoft)
    name.containsAny("스포츠", "레저") -> CategoryStyle(7, R.drawable.category_sports, ActiveInk, ActiveSoft)
    name.containsAny("취미", "게임", "음반") -> CategoryStyle(8, R.drawable.category_game, HobbyInk, HobbySoft)
    name.containsAny("예술", "창작") -> CategoryStyle(9, R.drawable.category_art, HobbyInk, HobbySoft)
    name.containsAny("유아", "아동", "키즈") -> CategoryStyle(10, R.drawable.category_kids, KidsInk, KidsSoft)
    name.containsAny("도서", "책") -> CategoryStyle(11, R.drawable.category_book, KidsInk, KidsSoft)
    name.containsAny("건강") -> CategoryStyle(12, R.drawable.category_health, FoodInk, FoodSoft)
    name.containsAny("식품", "음식") -> CategoryStyle(13, R.drawable.category_food, FoodInk, FoodSoft)
    name.containsAny("반려", "펫") -> CategoryStyle(14, R.drawable.category_pet, NatureInk, NatureSoft)
    name.containsAny("식물", "플랜트") -> CategoryStyle(15, R.drawable.category_plant, NatureInk, NatureSoft)
    name.containsAny("티켓", "교환권") -> CategoryStyle(16, R.drawable.category_ticket, TicketInk, TicketSoft)
    name.containsAny("쿠폰") -> CategoryStyle(17, R.drawable.category_coupon, TicketInk, TicketSoft)
    else -> CategoryStyle(99, R.drawable.category_etc, Colors.Muted, Colors.Surface)
}

private fun String.toCategoryItem(id: String): CategoryItem {
    val style = categoryStyle(this)
    return CategoryItem(id, style.icon, this, style.tint, style.surface, style.order)
}

// 서버 카테고리(GET /api/v1/categories)가 오기 전이거나 실패했을 때만 쓰는 대체 목록
private val categories = listOf("디지털기기", "생활가전", "가구·인테리어", "스포츠·레저", "패션·잡화", "뷰티", "취미·게임", "예술·창작")
    .mapIndexed { index, name -> name.toCategoryItem((index + 1).toString()) }
    .sortedBy(CategoryItem::order)

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
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
    val auctionGridState = rememberLazyGridState()
    // 같은 묶음끼리 붙도록 정렬한다. sortedBy 는 안정 정렬이라 묶음 안에서는 서버 순서가 유지된다
    val visibleCategories = remoteCategories?.takeIf { it.isNotEmpty() }
        ?.map { category -> category.name.toCategoryItem(category.categoryId) }
        ?.sortedBy(CategoryItem::order)
        ?: categories
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
                DibNotificationBell()
            }
            HorizontalDivider(color = Colors.Border)
            }
        },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        DibPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(12.dp))
            // 홈과 같은 검색 상자
            DibSearchBar("어떤 상품을 찾고 있나요?", onSearchClick)
            Spacer(Modifier.height(24.dp))
            Text(if(selectedId == null) "전체 카테고리" else "진행·예정 경매", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                                    Image(
                                        painterResource(category.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(25.dp),
                                        colorFilter = ColorFilter.tint(category.tint)
                                    )
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
                if (visibleAuctions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("진행 중이거나 예정된 경매가 없어요.", color = Colors.Muted, fontSize = 13.sp) }
                    return@Column
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = auctionGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleAuctions) { auction ->
                        // 홈과 같은 상품 카드. 이 화면은 찜 상태를 모르므로 하트는 그리지 않는다
                        HomeAuctionCard(auction, favorite = false, onFavorite = null, onClick = { onProductClick(auction.id) }, showStatus = true)
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
}

