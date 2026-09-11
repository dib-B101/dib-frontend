package com.ssafy.dib.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors

/** Figma wishlist: 28 dp visual control within a 44 dp touch target. */
@Composable
fun DibWishlistButton(selected: Boolean, onSelectedChange: (Boolean) -> Unit, productName: String, modifier: Modifier = Modifier) {
    IconToggleButton(
        checked = selected,
        onCheckedChange = onSelectedChange,
        modifier = modifier.size(44.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = WireframeColors.Background
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painterResource(if (selected) R.drawable.favorite_selected else R.drawable.favorite_outline),
                    contentDescription = "$productName ${if (selected) "찜 해제" else "찜하기"}",
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(if (selected) WireframeColors.Urgent else WireframeColors.Muted)
                )
            }
        }
    }
}
