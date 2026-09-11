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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class CategoryItem(val id: String, val icon: String, val name: String, val tint: Color, val surface: Color)

private val categories = listOf(
    CategoryItem("1", "▣", "디지털기기", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("2", "▤", "생활가전", Color(0xFFF47E58), Color(0xFFFAF0EB)),
    CategoryItem("3", "▰", "가구·인테리어", Colors.Navy, Color(0xFFF0F2FA)),
    CategoryItem("4", "◉", "스포츠·레저", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("5", "♧", "패션·잡화", Color(0xFFF47E58), Color(0xFFFAF0EB)),
    CategoryItem("6", "◕", "뷰티", Colors.Navy, Color(0xFFF0F2FA)),
    CategoryItem("7", "⌁", "취미·게임", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("8", "◌", "예술·창작", Color(0xFFF47E58), Color(0xFFFAF0EB))
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
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCategories = remoteCategories?.mapIndexed { index, category ->
        val style = categories[index % categories.size]
        CategoryItem(category.categoryId, style.icon, category.name, style.tint, style.surface)
    } ?: categories
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color.White,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedId != null) {
                    Text("←", Modifier.size(40.dp).clickable { selectedId = null; selectedName = null }.padding(top = 6.dp), fontSize = 24.sp)
                    Text(selectedName.orEmpty(), Modifier.weight(1f), color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                } else {
                    Image(painterResource(R.drawable.dib_primary_logo), contentDescription = "dib", modifier = Modifier.size(42.dp, 27.dp))
                    Spacer(Modifier.weight(1f))
                }
                Text("알림", Modifier.clickable(onClick = onNotificationsClick).padding(12.dp), color = Color(0xB813284B), fontSize = 12.sp)
            }
        },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(44.dp).background(Color(0xFFF2FCF9), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD3F2E9), RoundedCornerShape(12.dp)).clickable(onClick = onSearchClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = Color(0xFF70DAC0), fontSize = 22.sp)
                Text("상품을 검색해보세요", Modifier.padding(start = 10.dp), color = Colors.Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(selectedName ?: "카테고리", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (selectedId == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(visibleCategories) { category ->
                        Column(
                            Modifier.height(88.dp).clickable { selectedId = category.id; selectedName = category.name; onCategorySelected(category.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(Modifier.size(48.dp).background(category.surface, CircleShape), contentAlignment = Alignment.Center) {
                                Text(category.icon, color = category.tint, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(category.name, Modifier.padding(top = 8.dp), color = Colors.Navy, fontSize = 10.sp, maxLines = 1)
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
                Text("총 ${visibleAuctions.size}개의 경매", color = Colors.Muted, fontSize = 11.sp)
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
                            Modifier.clickable { onProductClick(auction.id) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.fillMaxWidth().height(132.dp))
                            Text(auction.name, color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${auction.priceLabel} · ${remainingTimeLabel(auction.remainingSeconds)} 남음", color = Colors.Navy, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
