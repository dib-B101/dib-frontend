package com.ssafy.dib.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors
import kotlinx.coroutines.launch

/** Figma wishlist: 28 dp visual control within a 44 dp touch target. */
@Composable
fun DibWishlistButton(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    productName: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // 상세 하단처럼 이미 카드 배경 위에 놓일 때는 흰 원 없이 하트만 크게 그린다 (테두리가 겹쳐 보이던 문제)
    plain: Boolean = false
) {
    val heartScale = remember { Animatable(1f) }
    val animationScope = rememberCoroutineScope()
    IconToggleButton(
        checked = selected,
        onCheckedChange = { next ->
            animationScope.launch {
                heartScale.snapTo(.72f)
                heartScale.animateTo(1.2f, spring(stiffness = Spring.StiffnessMedium))
                heartScale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
            }
            onSelectedChange(next)
        },
        enabled = enabled,
        modifier = modifier.size(44.dp)
    ) {
        if (plain) {
            Image(
                painterResource(if (selected) R.drawable.favorite_selected else R.drawable.favorite_outline),
                contentDescription = "$productName ${if (selected) "찜 해제" else "찜하기"}",
                modifier = Modifier.size(28.dp).graphicsLayer(scaleX = heartScale.value, scaleY = heartScale.value),
                colorFilter = ColorFilter.tint(if (selected) WireframeColors.Favorite else WireframeColors.Muted)
            )
            return@IconToggleButton
        }
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = WireframeColors.Background.copy(alpha = .94f),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painterResource(if (selected) R.drawable.favorite_selected else R.drawable.favorite_outline),
                    contentDescription = "$productName ${if (selected) "찜 해제" else "찜하기"}",
                    modifier = Modifier.size(16.dp).graphicsLayer(scaleX = heartScale.value, scaleY = heartScale.value),
                    colorFilter = ColorFilter.tint(if (selected) WireframeColors.Favorite else WireframeColors.Muted)
                )
            }
        }
    }
}
