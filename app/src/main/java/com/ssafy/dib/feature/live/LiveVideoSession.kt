package com.ssafy.dib.feature.live

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ssafy.dib.domain.live.LiveStreamSession
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.VideoPreset169
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 화면이 그대로 그릴 수 있게 정리한 영상 연결 상태. */
sealed interface LiveVideoState {
    /** 아직 연결을 시도하지 않은 상태(권한 없음·에뮬레이터·미리보기 등). */
    data object Idle : LiveVideoState
    /** 서버에서 방 접속 토큰을 받아오는 중. */
    data object Preparing : LiveVideoState
    data object Connecting : LiveVideoState
    data object Connected : LiveVideoState
    data object Reconnecting : LiveVideoState
    data object Disconnected : LiveVideoState
    data class Failed(val message: String) : LiveVideoState
}

enum class LiveVideoRole { PUBLISHER, VIEWER }

/**
 * LiveKit Room 을 감싸서 화면이 SDK 타입을 직접 다루지 않게 한다.
 * 상태는 Compose 스냅샷 상태로 노출하고, 정리는 [release] 한 번으로 끝낸다.
 */
@Stable
class LiveVideoSession internal constructor(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val role: LiveVideoRole
) {
    var state by mutableStateOf<LiveVideoState>(LiveVideoState.Idle)
        private set

    /** 화면에 그릴 트랙. 방송자는 내 카메라, 시청자는 구독한 원격 트랙. */
    var videoTrack by mutableStateOf<VideoTrack?>(null)
        private set

    var room by mutableStateOf<Room?>(null)
        private set

    var cameraEnabled by mutableStateOf(false)
        private set

    var microphoneEnabled by mutableStateOf(false)
        private set

    var usingFrontCamera by mutableStateOf(true)
        private set

    private var job: Job? = null
    private var eventsJob: Job? = null
    private var tokenProvider: (suspend () -> Result<LiveStreamSession>)? = null

    val isBusy: Boolean
        get() = state is LiveVideoState.Preparing || state is LiveVideoState.Connecting

    internal fun start(provider: suspend () -> Result<LiveStreamSession>) {
        if (job?.isActive == true) return
        tokenProvider = provider
        state = LiveVideoState.Preparing
        job = scope.launch {
            val credentials = provider().getOrElse { error ->
                state = LiveVideoState.Failed(error.message?.takeIf(String::isNotBlank) ?: DEFAULT_TOKEN_ERROR)
                return@launch
            }
            connectTo(credentials)
        }
    }

    /** 실패한 연결만 다시 시도한다. 채팅·경매는 별도 소켓이라 영향이 없다. */
    fun retry() {
        val provider = tokenProvider ?: return
        disconnectRoom()
        job?.cancel()
        job = null
        start(provider)
    }

    private suspend fun connectTo(credentials: LiveStreamSession) {
        val created = LiveKit.create(
            appContext = appContext,
            options = RoomOptions(
                // 피드에는 활성 방송 하나만 연결하므로 시청자는 항상 최고 품질 레이어를 요청한다.
                adaptiveStream = false,
                dynacast = role == LiveVideoRole.PUBLISHER,
                videoTrackCaptureDefaults = if (role == LiveVideoRole.PUBLISHER) {
                    LocalVideoTrackOptions(captureParams = VideoPreset169.H1080.capture)
                } else null,
                videoTrackPublishDefaults = if (role == LiveVideoRole.PUBLISHER) {
                    VideoTrackPublishDefaults(
                        videoEncoding = VideoPreset169.H1080.encoding,
                        simulcast = true,
                        simulcastLayers = listOf(VideoPreset169.H360, VideoPreset169.H720)
                    )
                } else null
            )
        )
        room = created
        state = LiveVideoState.Connecting
        eventsJob = scope.launch {
            created.events.collect { event -> onRoomEvent(event) }
        }
        try {
            created.connect(url = credentials.serverUrl, token = credentials.token)
            state = LiveVideoState.Connected
            if (role == LiveVideoRole.PUBLISHER) publishLocalMedia(created)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            state = LiveVideoState.Failed(error.message?.takeIf(String::isNotBlank) ?: DEFAULT_CONNECT_ERROR)
        }
    }

    private suspend fun publishLocalMedia(target: Room) {
        // 방송자가 직전에 꺼 둔 카메라·마이크는 꺼진 채로 다시 붙는다 (LiveHostMediaPreference)
        val wantMicrophone = LiveHostMediaPreference.microphoneEnabled
        val wantCamera = LiveHostMediaPreference.cameraEnabled
        runCatching { target.localParticipant.setMicrophoneEnabled(wantMicrophone) }
            .onSuccess { microphoneEnabled = wantMicrophone }
        runCatching { target.localParticipant.setCameraEnabled(wantCamera) }
            .onSuccess { cameraEnabled = wantCamera }
        if (wantCamera) bindLocalCameraTrack()
    }

    private fun onRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.Connected -> state = LiveVideoState.Connected
            is RoomEvent.Reconnecting -> state = LiveVideoState.Reconnecting
            is RoomEvent.Reconnected -> state = LiveVideoState.Connected
            is RoomEvent.Disconnected -> {
                videoTrack = null
                state = event.error?.message?.takeIf(String::isNotBlank)
                    ?.let { LiveVideoState.Failed(it) }
                    ?: LiveVideoState.Disconnected
            }
            is RoomEvent.FailedToConnect -> {
                videoTrack = null
                state = LiveVideoState.Failed(
                    event.error.message?.takeIf(String::isNotBlank) ?: DEFAULT_CONNECT_ERROR
                )
            }
            is RoomEvent.TrackSubscribed -> if (role == LiveVideoRole.VIEWER) {
                (event.track as? VideoTrack)?.let { videoTrack = it }
            }
            is RoomEvent.TrackUnsubscribed -> if (event.track === videoTrack) videoTrack = null
            else -> Unit
        }
    }

    private fun bindLocalCameraTrack() {
        videoTrack = localCameraTrack()
    }

    private fun localCameraTrack(): LocalVideoTrack? =
        room?.localParticipant?.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack

    fun updateCameraEnabled(enabled: Boolean) {
        val target = room ?: return
        scope.launch {
            runCatching { target.localParticipant.setCameraEnabled(enabled) }
                .onSuccess {
                    cameraEnabled = enabled
                    if (role == LiveVideoRole.PUBLISHER) LiveHostMediaPreference.remember(camera = enabled)
                    if (enabled) bindLocalCameraTrack() else videoTrack = null
                }
        }
    }

    fun updateMicrophoneEnabled(enabled: Boolean) {
        val target = room ?: return
        scope.launch {
            runCatching { target.localParticipant.setMicrophoneEnabled(enabled) }
                .onSuccess {
                    microphoneEnabled = enabled
                    if (role == LiveVideoRole.PUBLISHER) LiveHostMediaPreference.remember(microphone = enabled)
                }
        }
    }

    fun switchCamera() {
        val track = localCameraTrack() ?: return
        val next = when (track.options.position) {
            CameraPosition.FRONT -> CameraPosition.BACK
            CameraPosition.BACK -> CameraPosition.FRONT
            else -> null
        }
        runCatching { track.switchCamera(position = next) }
            .onSuccess { usingFrontCamera = next != CameraPosition.BACK }
    }

    fun release() {
        job?.cancel()
        job = null
        disconnectRoom()
        state = LiveVideoState.Idle
    }

    private fun disconnectRoom() {
        eventsJob?.cancel()
        eventsJob = null
        val target = room
        videoTrack = null
        room = null
        cameraEnabled = false
        microphoneEnabled = false
        target?.let {
            runCatching { it.disconnect() }
            runCatching { it.release() }
        }
    }

    private companion object {
        const val DEFAULT_TOKEN_ERROR = "영상 연결 정보를 받지 못했어요."
        const val DEFAULT_CONNECT_ERROR = "영상 서버에 연결하지 못했어요."
    }
}

