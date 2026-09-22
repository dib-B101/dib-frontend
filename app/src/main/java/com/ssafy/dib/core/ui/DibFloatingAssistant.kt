package com.ssafy.dib.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlin.math.roundToInt

/** 띱이가 말풍선에 띄워 줄 알림 한 건. */
data class AssistantNotice(
    val id: String,
    val title: String,
    val body: String,
    val actionable: Boolean
)

/**
 * 화면 위에 떠 있는 띱이. 두 가지 일을 한다.
 *
 * 1. 내 방송이 켜져 있으면 그 사실을 계속 알리고, 누르면 방송 콘솔로 데려간다.
 *    (예전엔 로그인·앱 복귀 때마다 콘솔로 강제 이동시켰다. 방송하면서 다른 화면을 보는 건
 *     정상적인 사용인데 뒤로 가기가 소용없었다.)
 * 2. 알림이 오면 말풍선으로 띄우고, 누르면 해당 화면으로 보내 준다.
 *    스낵바로 띄우던 걸 여기로 옮겼다 — 스낵바는 몇 초 뒤 사라져서 놓치면 끝이었다.
 *
 * 화면마다 상단 바를 따로 만들고 있어서 고정 위치로 두면 검색·알림 버튼과 겹친다.
 * 그래서 눌러서 끌면 원하는 자리로 옮길 수 있게 했다. 위치는 화면 밖으로 못 나간다.
 */
@Composable
fun DibFloatingAssistant(
    live: Boolean,
    notice: AssistantNotice?,
    onOpenLive: () -> Unit,
    onOpenNotice: (AssistantNotice) -> Unit,
    onDismissNotice: (AssistantNotice) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!live && notice == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // TopEnd 를 기준점으로 삼아서 x 는 왼쪽(음수), y 는 아래(양수)로만 움직인다
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .statusBarsPadding()
            // 모든 헤더 오른쪽에 알림 버튼이 생겨서, 기본 자리를 헤더(56dp) 아래로 내린다. 끌어서 옮길 수 있다
            .padding(top = 64.dp, end = 12.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onSizeChanged {
                widthPx = it.width.toFloat()
                heightPx = it.height.toFloat()
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(visible = notice != null, enter = fadeIn(), exit = fadeOut()) {
            notice?.let {
                AssistantSpeechBubble(
                    notice = it,
                    onOpen = { onOpenNotice(it) },
                    onDismiss = { onDismissNotice(it) }
                )
            }
        }
        AssistantFace(
            live = live,
            hasNotice = notice != null,
            onTap = { if (notice != null) onOpenNotice(notice) else if (live) onOpenLive() },
            // 드래그는 얼굴을 잡고 한다. 예전엔 바깥 Column 에 드래그를 걸었는데,
            // 부모의 드래그 감지가 자식의 탭을 삼켜서 띱이를 눌러도 아무 반응이 없었다.
            // 같은 요소에 pointerInput 두 개를 나란히 붙이면 탭과 드래그가 같이 산다
            onDrag = { dx, dy ->
                // 화면 밖으로 끌어내면 다시 잡을 수 없다. 경계 안에 가둔다
                val minX = -(screenWidthPx - widthPx).coerceAtLeast(0f)
                val maxY = (screenHeightPx - heightPx).coerceAtLeast(0f)
                offsetX = (offsetX + dx).coerceIn(minX, 0f)
                offsetY = (offsetY + dy).coerceIn(0f, maxY)
            }
        )
    }
}

@Composable
private fun AssistantFace(
    live: Boolean,
    hasNotice: Boolean,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    // 깜빡이는 테두리가 있어야 "지금 진행 중"으로 읽힌다. 정지된 배지는 지나간 상태로 보인다
    val transition = rememberInfiniteTransition(label = "assistant")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "assistantPulse"
    )
    val ringColor = if (hasNotice) Colors.Mint else Colors.Live

    // 제스처를 얼굴 전체(배지 포함)에 건다. 예전엔 Image 에만 걸고 그 위에 LIVE 배지를 Surface 로 얹었는데,
    // Material3 Surface 는 onClick 이 없어도 뒤로 터치를 안 넘긴다. 배지가 얼굴 아래 절반을 덮고 있어서
    // 거기를 누르면 아무 반응이 없었다. 배지는 터치를 가로채지 않는 Box 로 바꾼다
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .size(52.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    ) {
        Image(
            painter = painterResource(R.drawable.dib_mascot_face),
            contentDescription = if (live) "방송 중. 눌러서 방송 콘솔로 이동" else "새 알림",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.5.dp, ringColor.copy(alpha = pulse), CircleShape)
        )
        if (live) Box(
            modifier = Modifier
                .padding(bottom = 1.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Colors.Live)
        ) {
            Text(
                "LIVE",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun AssistantSpeechBubble(
    notice: AssistantNotice,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Colors.Navy,
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 240.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    Modifier
                        .widthIn(max = 186.dp)
                        .pointerInput(notice.id) { detectTapGestures { onOpen() } }
                ) {
                    Text(
                        notice.title.ifBlank { "새 알림" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        // 제목이 핵심 메시지다. 한 줄로 자르면 "…라이브가 있…" 처럼 뜻이 잘린다
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (notice.body.isNotBlank()) Text(
                        notice.body,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .pointerInput(notice.id) { detectTapGestures { onDismiss() } },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 10.sp)
                }
            }
            Text(
                if (notice.actionable) "눌러서 보러 가기" else "눌러서 닫기",
                color = Colors.Mint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}
