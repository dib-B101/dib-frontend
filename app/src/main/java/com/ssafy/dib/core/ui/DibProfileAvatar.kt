package com.ssafy.dib.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** 회원 프로필 사진. URL 이 없거나 못 받아오면 기본 사람 아이콘을 그린다. */
@Composable
fun DibProfileAvatar(imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Colors.Surface), contentAlignment = Alignment.Center) {
        if (!imageUrl.isNullOrBlank()) {
            DibNetworkImage(imageUrl, "프로필 사진", Modifier.fillMaxSize(), placeholderText = "")
        } else {
            Image(painterResource(R.drawable.seller), null, Modifier.size(size * 0.55f), colorFilter = ColorFilter.tint(Colors.Muted))
        }
    }
}
