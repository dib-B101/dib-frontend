package com.ssafy.dib.feature.auction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.feature.home.ProductPhoto
import com.ssafy.dib.feature.home.allHomeAuctions

/** Figma 01_Wireframe / 03A_Image_Viewer. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProductImageViewerScreen(
    productId: String,
    initialPage: Int,
    imageUrls: List<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val product = allHomeAuctions.firstOrNull { it.id == productId } ?: allHomeAuctions.first()
    val pageCount = imageUrls.size.takeIf { it > 0 } ?: 5
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, pageCount - 1), pageCount = { pageCount })
    val background = Color(0xFF1A1A1A)

    Column(
        modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Image(
                    painterResource(R.drawable.close),
                    "이미지 뷰어 닫기",
                    Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${pagerState.currentPage + 1} / $pageCount", color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.width(16.dp))
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            ZoomableProductImage(product.photo, imageUrls.getOrNull(it), it)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pageCount) { index ->
                Box(
                    Modifier.size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                        .background(if (index == pagerState.currentPage) Color.White else Color(0xFF777777), CircleShape)
                )
            }
        }
        Text(
            "좌우로 사진 이동 · 두 손가락으로 확대",
            color = Color(0xFFB7B7B7),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 28.dp)
        )
    }
}

@Composable
private fun ZoomableProductImage(photo: ProductPhoto, imageUrl: String?, page: Int) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        if (nextScale == 1f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX += panChange.x
            offsetY += panChange.y
        }
    }

    Box(
        Modifier.fillMaxSize()
            .clipToBounds()
            .pointerInput(scale) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2f
                            offsetX = size.width / 2f - position.x
                            offsetY = size.height / 2f - position.y
                        }
                    }
                )
            }
            .transformable(transformState),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null || page == 0) {
            ProductPhoto(
                photo,
                imageUrl,
                Modifier.fillMaxWidth().height(560.dp).graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(560.dp).background(Color(0xFF333333)).graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("확대 이미지 ${page + 1}", color = Color(0xFFB7B7B7), fontSize = 13.sp)
            }
        }
    }
}
