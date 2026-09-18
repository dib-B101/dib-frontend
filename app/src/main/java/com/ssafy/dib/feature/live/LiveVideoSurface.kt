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
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

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
    modifier: Modifier = Modifier
) {
    if (room == null || videoTrack == null) return
    key(room) { LiveVideoSurfaceContent(room, videoTrack, mirror, modifier) }
}

@Composable
private fun LiveVideoSurfaceContent(
    room: Room,
    videoTrack: VideoTrack,
    mirror: Boolean,
    modifier: Modifier
) {
    val boundView = remember { mutableStateOf<TextureViewRenderer?>(null) }
    val boundTrack = remember { mutableStateOf<VideoTrack?>(null) }

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
        modifier = modifier,
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
        }
    )
}
