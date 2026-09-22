package com.ssafy.dib.feature.live

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ssafy.dib.core.device.DeviceEnvironment
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.live.LiveChatMessage
import com.ssafy.dib.domain.live.LiveStreamSession
import com.ssafy.dib.feature.home.formatClock
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

/** 입찰 이벤트를 채팅 흐름에 시스템 줄로 섞어 보여주기 위한 표시용 모델. */
data class LiveBidNotice(
    val noticeId: String,
    val nickname: String?,
    val amount: Int,
    val time: String
)

internal sealed interface LiveConsoleFeedEntry {
    val entryKey: String
    val entryTime: String

    data class Chat(val message: LiveChatMessage) : LiveConsoleFeedEntry {
        override val entryKey: String get() = "chat:" + message.liveChattingId
        override val entryTime: String get() = message.time
    }

    data class Bid(val notice: LiveBidNotice) : LiveConsoleFeedEntry {
        override val entryKey: String get() = "bid:" + notice.noticeId
        override val entryTime: String get() = notice.time
    }
}

internal fun mergeLiveConsoleFeed(
    chatMessages: List<LiveChatMessage>,
    bidNotices: List<LiveBidNotice>
): List<LiveConsoleFeedEntry> = (
    chatMessages.map(LiveConsoleFeedEntry::Chat) + bidNotices.map(LiveConsoleFeedEntry::Bid)
    ).sortedBy(LiveConsoleFeedEntry::entryTime).distinctBy(LiveConsoleFeedEntry::entryKey)

/**
 * 채팅 작성자 표시명. 닉네임이 없을 때 회원 번호가 그대로 노출되면
 * 사람 이름이 갑자기 "1", "2" 처럼 보여 마스킹된 것으로 오해된다.
 */
internal fun liveChatSpeakerLabel(
    message: LiveChatMessage,
    currentMemberId: String?,
    fromSeller: Boolean
): String = message.nickname?.takeIf(String::isNotBlank)
    ?: when {
        currentMemberId?.takeIf(String::isNotBlank) == message.memberId -> "나"
        fromSeller -> "판매자"
        else -> "익명"
    }

internal fun liveBidNoticeLabel(notice: LiveBidNotice): String {
    val amount = "%,d원 입찰".format(notice.amount)
    return notice.nickname?.takeIf(String::isNotBlank)?.let { "${it}님이 " + amount } ?: amount
}

internal fun liveAuctionStatusLabel(status: String): String = when (status.uppercase()) {
    "SCHEDULED", "READY", "PENDING" -> "예정"
    "ACTIVE" -> "진행중"
    "ENDED", "CLOSED", "SOLD" -> "종료"
    "CANCELED", "CANCELLED" -> "취소"
    else -> status
}

internal fun liveConnectionLabel(state: RealtimeConnectionState?): String = when (state) {
    RealtimeConnectionState.Connecting -> "연결 중"
    RealtimeConnectionState.Connected -> "실시간 연결됨"
    RealtimeConnectionState.Reconnecting -> "재연결 중"
    RealtimeConnectionState.Disconnected -> "연결 끊김"
    null -> "실시간 대기"
}

