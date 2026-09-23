package com.ssafy.dib.feature.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import livekit.org.webrtc.RendererCommon
import livekit.org.webrtc.VideoSink
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 방송을 시작할 때만 물어보는 권한 묶음. 시청자 화면에서는 절대 요청하지 않는다. */
object LiveMediaPermissions {
    val REQUIRED: Array<String> = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    fun granted(context: Context): Boolean = REQUIRED.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** LiveKit 트랙을 그리는 표면. Room 이 바뀌면 렌더러를 새로 만들어야 해서 key 로 묶는다. */
@Composable
internal fun LiveVideoSurface(
    room: Room?,
    videoTrack: VideoTrack?,
    mirror: Boolean,
    fill: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (room == null || videoTrack == null) return
    key(room) { LiveVideoSurfaceContent(room, videoTrack, mirror, fill, modifier) }
}

@Composable
private fun LiveVideoSurfaceContent(
    room: Room,
    videoTrack: VideoTrack,
    mirror: Boolean,
    fill: Boolean,
    modifier: Modifier
) {
    val boundView = remember { mutableStateOf<TextureViewRenderer?>(null) }
    val boundTrack = remember { mutableStateOf<VideoTrack?>(null) }
    // 채우기(fill)를 요청해도 영상과 화면 비율이 크게 다르면 전체 보기로 바꾼다.
    // 태블릿·가로 화면으로 방송하면 영상이 4:3·가로형이라, 세로로 긴 폰 화면을 채우면 양옆이 절반 가까이 잘렸다
    val frameAspect = remember { mutableFloatStateOf(0f) }
    val viewSize = remember { mutableStateOf(IntSize.Zero) }

    if (fill) DisposableEffect(videoTrack) {
        val mainHandler = Handler(Looper.getMainLooper())
        // 프레임마다 상태를 바꾸지 않도록 비율이 달라졌을 때만 메인 스레드로 넘긴다
        var lastAspect = 0f
        val sink = VideoSink { frame ->
            val height = frame.rotatedHeight
            if (height > 0) {
                val aspect = frame.rotatedWidth.toFloat() / height
                if (abs(aspect - lastAspect) > 0.01f) {
                    lastAspect = aspect
                    mainHandler.post { frameAspect.floatValue = aspect }
                }
            }
        }
        videoTrack.addRenderer(sink)
        onDispose { videoTrack.removeRenderer(sink) }
    }

    DisposableEffect(Unit) {
        onDispose {
            boundView.value?.let { view ->
                boundTrack.value?.removeRenderer(view)
                runCatching { view.release() }
            }
            boundView.value = null
            boundTrack.value = null
        }
    }

    AndroidView(
        modifier = modifier.onSizeChanged { viewSize.value = it },
        factory = { context ->
            TextureViewRenderer(context).also { view ->
                room.initVideoRenderer(view)
                boundView.value = view
            }
        },
        update = { view ->
            if (boundTrack.value !== videoTrack) {
                boundTrack.value?.removeRenderer(view)
                videoTrack.addRenderer(view)
                boundTrack.value = videoTrack
            }
            view.setMirror(mirror)
            view.setScalingType(
                if (fill && fillKeepsMostOfFrame(frameAspect.floatValue, viewSize.value)) RendererCommon.ScalingType.SCALE_ASPECT_FILL
                else RendererCommon.ScalingType.SCALE_ASPECT_FIT
            )
        }
    )
}

/**
 * 화면을 채웠을 때 영상의 80% 이상이 보이면 채우기를 쓴다. 비율을 아직 모르면(첫 프레임 전) 채우기로 둔다.
 * 폰 세로 방송(9:16)을 폰(약 9:19.5)에서 보면 82% 가 보여 채우기, 태블릿 4:3 영상은 60% 남짓이라 전체 보기가 된다
 */
internal fun fillKeepsMostOfFrame(frameAspect: Float, viewSize: IntSize): Boolean {
    if (frameAspect <= 0f || viewSize.width <= 0 || viewSize.height <= 0) return true
    val viewAspect = viewSize.width.toFloat() / viewSize.height
    return min(frameAspect, viewAspect) / max(frameAspect, viewAspect) >= FILL_MIN_VISIBLE_RATIO
}

private const val FILL_MIN_VISIBLE_RATIO = 0.8f
