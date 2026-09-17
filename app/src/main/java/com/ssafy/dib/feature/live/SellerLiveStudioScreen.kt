package com.ssafy.dib.feature.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ssafy.dib.R
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import io.livekit.android.LiveKit
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** LiveKit 접속 자격 정보. API 계약 계층과 독립적으로 두어 호스트 화면을 재사용한다. */
data class SellerLiveSession(
    val serverUrl: String,
    val token: String,
    val roomName: String,
    val participantName: String
)

private enum class StudioConnectionState { PermissionRequired, WaitingForSession, Connecting, Live, Failed }

@Composable
fun SellerLiveStudioScreen(
    title: String,
    session: SellerLiveSession?,
    auctions: List<AuctionSummary>,
    viewerCount: Int,
    sessionLoading: Boolean,
    sessionError: String?,
    onRequestSession: () -> Unit,
    onPublisherConnected: () -> Unit,
    onStartAuction: (String) -> Unit,
    onEndLive: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val room = remember { LiveKit.create(context.applicationContext) }
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var localVideoTrack by remember { mutableStateOf<LocalVideoTrack?>(null) }
    var cameraEnabled by remember { mutableStateOf(true) }
    var microphoneEnabled by remember { mutableStateOf(true) }
    var connectionState by remember { mutableStateOf(StudioConnectionState.PermissionRequired) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var publishedSessionToken by remember { mutableStateOf<String?>(null) }
    var hasMediaPermissions by remember { mutableStateOf(listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true) {
            hasMediaPermissions = true
            connectionState = StudioConnectionState.WaitingForSession
            onRequestSession()
        } else {
            hasMediaPermissions = false
            connectionState = StudioConnectionState.PermissionRequired
            connectionError = "방송하려면 카메라와 마이크 권한이 필요해요."
        }
    }

    LaunchedEffect(Unit) {
        if (hasMediaPermissions) {
            connectionState = StudioConnectionState.WaitingForSession
            onRequestSession()
        }
    }

    LaunchedEffect(sessionError) {
        if (session == null && sessionError != null) connectionState = StudioConnectionState.Failed
    }

    LaunchedEffect(session?.token, hasMediaPermissions) {
        val activeSession = session ?: return@LaunchedEffect
        if (!hasMediaPermissions || publishedSessionToken == activeSession.token) return@LaunchedEffect
        connectionState = StudioConnectionState.Connecting
        connectionError = null
        runCatching {
            room.connect(activeSession.serverUrl, activeSession.token)
            room.localParticipant.setMicrophoneEnabled(true)
            room.localParticipant.setCameraEnabled(true)
            val track = room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
                ?: error("카메라 영상을 준비하지 못했어요.")
            renderer?.let(track::addRenderer)
            localVideoTrack = track
        }.onSuccess {
            publishedSessionToken = activeSession.token
            connectionState = StudioConnectionState.Live
            onPublisherConnected()
        }.onFailure { error ->
            connectionState = StudioConnectionState.Failed
            connectionError = error.message ?: "방송 서버에 연결하지 못했어요."
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState != StudioConnectionState.Live) return@LaunchedEffect
        while (true) {
            delay(1_000)
            elapsedSeconds++
        }
    }

    DisposableEffect(room) {
        onDispose {
            renderer?.let { localVideoTrack?.removeRenderer(it) }
            room.disconnect()
            room.release()
        }
    }

    Box(modifier.fillMaxSize().background(Color(0xFF101923))) {
        AndroidView(
            factory = { viewContext ->
                SurfaceViewRenderer(viewContext).also { view ->
                    room.initVideoRenderer(view)
                    view.setMirror(true)
                    renderer = view
                    localVideoTrack?.addRenderer(view)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                localVideoTrack?.removeRenderer(view)
                view.release()
                if (renderer === view) renderer = null
            }
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (connectionState == StudioConnectionState.Live && cameraEnabled) .08f else .5f)))

        StudioHeader(
            title = title,
            live = connectionState == StudioConnectionState.Live,
            elapsedSeconds = elapsedSeconds,
            viewerCount = viewerCount,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (connectionState != StudioConnectionState.Live) {
            StudioConnectionCard(
                state = connectionState,
                loading = sessionLoading,
                message = sessionError ?: connectionError,
                onAction = {
                    if (!hasMediaPermissions) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    } else {
                        connectionState = StudioConnectionState.WaitingForSession
                        onRequestSession()
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (connectionState == StudioConnectionState.Live) {
                StudioAuctionRail(auctions, onStartAuction)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioControl(
                    icon = if (microphoneEnabled) R.drawable.mic_outline else R.drawable.mic_off_outline,
                    label = if (microphoneEnabled) "마이크" else "음소거",
                    selected = microphoneEnabled,
                    enabled = connectionState == StudioConnectionState.Live
                ) {
                    val next = !microphoneEnabled
                    scope.launch {
                        runCatching { room.localParticipant.setMicrophoneEnabled(next) }
                            .onSuccess { microphoneEnabled = next }
                    }
                }
                StudioControl(
                    icon = if (cameraEnabled) R.drawable.camera_outline else R.drawable.camera_off_outline,
                    label = if (cameraEnabled) "카메라" else "카메라 꺼짐",
                    selected = cameraEnabled,
                    enabled = connectionState == StudioConnectionState.Live
                ) {
                    val next = !cameraEnabled
                    scope.launch {
                        runCatching {
                            room.localParticipant.setCameraEnabled(next)
                            if (next) {
                                val nextTrack = room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
                                    ?: error("카메라를 다시 켜지 못했어요.")
                                if (nextTrack !== localVideoTrack) {
                                    renderer?.let { view ->
                                        localVideoTrack?.removeRenderer(view)
                                        nextTrack.addRenderer(view)
                                    }
                                    localVideoTrack = nextTrack
                                }
                            }
                        }.onSuccess { cameraEnabled = next }
                    }
                }
                StudioControl(
                    icon = R.drawable.camera_flip_outline,
                    label = "전환",
                    selected = true,
                    enabled = connectionState == StudioConnectionState.Live && cameraEnabled
                ) { runCatching { localVideoTrack?.switchCamera() } }
                StudioControl(
                    icon = R.drawable.close,
                    label = "종료",
                    selected = false,
                    destructive = true,
                    enabled = connectionState == StudioConnectionState.Live
                ) {
                    room.disconnect()
                    onEndLive()
                }
            }
        }
    }
}

@Composable
private fun StudioHeader(
    title: String,
    live: Boolean,
    elapsedSeconds: Int,
    viewerCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.back),
            "방송 관리로 돌아가기",
            Modifier.size(42.dp).clickable(onClick = onBack).padding(9.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (live) "방송 중 · ${formatStudioDuration(elapsedSeconds)}" else "방송 준비", color = Color.White.copy(alpha = .74f), fontSize = 10.sp)
        }
        Surface(color = if (live) Colors.Live else Color.Black.copy(alpha = .34f), shape = RoundedCornerShape(12.dp)) {
            Text(if (live) "● LIVE  ${viewerCount.coerceAtLeast(0)}명" else "연결 준비", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudioConnectionCard(
    state: StudioConnectionState,
    loading: Boolean,
    message: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.padding(horizontal = 28.dp), color = Color(0xEEFFFFFF), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading || state == StudioConnectionState.Connecting || state == StudioConnectionState.WaitingForSession) {
                CircularProgressIndicator(Modifier.size(30.dp), color = Colors.Navy, strokeWidth = 3.dp)
            } else {
                Image(painterResource(R.drawable.camera_outline), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(Colors.Navy))
            }
            Text(
                when (state) {
                    StudioConnectionState.PermissionRequired -> "카메라와 마이크를 연결해주세요"
                    StudioConnectionState.WaitingForSession -> "방송 연결 정보를 받고 있어요"
                    StudioConnectionState.Connecting -> "LiveKit 방송 서버에 연결 중이에요"
                    StudioConnectionState.Failed -> "방송 연결을 확인해주세요"
                    StudioConnectionState.Live -> "방송 중"
                },
                color = Colors.Navy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            message?.let { Text(it, color = Colors.Muted, fontSize = 11.sp) }
            if (!loading && state in setOf(StudioConnectionState.PermissionRequired, StudioConnectionState.Failed)) {
                Button(onAction, Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                    Text(if (state == StudioConnectionState.PermissionRequired) "권한 허용하기" else "다시 연결", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StudioAuctionRail(auctions: List<AuctionSummary>, onStartAuction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("방송 상품 ${auctions.size}개", Modifier.padding(horizontal = 20.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auctions, key = AuctionSummary::auctionId) { auction ->
                Surface(color = Color(0xEFFFFFFF), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.width(260.dp).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(auction.title, color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("시작가 ${"%,d".format(auction.startPrice)}원", color = Colors.Muted, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onStartAuction(auction.auctionId) },
                            enabled = !auction.status.equals("ACTIVE", true),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                        ) { Text(if (auction.status.equals("ACTIVE", true)) "진행 중" else "경매 시작", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioControl(
    icon: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        Modifier.clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier.size(50.dp).background(
                when {
                    !enabled -> Color.Black.copy(alpha = .24f)
                    destructive -> Colors.Live
                    selected -> Color.White
                    else -> Color.Black.copy(alpha = .52f)
                },
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(icon), label, Modifier.size(23.dp), colorFilter = ColorFilter.tint(if (selected && !destructive) Colors.Navy else Color.White))
        }
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else .5f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun formatStudioDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3_600
    val minutes = safe % 3_600 / 60
    val remainder = safe % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainder)
}
