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
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
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

/** LIVE_AUCTION_CLOSED 한 건의 표시용 결과. winnerId 는 서버가 아직 안 주면 null 이고, 그때는 낙찰자 연출을 하지 않는다. */
data class LiveAuctionResult(val auctionId: String, val title: String, val finalPrice: Int?, val sold: Boolean, val winnerId: String?, val occurredAt: String?)

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

/**
 * 새로 받은 채팅을 목록에 합친다.
 *
 * 내가 보낸 댓글은 서버 수락 응답(CHAT_ACCEPTED)으로 먼저 들어오는데 거기엔 닉네임이 없다.
 * 그래서 처음엔 "나"로 보이다가 다시 들어와 이력을 받으면 닉네임으로 바뀌었다.
 * 내 댓글은 내 프로필 닉네임을 채워 넣고, 같은 댓글이 방송 이벤트로 한 번 더 오면 닉네임이 있는 쪽을 남긴다.
 */
internal fun mergeLiveChatMessage(
    messages: List<LiveChatMessage>,
    incoming: LiveChatMessage,
    currentMemberId: String?,
    currentNickname: String?
): List<LiveChatMessage> {
    val filled = if (incoming.nickname.isNullOrBlank() && currentMemberId != null && incoming.memberId == currentMemberId) {
        incoming.copy(nickname = currentNickname?.takeIf(String::isNotBlank))
    } else incoming
    val index = messages.indexOfFirst { it.liveChattingId == filled.liveChattingId }
    if (index < 0) return messages + filled
    if (!messages[index].nickname.isNullOrBlank() || filled.nickname.isNullOrBlank()) return messages
    return messages.toMutableList().also { it[index] = messages[index].copy(nickname = filled.nickname) }
}

/*
 * 판매자 공지 규약.
 *
 * 서버에는 공지 기능이 없어 예전엔 "판매자가 보낸 마지막 채팅"을 공지로 띄웠다. 그래서 판매자가 인사만 해도 공지가 바뀌었다.
 * 이제 판매자가 콘솔에서 댓글을 골라 "공지로 설정"하면 머리말을 붙인 채팅을 한 번 더 보내고,
 * 앱은 그 머리말이 붙은 판매자 채팅만 공지로 본다. 공지 내리기는 해제 머리말만 보낸다.
 * 두 메시지는 공지 줄에만 쓰고 채팅 목록에는 보이지 않는다. 채팅 이력에 남으므로 다시 들어와도 같은 공지가 보인다.
 */
internal const val LIVE_NOTICE_PREFIX = "[공지] "
internal const val LIVE_NOTICE_CLEAR = "[공지 해제]"
private const val LIVE_CHAT_MAX_LENGTH = 500

internal fun liveNoticeCommand(content: String): String =
    LIVE_NOTICE_PREFIX + content.trim().take(LIVE_CHAT_MAX_LENGTH - LIVE_NOTICE_PREFIX.length)

/** 공지·공지 해제 메시지인지. 채팅 목록에서 숨길 때 쓴다 (판매자가 보낸 것만 규약으로 인정한다) */
internal fun isLiveNoticeControl(message: LiveChatMessage, sellerMemberId: String?): Boolean =
    sellerMemberId != null && message.memberId == sellerMemberId &&
        (message.content.startsWith(LIVE_NOTICE_PREFIX) || message.content == LIVE_NOTICE_CLEAR)

