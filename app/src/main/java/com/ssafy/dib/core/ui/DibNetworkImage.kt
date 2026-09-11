package com.ssafy.dib.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun DibNetworkImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderText: String = "상품 이미지"
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = imageUrl?.let(DibBitmapLoader::cached),
        key1 = imageUrl
    ) {
        value = imageUrl?.takeIf(String::isNotBlank)?.let { url ->
            DibBitmapLoader.cached(url) ?: withContext(Dispatchers.IO) { DibBitmapLoader.load(url) }
        }
    }
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFE4E7EB)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Text(placeholderText, color = Color(0xFF7C8490), fontSize = 11.sp)
        }
    }
}

private object DibBitmapLoader {
    private const val CACHE_KILOBYTES = 32 * 1024
    private const val MAX_DIMENSION = 1_600
    private val client = OkHttpClient()
    private val cache = object : LruCache<String, Bitmap>(CACHE_KILOBYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun cached(url: String): Bitmap? = cache.get(url)

    fun load(url: String): Bitmap? = runCatching {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val bytes = response.body.bytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
        }
    }.getOrNull()

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_DIMENSION || height / sample > MAX_DIMENSION) sample *= 2
        return sample
    }
}