/**
 * 방송자의 카메라·마이크 켬/끔을 화면 밖에 기억한다.
 * 콘솔을 나가면 세션이 release 되고 다시 들어오면 새 세션이 무조건 둘 다 켰다 — 카메라를 꺼 두고 방송하다
 * 알림으로 돌아온 판매자의 카메라가 저절로 켜지던 문제다. 방송이 끝나면 reset 해 다음 방송은 기본값(둘 다 켬)으로 시작한다.
 */
object LiveHostMediaPreference {
    var cameraEnabled: Boolean = true
        private set
    var microphoneEnabled: Boolean = true
        private set

    fun remember(camera: Boolean = cameraEnabled, microphone: Boolean = microphoneEnabled) {
        cameraEnabled = camera
        microphoneEnabled = microphone
    }

    fun reset() {
        cameraEnabled = true
        microphoneEnabled = true
    }
}

/**
 * 화면 수명에 맞춰 LiveKit 연결을 열고 닫는다.
 * [enabled] 가 false 면 접속을 시도하지 않아 시청자·에뮬레이터·권한 거절 상황에서도 안전하다.
 */
@Composable
fun rememberLiveVideoSession(
    role: LiveVideoRole,
    enabled: Boolean,
    tokenProvider: (suspend () -> Result<LiveStreamSession>)?
): LiveVideoSession {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val session = remember(role) { LiveVideoSession(appContext, scope, role) }
    val currentProvider by rememberUpdatedState(tokenProvider)
    DisposableEffect(session, enabled, tokenProvider != null) {
        if (enabled && tokenProvider != null) {
            session.start { currentProvider?.invoke() ?: Result.failure(IllegalStateException("영상 연결 정보를 받지 못했어요.")) }
        }
        onDispose { session.release() }
    }
    return session
}
