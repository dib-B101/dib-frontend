package com.ssafy.dib.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** 헤더 알림 벨이 필요로 하는 값. AppNavHost 가 한 번 제공하고 모든 서브 헤더가 읽는다. */
data class DibNotificationBellState(val unreadCount: Int, val onClick: () -> Unit)

// 헤더마다 미읽음 개수·클릭 콜백을 파라미터로 뚫으면 화면 20여 개의 시그니처가 바뀐다. 알림 벨은 어느 화면에서나
// 같은 뜻이라 CompositionLocal 로 한 번만 제공한다. null 이면(프리뷰·라이브 전체화면) 벨을 그리지 않는다
val LocalDibNotificationBell = compositionLocalOf<DibNotificationBellState?> { null }

/** 헤더 오른쪽 알림 벨. 미읽음 개수는 타원이 아닌 원 배지로 보여준다. */
@Composable
fun DibNotificationBell(modifier: Modifier = Modifier, tint: Color = Colors.Text) {
    val bell = LocalDibNotificationBell.current ?: return
    Box(modifier.size(44.dp).clickable(onClick = bell.onClick), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.notification_vector),
            contentDescription = if (bell.unreadCount > 0) "새 알림 ${bell.unreadCount}개" else "알림",
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        if (bell.unreadCount > 0) {
            // 두 자리(99+)도 같은 크기의 원 안에 들어가게 글자 크기만 줄인다
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 2.dp).size(18.dp).background(Colors.Live, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (bell.unreadCount > 99) "99+" else bell.unreadCount.toString(),
                    color = Color.White,
                    fontSize = if (bell.unreadCount > 99) 7.sp else 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 뒤로가기 + 제목 + 알림 벨(+ 추가 액션)로 이뤄진 공통 서브 헤더.
 * 라이브 전체화면을 뺀 모든 하위 화면이 같은 헤더를 써서 어디서든 알림으로 갈 수 있다.
 */
@Composable
fun DibSubAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showNotifications: Boolean = true,
    @DrawableRes backIcon: Int = R.drawable.back,
    backDescription: String = "뒤로",
    titleContent: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier.background(Colors.Background)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Image(painterResource(backIcon), backDescription, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
            }
            if (titleContent != null) titleContent()
            else Text(title, Modifier.weight(1f), color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
            if (showNotifications) DibNotificationBell()
        }
        HorizontalDivider(color = Colors.Border)
    }
}
