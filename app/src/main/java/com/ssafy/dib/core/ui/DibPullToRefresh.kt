package com.ssafy.dib.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/**
 * 서버 데이터를 보여주는 화면에서 사용하는 공통 당겨서 새로고침 컨테이너.
 *
 * 각 화면이 같은 Material 제스처와 진행 표시를 쓰도록 한곳에 둔다. 안쪽 콘텐츠는
 * LazyColumn 같은 스크롤 컨테이너여야 빈 화면에서도 당김 동작을 받을 수 있다.
 *
 * 인디케이터만 내려오고 내용은 제자리에 있으면 당기는 느낌이 없어서, 당긴 거리만큼 화면 전체를 함께 끌어내린다.
 * 새로고침 중에는 임계 거리에 머물고 끝나면 제자리로 돌아간다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DibPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val threshold = PullToRefreshDefaults.PositionalThreshold
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = Colors.Navy
            )
        }
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = state.distanceFraction.coerceIn(0f, 1.5f) * threshold.toPx() }) {
            content()
        }
    }
}
