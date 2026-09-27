package com.ssafy.dib.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun RecommendedAuctionsScreen(
    auctions: List<HomeAuction>?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit
) {
    var contentView by rememberSaveable { mutableStateOf(DibContentView.Grid) }
    Scaffold(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { DibSubAppBar("추천 경매", onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        DibPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("추천 경매 모아보기", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("추천 순서대로 둘러보세요", color = Colors.Muted, fontSize = 12.sp)
                        }
                        DibViewModeToggle(contentView, { contentView = it })
                    }
                }
                when {
                    isLoading && auctions == null -> item {
                        Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Colors.Navy)
                            Text("추천 경매를 불러오고 있어요", Modifier.padding(top = 14.dp), color = Colors.Muted, fontSize = 13.sp)
                        }
                    }
                    errorMessage != null && auctions == null -> item {
                        Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("추천 경매를 불러오지 못했어요", color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), modifier = Modifier.padding(top = 12.dp)) {
                                Text("다시 시도")
                            }
                        }
                    }
                    auctions.isNullOrEmpty() -> item {
                        Text("지금 추천할 경매가 없어요", Modifier.fillMaxWidth().padding(top = 80.dp), color = Colors.Muted, fontSize = 14.sp)
                    }
                    contentView == DibContentView.Grid -> {
                        val rows = auctions.chunked(2)
                        items(rows.size) { index ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rows[index].forEach { auction ->
                                    HomeAuctionCard(auction, favorite = false, onFavorite = null, onClick = { onProductClick(auction.id) }, modifier = Modifier.weight(1f))
                                }
                                if (rows[index].size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    else -> items(auctions.size) { index ->
                        HomeAuctionListCard(auctions[index], favorite = false, onFavorite = null, onClick = { onProductClick(auctions[index].id) })
                    }
                }
                if (!auctions.isNullOrEmpty()) item {
                    Button(onClick = onBrowseAll, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                        Text("전체 경매 둘러보기")
                    }
                }
            }
        }
    }
}