/** 방송자가 진행 중인 Live 를 조작하는 콘솔. 영상 송출이 실패해도 채팅·경매 조작은 그대로 동작한다. */
@Composable
fun LiveBroadcastConsoleScreen(
    liveTitle: String,
    liveStatus: String,
    viewerCount: Int,
    roomName: String?,
    streamTokenProvider: (suspend () -> Result<LiveStreamSession>)?,
    auctions: List<AuctionSummary>,
    chatMessages: List<LiveChatMessage>,
    bidNotices: List<LiveBidNotice>,
    connectionState: RealtimeConnectionState?,
    isLoading: Boolean,
    errorMessage: String?,
    actionLoading: Boolean,
    actionError: String?,
    actionErrorCode: String?,
    actionMessage: String?,
    chatError: String?,
    currentMemberId: String?,
    liveEnded: Boolean,
    onRetry: () -> Unit,
    onStartAuction: (String) -> Unit,
    onEndLive: () -> Unit,
    onSendChat: (String) -> Boolean,
    onOpenItemPlan: () -> Unit,
    onDismissActionError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 시스템 뒤로가기(제스처·버튼)는 화면의 뒤로가기 버튼과 달리 onBack 을 타지 않고 백스택만 pop 한다.
    // 그러면 방송 편성 화면으로 떨어지고 "방송 켜져 있다" 안내도 안 뜬다. 같은 경로로 모아 준다
    BackHandler { onBack() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showEndConfirm by rememberSaveable { mutableStateOf(false) }
    // 종료가 막혔을 때 버튼을 흐리게만 두면 왜 안 되는지 알 방법이 없다 — 눌리게 두고 사유를 띄운다
    var showEndBlocked by rememberSaveable { mutableStateOf(false) }
    val activeAuction = auctions.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
    val priceRequired = actionErrorCode == "AUCTION_PRICE_REQUIRED"

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Surface
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Colors.Navy)
            }
            errorMessage != null -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
                TextButton(onClick = onBack) { Text("돌아가기") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                LiveConsoleHeader(
                    liveTitle = liveTitle,
                    liveStatus = liveStatus,
                    viewerCount = viewerCount,
                    connectionState = connectionState,
                    liveEnded = liveEnded,
                    endEnabled = !liveEnded && !actionLoading,
                    onBack = onBack,
                    onEndRequest = {
                        if (activeAuction != null) showEndBlocked = true else showEndConfirm = true
                    }
                )
                HorizontalDivider(color = Colors.Border)
                LiveBroadcastVideoPanel(
                    roomName = roomName,
                    liveEnded = liveEnded,
                    streamTokenProvider = streamTokenProvider,
                    modifier = Modifier.padding(16.dp)
                )
                if (liveEnded) LiveConsoleBanner("방송이 종료됐어요.", Colors.Urgent, Color(0xFFFFE9E9))
                actionMessage?.let { LiveConsoleBanner(it, Colors.MintInk, Color(0xFFDDF8F0)) }
                actionError?.let { message ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LiveConsoleBanner(message, Colors.Urgent, Color(0xFFFFE9E9), Modifier)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (priceRequired) TextButton(onClick = onOpenItemPlan) { Text("상품 편성으로 이동", fontSize = 12.sp) }
                            TextButton(onClick = onDismissActionError) { Text("닫기", fontSize = 12.sp) }
                        }
                    }
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = Colors.Background, contentColor = Colors.Navy) {
                    Tab(selectedTab == 0, { selectedTab = 0 }) {
                        Text("상품 ${auctions.size}", Modifier.padding(vertical = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Tab(selectedTab == 1, { selectedTab = 1 }) {
                        Text("채팅", Modifier.padding(vertical = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (selectedTab == 0) LiveConsoleProductTab(
                    auctions = auctions,
                    hasActiveAuction = activeAuction != null,
                    actionLoading = actionLoading,
                    liveEnded = liveEnded,
                    onStartAuction = onStartAuction,
                    onOpenItemPlan = onOpenItemPlan,
                    modifier = Modifier.weight(1f)
                ) else LiveChatPanel(
                    chatMessages = chatMessages,
                    bidNotices = bidNotices,
                    currentMemberId = currentMemberId,
                    sellerMemberId = currentMemberId,
                    chatError = chatError,
                    inputEnabled = !liveEnded,
                    onSendChat = onSendChat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showEndConfirm) AlertDialog(
        onDismissRequest = { if (!actionLoading) showEndConfirm = false },
        title = { Text("방송을 종료할까요?") },
        text = { Text("종료하면 시청자의 채팅과 입찰이 모두 닫혀요.", fontSize = 13.sp) },
        confirmButton = {
            TextButton(
                onClick = { showEndConfirm = false; onEndLive() },
                enabled = !actionLoading
            ) { Text("방송 종료", color = Colors.Urgent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton({ showEndConfirm = false }, enabled = !actionLoading) { Text("계속 방송") } }
    )

    // 진행 중인 경매를 두고 방송을 끊으면 입찰자는 낙찰되고도 화면이 사라진다. 막되, 왜 막혔는지는 알려준다
    if (showEndBlocked) AlertDialog(
        onDismissRequest = { showEndBlocked = false },
        title = { Text("아직 종료할 수 없어요") },
        text = {
            Text(
                buildString {
                    append("‘")
                    append(activeAuction?.title.orEmpty().ifBlank { "진행 중인 상품" })
                    append("’ 경매가 진행 중이에요.")
                    activeAuction?.remainingSeconds?.takeIf { it > 0 }?.let {
                        append(" 약 ")
                        append(remainingLabel(it))
                        append(" 남았어요.")
                    }
                    append("\n\n경매가 끝나면 방송을 종료할 수 있어요. 입찰자가 낙찰을 확인하기 전에 방송이 끊기지 않게 하려는 거예요.")
                },
                fontSize = 13.sp
            )
        },
        confirmButton = { TextButton({ showEndBlocked = false }) { Text("알겠어요") } }
    )
}

private fun remainingLabel(seconds: Int): String =
    if (seconds >= 60) "${seconds / 60}분 ${seconds % 60}초" else "${seconds}초"

@Composable
private fun LiveConsoleHeader(
    liveTitle: String,
    liveStatus: String,
    viewerCount: Int,
    connectionState: RealtimeConnectionState?,
    liveEnded: Boolean,
    endEnabled: Boolean,
    onBack: () -> Unit,
    onEndRequest: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "consoleLivePulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = .4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "consoleLiveDot"
    )
    Column(Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text))
            }
            Column(Modifier.weight(1f)) {
                Text(liveTitle.ifBlank { "Live 방송" }, color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(liveConnectionLabel(connectionState), color = Colors.Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onEndRequest, enabled = endEnabled) {
                Text("방송 종료", color = if (endEnabled) Colors.Urgent else Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.padding(start = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = if (liveEnded) Colors.Muted else Colors.Live, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (!liveEnded) Box(Modifier.size(6.dp).graphicsLayer(alpha = dotAlpha).background(Color.White, CircleShape))
                    Text(if (liveEnded) "종료" else "LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(painterResource(R.drawable.visibility), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                Text("%,d명 시청 중".format(viewerCount.coerceAtLeast(0)), color = Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (liveStatus.isNotBlank() && liveStatus != "LIVE") Text(liveStatus, color = Colors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LiveBroadcastVideoPanel(
    roomName: String?,
    liveEnded: Boolean,
    streamTokenProvider: (suspend () -> Result<LiveStreamSession>)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEmulator = remember { DeviceEnvironment.isEmulator }
    var permissionGranted by remember { mutableStateOf(LiveMediaPermissions.granted(context)) }
    var permissionBlocked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = LiveMediaPermissions.REQUIRED.all { result[it] == true }
        permissionGranted = allGranted
        // 거절 후 rationale 도 못 띄우는 상태면 설정에서 직접 켜야 하는 영구 거절이다
        val activity = context.findActivity()
        permissionBlocked = !allGranted && activity != null && LiveMediaPermissions.REQUIRED.none {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    // 설정 화면에서 권한을 켜고 돌아온 경우를 잡아준다
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = LiveMediaPermissions.granted(context)
                permissionGranted = granted
                if (granted) permissionBlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val videoEnabled = streamTokenProvider != null && !isEmulator && permissionGranted && !liveEnded
    val session = rememberLiveVideoSession(LiveVideoRole.PUBLISHER, videoEnabled, streamTokenProvider)
    val streaming = session.state == LiveVideoState.Connected || session.state == LiveVideoState.Reconnecting

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF17212D)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isEmulator -> LiveVideoNotice(
                    "카메라 송출은 실기기에서 테스트해주세요",
                    "에뮬레이터에서는 송출을 시도하지 않아요. 채팅과 경매 진행은 그대로 동작해요."
                )
                streamTokenProvider == null -> LiveVideoNotice(
                    "영상 송출을 사용할 수 없어요",
                    "지금은 채팅과 경매 진행만 동작해요"
                )
                liveEnded -> LiveVideoNotice("방송이 종료됐어요", "영상 송출이 중지됐어요")
                !permissionGranted -> LiveCameraPermissionNotice(
                    blocked = permissionBlocked,
                    onRequest = { permissionLauncher.launch(LiveMediaPermissions.REQUIRED) },
                    onOpenSettings = { openAppSettings(context) }
                )
                else -> when (val state = session.state) {
                    is LiveVideoState.Failed -> LiveVideoNotice(
                        "영상을 연결하지 못했어요",
                        state.message,
                        action = "다시 시도" to session::retry
                    )
                    LiveVideoState.Disconnected -> LiveVideoNotice(
                        "영상 연결이 끊겼어요",
                        "채팅과 경매 진행은 그대로 동작해요",
                        action = "다시 연결" to session::retry
                    )
                    LiveVideoState.Connected, LiveVideoState.Reconnecting ->
                        if (session.cameraEnabled && session.videoTrack != null) LiveVideoSurface(
                            room = session.room,
                            videoTrack = session.videoTrack,
                            mirror = session.usingFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        ) else LiveVideoNotice("카메라가 꺼져 있어요", "아래 버튼으로 다시 켤 수 있어요")
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Colors.Mint, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                        Text("영상을 연결하는 중이에요", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            roomName?.takeIf(String::isNotBlank)?.let {
                Text(
                    "room · " + it,
                    Modifier.align(Alignment.BottomStart).padding(8.dp),
                    color = Colors.Mint,
                    fontSize = 9.sp
                )
            }
        }
        if (!isEmulator && permissionGranted && !liveEnded && streamTokenProvider != null) Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { session.updateCameraEnabled(!session.cameraEnabled) },
                modifier = Modifier.weight(1f),
                enabled = streaming
            ) { Text(if (session.cameraEnabled) "카메라 끄기" else "카메라 켜기", fontSize = 12.sp) }
            OutlinedButton(
                onClick = { session.updateMicrophoneEnabled(!session.microphoneEnabled) },
                modifier = Modifier.weight(1f),
                enabled = streaming
            ) { Text(if (session.microphoneEnabled) "음소거" else "음소거 해제", fontSize = 12.sp) }
            OutlinedButton(
                onClick = session::switchCamera,
                modifier = Modifier.weight(1f),
                enabled = streaming && session.cameraEnabled
            ) { Text(if (session.usingFrontCamera) "후면 전환" else "전면 전환", fontSize = 12.sp) }
        }
    }
}

/** 영상 영역 안에서만 쓰는 어두운 배경용 안내. */
@Composable
internal fun LiveVideoNotice(
    title: String,
    description: String?,
    action: Pair<String, () -> Unit>? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Image(painterResource(R.drawable.live_video), null, Modifier.size(30.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = .8f)))
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        description?.takeIf(String::isNotBlank)?.let {
            Text(it, color = Color.White.copy(alpha = .66f), fontSize = 10.sp)
        }
        action?.let { (label, onClick) ->
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Colors.Mint)
            ) { Text(label, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun LiveCameraPermissionNotice(
    blocked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    LiveVideoNotice(
        title = "카메라 권한이 필요해요",
        description = if (blocked) {
            "권한을 계속 거절해서 설정에서 직접 켜야 해요. 채팅과 경매 진행은 그대로 동작해요."
        } else {
            "카메라와 마이크를 허용하면 영상이 송출돼요"
        },
        action = if (blocked) "설정 열기" to onOpenSettings else "허용하기" to onRequest
    )
}

@Composable
private fun LiveConsoleBanner(
    message: String,
    textColor: Color,
    background: Color,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
) {
    Text(
        message,
        modifier.fillMaxWidth().background(background, RoundedCornerShape(10.dp)).padding(12.dp),
        color = textColor,
        fontSize = 12.sp
    )
}

@Composable
private fun LiveConsoleProductTab(
    auctions: List<AuctionSummary>,
    hasActiveAuction: Boolean,
    actionLoading: Boolean,
    liveEnded: Boolean,
    onStartAuction: (String) -> Unit,
    onOpenItemPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (auctions.isEmpty()) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("편성된 상품이 없어요", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("상품 편성에서 판매할 상품을 먼저 넣어주세요", Modifier.padding(top = 6.dp), color = Colors.Muted, fontSize = 12.sp)
            OutlinedButton(onClick = onOpenItemPlan, modifier = Modifier.padding(top = 12.dp)) { Text("상품 편성으로 이동") }
        }
        return
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(auctions, key = AuctionSummary::auctionId) { auction ->
            LiveConsoleProductRow(
                auction = auction,
                startEnabled = !liveEnded && !actionLoading && !hasActiveAuction &&
                    !auction.status.equals("ACTIVE", ignoreCase = true) &&
                    liveAuctionStatusLabel(auction.status) == "예정",
                onStartAuction = onStartAuction
            )
        }
    }
}

@Composable
private fun LiveConsoleProductRow(
    auction: AuctionSummary,
    startEnabled: Boolean,
    onStartAuction: (String) -> Unit
) {
    val active = auction.status.equals("ACTIVE", ignoreCase = true)
    val remaining = rememberLiveCountdown(auction.auctionId, auction.remainingSeconds, active)
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, if (active) Colors.Live else Colors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(auction.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "시작가 " + (auction.startPriceOrNull?.let { "%,d원".format(it) } ?: "가격 미정") +
                        " · " + (auction.auctionTimeSeconds.takeIf { it > 0 }?.let { "${it / 60}분" } ?: "시간 미정"),
                    color = Colors.Muted,
                    fontSize = 11.sp
                )
            }
            Surface(color = if (active) Colors.Live else Colors.NavySoft, shape = RoundedCornerShape(9.dp)) {
                Text(
                    liveAuctionStatusLabel(auction.status),
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (active) Color.White else Colors.Navy,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (active) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("현재가 %,d원".format(auction.currentPrice), color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("입찰 ${auction.bidCount}회", color = Colors.Muted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(painterResource(R.drawable.timer_outline), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Colors.Live))
                Text(formatClock(remaining), color = Colors.Live, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (startEnabled) Button(
            onClick = { onStartAuction(auction.auctionId) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
        ) { Text("지금 시작", fontWeight = FontWeight.Bold) }
    }
}

/** 남은 시간은 서버 값으로 맞추고, 진행 중일 때만 1초마다 줄인다. */
@Composable
internal fun rememberLiveCountdown(key: String?, remainingSeconds: Int, running: Boolean): Int {
    var remaining by remember(key) { mutableIntStateOf(remainingSeconds) }
    LaunchedEffect(key, remainingSeconds, running) {
        remaining = remainingSeconds
        if (!running) return@LaunchedEffect
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }
    return remaining
}

/**
 * 판매자가 남긴 마지막 채팅을 채팅창 위에 고정해 보여준다.
 * 일반 채팅은 계속 올라가 묻히므로 공지성 안내는 항상 보여야 한다.
 */
@Composable
private fun LiveSellerNoticeBar(notice: LiveChatMessage) {
    var expanded by rememberSaveable(notice.liveChattingId) { mutableStateOf(false) }
    var overflowing by remember(notice.liveChattingId) { mutableStateOf(false) }
    val expandable = overflowing || expanded
    Column(
        Modifier.fillMaxWidth()
            .background(Colors.NavySoft)
            .then(if (expandable) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "판매자 공지",
                Modifier.background(Colors.Navy, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                notice.nickname?.takeIf(String::isNotBlank).orEmpty(),
                Modifier.weight(1f),
                color = Colors.Navy,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (expandable) Image(
                painterResource(R.drawable.chevron_right),
                if (expanded) "접기" else "펼치기",
                Modifier.size(16.dp).graphicsLayer(rotationZ = if (expanded) 270f else 90f),
                colorFilter = ColorFilter.tint(Colors.Navy)
            )
        }
        Text(
            notice.content,
            Modifier.fillMaxWidth().padding(top = 4.dp)
                .then(if (expanded) Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()) else Modifier),
            color = Colors.Text,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) overflowing = layout.hasVisualOverflow }
        )
    }
    HorizontalDivider(color = Colors.Border)
}

@Composable
internal fun LiveChatPanel(
    chatMessages: List<LiveChatMessage>,
    bidNotices: List<LiveBidNotice>,
    currentMemberId: String?,
    sellerMemberId: String?,
    chatError: String?,
    inputEnabled: Boolean,
    onSendChat: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val feed = remember(chatMessages, bidNotices) { mergeLiveConsoleFeed(chatMessages, bidNotices) }
    val pinnedNotice = remember(chatMessages, sellerMemberId) {
        sellerMemberId?.takeIf(String::isNotBlank)?.let { seller ->
            chatMessages.lastOrNull { it.memberId == seller && it.content.isNotBlank() }
        }
    }
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(feed.size) {
        if (feed.isNotEmpty()) listState.animateScrollToItem(feed.lastIndex)
    }
    Column(modifier.fillMaxSize()) {
        pinnedNotice?.let { LiveSellerNoticeBar(it) }
        if (feed.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("아직 채팅이 없어요", color = Colors.Muted, fontSize = 12.sp)
        } else LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(feed, key = LiveConsoleFeedEntry::entryKey) { entry ->
                when (entry) {
                    is LiveConsoleFeedEntry.Bid -> Text(
                        liveBidNoticeLabel(entry.notice),
                        Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(9.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Colors.MintInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    is LiveConsoleFeedEntry.Chat -> {
                        val fromSeller = sellerMemberId?.takeIf(String::isNotBlank) == entry.message.memberId
                        Column(
                            Modifier.fillMaxWidth().then(
                                if (fromSeller) Modifier
                                    .background(Colors.NavySoft, RoundedCornerShape(9.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                else Modifier
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (fromSeller) Text(
                                    "판매자",
                                    Modifier.background(Colors.Navy, RoundedCornerShape(5.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    liveChatSpeakerLabel(entry.message, currentMemberId, fromSeller),
                                    color = if (fromSeller) Colors.Navy else Colors.Muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                entry.message.content,
                                color = Colors.Text,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = if (fromSeller) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        chatError?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Colors.Urgent, fontSize = 11.sp) }
        HorizontalDivider(color = Colors.Border)
        Row(
            Modifier.fillMaxWidth().background(Colors.Background).imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(500) },
                modifier = Modifier.weight(1f),
                enabled = inputEnabled,
                placeholder = { Text(if (inputEnabled) "메시지를 입력하세요" else "방송이 종료됐어요", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            IconButton(
                onClick = { if (onSendChat(draft.trim())) draft = "" },
                enabled = inputEnabled && draft.isNotBlank()
            ) {
                Image(
                    painterResource(R.drawable.send),
                    "보내기",
                    Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (inputEnabled && draft.isNotBlank()) Colors.Navy else Colors.Muted)
                )
            }
        }
    }
}
