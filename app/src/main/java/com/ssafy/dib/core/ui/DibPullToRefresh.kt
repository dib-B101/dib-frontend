package com.ssafy.dib.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 서버 데이터를 보여주는 화면에서 사용하는 공통 당겨서 새로고침 컨테이너.
 *
 * 각 화면이 같은 Material 제스처와 진행 표시를 쓰도록 한곳에 둔다. 안쪽 콘텐츠는
 * LazyColumn 같은 스크롤 컨테이너여야 빈 화면에서도 당김 동작을 받을 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DibPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        content = content
    )
}