/** 지금 걸려 있는 공지. 판매자의 마지막 공지·해제 메시지를 보고 정한다. 내용은 머리말을 뗀 채로 돌려준다 */
internal fun currentLiveNotice(messages: List<LiveChatMessage>, sellerMemberId: String?): LiveChatMessage? {
    val seller = sellerMemberId?.takeIf(String::isNotBlank) ?: return null
    val last = messages.filter { isLiveNoticeControl(it, seller) }.maxByOrNull(LiveChatMessage::time) ?: return null
    if (last.content == LIVE_NOTICE_CLEAR) return null
    return last.copy(content = last.content.removePrefix(LIVE_NOTICE_PREFIX)).takeIf { it.content.isNotBlank() }
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

/** 상품 카드의 경매 시간 표기. 분 단위로만 보이면 300초/60초처럼 1분 미만 상품이 "0분"으로 뭉개진다. */
internal fun liveDurationLabel(seconds: Long): String = when {
    seconds < 60 -> "${seconds}초"
    seconds % 60 == 0L -> "${seconds / 60}분"
    else -> "${seconds / 60}분 ${seconds % 60}초"
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
    lastResult: LiveAuctionResult? = null,
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
    // 차단 다이얼로그도 같은 절대 시각 기준 카운트다운을 써야 "약 2분"이 카드 타이머와 같이 움직인다
    val activeRemaining = rememberLiveCountdown(activeAuction?.auctionId, activeAuction?.remainingSeconds ?: 0, activeAuction?.endedAt, running = activeAuction != null)
    val priceRequired = actionErrorCode == "AUCTION_PRICE_REQUIRED"
    // 경매가 끝나 진행 중 상품이 사라지면 차단 사유도 사라진다
    LaunchedEffect(activeAuction?.auctionId) { if (activeAuction == null) showEndBlocked = false }

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
                    // 카메라가 꺼져 있거나 연결 전일 때 검은 화면 대신 보여줄 대표 이미지 — 진행 중 상품이 없으면 편성 목록의 첫 상품으로 대체
                    coverImageUrl = (activeAuction ?: auctions.firstOrNull())?.imageUrls?.firstOrNull(),
                    // 영상이 화면의 주인공이 되도록 16dp 패딩을 없애고 weight 로 세로 공간을 더 배분한다 (#24)
                    modifier = Modifier.fillMaxWidth().weight(1.15f)
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
                    lastResult = lastResult,
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

    if (showEndConfirm) DibDialog(
        onDismissRequest = { if (!actionLoading) showEndConfirm = false },
        title = "방송을 종료할까요?",
        text = { Text("종료하면 시청자의 채팅과 입찰이 모두 닫혀요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp) },
        confirmButton = { DibDialogConfirmButton("방송 종료", { showEndConfirm = false; onEndLive() }, enabled = !actionLoading, destructive = true) },
        dismissButton = { DibDialogDismissButton({ showEndConfirm = false }, label = "계속 방송", enabled = !actionLoading) }
    )

    // 진행 중인 경매를 두고 방송을 끊으면 입찰자는 낙찰되고도 화면이 사라진다. 막되, 왜 막혔는지는 알려준다
    if (showEndBlocked) DibDialog(
        onDismissRequest = { showEndBlocked = false },
        title = "아직 종료할 수 없어요",
        text = {
            Text(
                buildString {
                    append("‘")
                    append(activeAuction?.title.orEmpty().ifBlank { "진행 중인 상품" })
                    append("’ 경매가 진행 중이에요.")
                    activeRemaining.takeIf { it > 0 }?.let {
                        append(" 약 ")
                        append(remainingLabel(it))
                        append(" 남았어요.")
                    }
                    append("\n\n경매가 끝나면 방송을 종료할 수 있어요. 입찰자가 낙찰을 확인하기 전에 방송이 끊기지 않게 하려는 거예요.")
                },
                fontSize = 13.sp
            )
        },
        confirmButton = { DibDialogConfirmButton("알겠어요", { showEndBlocked = false }) }
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
    coverImageUrl: String?,
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
    // 실제 영상이 그려질 때만 대표 이미지를 감춘다 — 나머지 모든 안내 분기(에뮬레이터·권한없음·연결끊김·카메라꺼짐 등)에서는 대표 이미지를 계속 보여준다
    val showingLiveSurface = videoEnabled &&
        (session.state == LiveVideoState.Connected || session.state == LiveVideoState.Reconnecting) &&
        session.cameraEnabled && session.videoTrack != null
    val controlsVisible = !isEmulator && permissionGranted && !liveEnded && streamTokenProvider != null

    // 버튼을 영상 아래에 별도로 두면 영상 높이가 그만큼 줄어든다 — 버튼을 영상 위 오버레이로 옮기고 패널은 Box 하나로만 구성한다 (#24)
    Box(
        modifier.fillMaxSize().background(Color(0xFF17212D)),
        contentAlignment = Alignment.Center
    ) {
        if (!showingLiveSurface) LiveCoverBackdrop(coverImageUrl)
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
                    ) else LiveVideoNotice("카메라가 꺼져 있어요", "대표 이미지를 보여주고 있어요 · 아래 버튼으로 다시 켤 수 있어요")
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Colors.Mint, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    Text("영상을 연결하는 중이에요", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        // 하단 컨트롤 오버레이와 겹치지 않도록 room 라벨을 위쪽으로 옮긴다
        roomName?.takeIf(String::isNotBlank)?.let {
            Text(
                "room · " + it,
                Modifier.align(Alignment.TopStart).padding(8.dp),
                color = Colors.Mint,
                fontSize = 9.sp
            )
        }
        if (controlsVisible) Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiveVideoControlPill(
                label = if (session.cameraEnabled) "카메라 끄기" else "카메라 켜기",
                enabled = streaming,
                onClick = { session.updateCameraEnabled(!session.cameraEnabled) }
            )
            LiveVideoControlPill(
                label = if (session.microphoneEnabled) "음소거" else "음소거 해제",
                enabled = streaming,
                onClick = { session.updateMicrophoneEnabled(!session.microphoneEnabled) }
            )
            LiveVideoControlPill(
                label = if (session.usingFrontCamera) "후면 전환" else "전면 전환",
                enabled = streaming && session.cameraEnabled,
                onClick = session::switchCamera
            )
        }
    }
}

/** 영상 위 오버레이용 작은 알약 버튼. OutlinedButton 을 그대로 쓰면 영상을 가리는 불투명한 흰 배경이 생겨 반투명 검정 배경으로 직접 그린다. */
@Composable
private fun LiveVideoControlPill(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.graphicsLayer(alpha = if (enabled) 1f else .5f),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = .45f),
        contentColor = Color.White
    ) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 라이브 영상이 그려지지 않는 동안(대기·오류·카메라 꺼짐 등) 검은 화면 대신 상품 대표 이미지를 배경으로 보여준다. */
@Composable
private fun LiveCoverBackdrop(url: String?) {
    // 응답에 이미지가 없으면 문구만 남긴다
    if (url.isNullOrBlank()) return
    DibNetworkImage(url, null, Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)))
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
    lastResult: LiveAuctionResult?,
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
        // 다음 경매가 아직 안 켜진 동안, 방금 끝난 경매의 결과를 먼저 보여준다 — 자동 시작은 없다는 것도 여기서 알린다
        if (!hasActiveAuction && lastResult != null) item(key = "console-last-result:${lastResult.auctionId}:${lastResult.occurredAt}") {
            LiveConsoleResultCard(lastResult)
        }
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
private fun LiveConsoleResultCard(result: LiveAuctionResult) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("‘${result.title}’ 경매가 종료됐어요", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            buildString {
                append(if (result.sold) "낙찰 · 최종가 %,d원".format(result.finalPrice ?: 0) else "유찰 · 입찰이 없었어요")
                if (result.winnerId != null) append(" · 낙찰자에게 결과를 알렸어요")
            },
            color = Colors.Text,
            fontSize = 12.sp
        )
        Text(
            "다음 상품은 아래 카드의 ‘지금 시작’을 눌러야 시작돼요. 자동으로 시작되지 않아요.",
            color = Colors.Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LiveConsoleProductRow(
    auction: AuctionSummary,
    startEnabled: Boolean,
    onStartAuction: (String) -> Unit
) {
    val active = auction.status.equals("ACTIVE", ignoreCase = true)
    val remaining = rememberLiveCountdown(auction.auctionId, auction.remainingSeconds, auction.endedAt, active)
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
                        " · " + (auction.auctionTimeSeconds.takeIf { it > 0 }?.let { liveDurationLabel(it) } ?: "시간 미정"),
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

/** 서버 시각 문자열을 Instant 로 복구한다. core/time 의 포맷터와 같은 순서(Instant → OffsetDateTime → LocalDateTime)를 따른다. */
internal fun parseLiveInstant(value: String?): java.time.Instant? = value?.takeIf(String::isNotBlank)?.let { raw ->
    runCatching { java.time.Instant.parse(raw) }
        .recoverCatching { java.time.OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { java.time.LocalDateTime.parse(raw).atZone(java.time.ZoneId.systemDefault()).toInstant() }
        .getOrNull()
}

// 올림으로 센다 — 내림이면 서버가 15초로 되돌린 직후 14 로 보인다 (remainingWholeSeconds 참고)
internal fun remainingSecondsUntil(endedAt: String?, now: java.time.Instant = java.time.Instant.now()): Int? =
    parseLiveInstant(endedAt)?.let { com.ssafy.dib.core.time.remainingWholeSeconds(now, it) }

/**
 * 남은 시간은 종료 절대 시각(endedAt) 기준으로 매 틱 다시 계산한다.
 * 정수 초를 그대로 들고 있으면 채팅 탭으로 갔다가 돌아왔을 때 컴포지션이 그 정수부터 다시 세기 시작해
 * 카운트다운이 과거 값으로 되감긴다 — endedAt 이 있으면 그 문제 자체가 생기지 않는다.
 * endedAt 이 없거나 파싱에 실패하면 기존 정수 카운트다운으로 폴백한다.
 */
@Composable
internal fun rememberLiveCountdown(key: String?, remainingSeconds: Int, endedAt: String?, running: Boolean): Int {
    val endInstant = remember(endedAt) { parseLiveInstant(endedAt) }
    var remaining by remember(key) {
        mutableIntStateOf(endInstant?.let { remainingSecondsUntil(endedAt) } ?: remainingSeconds)
    }
    LaunchedEffect(key, remainingSeconds, endedAt, running) {
        // (재)컴포지션 시점에도 즉시 다시 계산해야 탭을 벗어났다 돌아온 순간의 값이 정확하다
        remaining = endInstant?.let { remainingSecondsUntil(endedAt) } ?: remainingSeconds
        if (!running) return@LaunchedEffect
        while (remaining > 0) {
            delay(1_000)
            remaining = endInstant?.let { remainingSecondsUntil(endedAt) } ?: (remaining - 1)
        }
    }
    return remaining
}

/**
 * 판매자가 남긴 마지막 채팅을 채팅창 위에 고정해 보여준다.
 * 일반 채팅은 계속 올라가 묻히므로 공지성 안내는 항상 보여야 한다.
 */
@Composable
private fun LiveSellerNoticeBar(notice: LiveChatMessage, onClear: (() -> Unit)?) {
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
            if (onClear != null) Text(
                "공지 내리기",
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClear).padding(horizontal = 6.dp, vertical = 2.dp),
                color = Colors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalLayoutApi::class)
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
    // 공지·해제 메시지는 공지 줄에만 쓰고 채팅 목록에서는 뺀다
    val feed = remember(chatMessages, bidNotices, sellerMemberId) {
        mergeLiveConsoleFeed(chatMessages.filterNot { isLiveNoticeControl(it, sellerMemberId) }, bidNotices)
    }
    val pinnedNotice = remember(chatMessages, sellerMemberId) { currentLiveNotice(chatMessages, sellerMemberId) }
    // 판매자가 누른 댓글. 공지로 올릴지 묻는다
    var noticeCandidate by remember { mutableStateOf<LiveChatMessage?>(null) }
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(feed.size) {
        if (feed.isNotEmpty()) listState.animateScrollToItem(feed.lastIndex)
    }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        // 키보드가 올라오면 목록 높이가 줄어 마지막 메시지가 가려진다. 열릴 때 맨 아래로 다시 붙인다
        if (imeVisible && feed.isNotEmpty()) listState.animateScrollToItem(feed.lastIndex)
    }
    Column(modifier.fillMaxSize()) {
        pinnedNotice?.let { notice ->
            LiveSellerNoticeBar(notice, onClear = if (inputEnabled) ({ onSendChat(LIVE_NOTICE_CLEAR) }) else null)
        }
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
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                // 댓글을 누르면 공지로 올릴 수 있다
                                .clickable(enabled = inputEnabled) { noticeCandidate = entry.message }
                                .then(
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
        // Scaffold 의 safeDrawingPadding 이 이미 IME 인셋을 포함하므로 여기서 다시 밀면 입력창이 키보드 위로 두 배 뜬다
        Row(
            Modifier.fillMaxWidth().background(Colors.Background).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(500) },
                modifier = Modifier.weight(1f),
                enabled = inputEnabled,
                placeholder = { Text(if (inputEnabled) "메시지를 입력하세요 · 댓글을 누르면 공지로 올려요" else "방송이 종료됐어요", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
    noticeCandidate?.let { candidate ->
        DibDialog(
            onDismissRequest = { noticeCandidate = null },
            title = "공지로 올릴까요?",
            text = {
                Text(
                    "“${candidate.content}”\n\n시청자 화면 위에 고정돼요. 다른 댓글을 올리면 공지가 바뀌어요.",
                    color = Colors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                DibDialogConfirmButton("공지로 설정", {
                    onSendChat(liveNoticeCommand(candidate.content))
                    noticeCandidate = null
                })
            },
            dismissButton = { DibDialogDismissButton({ noticeCandidate = null }) }
        )
    }
}
