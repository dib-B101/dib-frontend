package com.ssafy.dib.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.domain.auction.RecommendedLive
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveListScreen(
    lives: List<RecommendedLive>?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onLiveClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by rememberSaveable { mutableStateOf(DibContentView.Grid) }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { DibSubAppBar("LIVE", onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        DibPullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("라이브 ${lives?.size ?: 0}개", Modifier.weight(1f), color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        DibViewModeToggle(viewMode, { viewMode = it })
                    }
                }
                if (error != null) item {
                    Text(error, Modifier.fillMaxWidth().clickable(onClick = onRefresh).padding(20.dp), color = Colors.Urgent)
                }
                if (lives.isNullOrEmpty() && !loading && error == null) item {
                    Text("현재 볼 수 있는 라이브가 없어요. 아래로 당겨 새로고침해 주세요.", Modifier.fillMaxWidth().padding(vertical = 48.dp), color = Colors.Muted)
                }
                val rows = if (viewMode == DibContentView.Grid) lives.orEmpty().chunked(2) else lives.orEmpty().map { listOf(it) }
                items(rows.size) { index ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows[index].forEach { live ->
                            LiveListCard(live, Modifier.weight(1f), onLiveClick)
                        }
                        if (viewMode == DibContentView.Grid && rows[index].size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveListCard(live: RecommendedLive, modifier: Modifier, onLiveClick: (String) -> Unit) {
    val isLive = live.status.equals("LIVE", ignoreCase = true)
    Column(
        modifier.background(Colors.Background, RoundedCornerShape(14.dp))
            .clickable(enabled = isLive) { onLiveClick(live.liveBroadcastId) }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // 홈 LIVE 카드와 같이 편성 상품의 대표 사진을 썸네일로 쓴다. 편성 상품이 없을 때만 기본 아이콘을 둔다
        val thumbnailUrl = live.firstItemThumbnailUrl?.takeIf(String::isNotBlank)
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(10.dp))
                .background(if (thumbnailUrl != null) Colors.Image else Colors.NavySoft),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailUrl != null) {
                DibNetworkImage(thumbnailUrl, live.firstItemTitle ?: live.title, Modifier.matchParentSize())
            } else {
                Image(painterResource(R.drawable.live_video), null, Modifier.size(36.dp), colorFilter = ColorFilter.tint(Colors.Navy))
            }
        }
        Text(if (isLive) "● LIVE" else homeLiveScheduleLabel(live.scheduledAt), color = if (isLive) Colors.Live else Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(live.title, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        live.description?.takeIf(String::isNotBlank)?.let {
            Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Colors.Muted, fontSize = 12.sp)
        }
    }
}
