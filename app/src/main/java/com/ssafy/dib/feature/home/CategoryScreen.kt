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
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private data class CategoryItem(val icon: String, val name: String, val tint: Color, val surface: Color)

private val categories = listOf(
    CategoryItem("▣", "디지털기기", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("▤", "생활가전", Color(0xFFF47E58), Color(0xFFFAF0EB)),
    CategoryItem("▰", "가구·인테리어", Colors.Navy, Color(0xFFF0F2FA)),
    CategoryItem("◉", "스포츠·레저", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("♧", "패션·잡화", Color(0xFFF47E58), Color(0xFFFAF0EB)),
    CategoryItem("◕", "뷰티", Colors.Navy, Color(0xFFF0F2FA)),
    CategoryItem("⌁", "취미·게임", Color(0xFF45CDAA), Color(0xFFE8F7F2)),
    CategoryItem("◌", "예술·창작", Color(0xFFF47E58), Color(0xFFFAF0EB))
)

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color.White,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    Text("←", Modifier.size(40.dp).clickable { selected = null }.padding(top = 6.dp), fontSize = 24.sp)
                    Text(selected.orEmpty(), Modifier.weight(1f), color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            Text(selected ?: "카테고리", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (selected == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(categories) { category ->
                        Column(
                            Modifier.height(88.dp).clickable { selected = category.name },
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
                Text("총 6개의 경매", color = Colors.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) { index ->
                        Column(
                            Modifier.clickable { onProductClick(if (index == 0) "camera" else "category-$index") },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(132.dp).background(Color(0xFFECEEF1), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("상품 이미지", color = Colors.Muted, fontSize = 10.sp) }
                            Text(if (index == 0) "빈티지 필름 카메라" else "${selected} 추천 상품 ${index + 1}", color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${35_000 + index * 5_000}원 · ${12 + index}분 남음", color = Colors.Navy, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
