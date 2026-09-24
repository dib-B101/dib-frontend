package com.ssafy.dib.feature.main

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.ssafy.dib.domain.product.ProductImageUpload
import java.io.ByteArrayOutputStream
import java.io.File

/** 등록 화면에서 고른 사진 한 장. rotationDegrees 는 기존 이미지 처리 경로와의 호환을 위해 유지한다. */
data class ProductImageSelection(val uri: Uri, val rotationDegrees: Int = 0)

internal const val MAX_PRODUCT_IMAGE_BYTES = 10L * 1024L * 1024L
internal const val PRODUCT_IMAGE_POLICY_LABEL = "JPG, PNG, WEBP · 장당 10MB 이하"

// 회전해서 다시 인코딩할 때 긴 변 상한. 원본(4000px대)을 그대로 풀면 비트맵 하나에 50MB 를 넘겨 저사양 기기에서 OOM 이 난다
private const val ROTATED_UPLOAD_MAX_SIDE_PX = 3_000
private const val ROTATED_UPLOAD_JPEG_QUALITY = 92

internal fun productImageValidationMessage(contentResolver: ContentResolver, uris: List<Uri>): String? {
    if (uris.size > 10) return "상품 사진은 최대 10장까지 선택할 수 있어요."
    uris.forEach { uri ->
        if (resolveProductImageMediaType(contentResolver, uri) == null) {
            return "올릴 수 없는 이미지예요. $PRODUCT_IMAGE_POLICY_LABEL 파일만 선택해주세요."
        }
        val size = runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        if (size > MAX_PRODUCT_IMAGE_BYTES) {
            return "10MB를 넘는 이미지가 있어요. 장당 10MB 이하로 선택해주세요."
        }
    }
    return null
}

internal fun resolveProductImageMediaType(contentResolver: ContentResolver, uri: Uri): String? {
    val header = runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            ByteArray(12).also { buffer -> input.read(buffer) }
        }
    }.getOrNull() ?: return null
    return detectProductImageMediaType(header)
}

internal fun detectProductImageMediaType(header: ByteArray): String? = when {
    header.size >= 3 &&
        header[0].toInt() and 0xff == 0xff &&
        header[1].toInt() and 0xff == 0xd8 &&
        header[2].toInt() and 0xff == 0xff -> "image/jpeg"
    header.size >= 8 && header.copyOfRange(0, 8).contentEquals(
        byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    ) -> "image/png"
    header.size >= 12 &&
        String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
        String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
    else -> null
}

internal fun <T> moveProductImage(
    images: MutableList<T>,
    fromIndex: Int,
    toIndex: Int
): Boolean {
    if (fromIndex !in images.indices || toIndex !in images.indices || fromIndex == toIndex) return false
    val image = images.removeAt(fromIndex)
    images.add(toIndex, image)
    return true
}

/**
 * 촬영한 사진을 받을 임시 파일의 FileProvider URI. 캐시 아래 camera/ 에 두고(file_paths.xml) 카메라 앱에 쓰기 권한을 준다.
 * 파일을 못 만들면 null — 호출 쪽이 안내 문구를 보여준다.
 */
internal fun createProductCameraUri(context: Context): Uri? = runCatching {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("dib-camera-", ".jpg", directory)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()

/**
 * 사진에 적힌 EXIF 회전값(도). 카메라로 세워 찍은 JPEG 는 픽셀은 눕혀 저장되고 EXIF 에만 90도가 적혀 있어
 * BitmapFactory 로 그냥 그리면 사진이 눕는다. 미리보기와 업로드 둘 다 이 값을 더해 바로 세운다.
 */
internal fun productImageExifRotation(contentResolver: ContentResolver, uri: Uri): Int = runCatching {
    contentResolver.openInputStream(uri)?.use { input -> ExifInterface(input).rotationDegrees }
}.getOrNull() ?: 0

/** 사용자 회전 + EXIF 회전을 0~359 로 합친다. */
internal fun totalProductImageRotation(exifRotation: Int, userRotation: Int): Int = ((exifRotation + userRotation) % 360 + 360) % 360

/**
 * 미리보기 비트맵. 원본(수천 px)을 그대로 풀면 썸네일 하나에 수십 MB 가 들어가서 목표 크기까지 줄여서 디코딩하고,
 * EXIF·사용자 회전을 반영해 세운 채로 돌려준다.
 */
internal fun decodeProductImagePreview(
    contentResolver: ContentResolver,
    uri: Uri,
    userRotation: Int = 0,
    targetPx: Int = 512
): Bitmap? {
    val rotation = totalProductImageRotation(productImageExifRotation(contentResolver, uri), userRotation)
    val decoded = decodeSampled(contentResolver, uri, targetPx) ?: return null
    return rotateBitmap(decoded, rotation)
}

/**
 * 업로드할 바이트. 회전이 필요 없으면(EXIF 정상 + 사용자 회전 0) 원본 바이트를 그대로 보내 화질·용량을 지킨다.
 * 회전이 필요하면 픽셀 자체를 돌려 JPEG 로 다시 인코딩한다 — 서버 저장·관리자 웹·다른 화면 어디서도 EXIF 를 다시 읽지 않기 때문이다.
 */
internal fun prepareProductImageUpload(
    contentResolver: ContentResolver,
    selection: ProductImageSelection,
    index: Int
): ProductImageUpload {
    val uri = selection.uri
    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "product-$index.jpg"
    val mediaType = resolveProductImageMediaType(contentResolver, uri) ?: "application/octet-stream"
    val rotation = totalProductImageRotation(productImageExifRotation(contentResolver, uri), selection.rotationDegrees)
    if (rotation == 0) {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("선택한 사진을 읽을 수 없습니다.")
        return ProductImageUpload(fileName = fileName, mediaType = mediaType, bytes = bytes)
    }
    val decoded = decodeSampled(contentResolver, uri, ROTATED_UPLOAD_MAX_SIDE_PX) ?: error("선택한 사진을 읽을 수 없습니다.")
    val rotated = rotateBitmap(decoded, rotation)
    val output = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, ROTATED_UPLOAD_JPEG_QUALITY, output)
    return ProductImageUpload(
        fileName = fileName.substringBeforeLast('.') + ".jpg",
        mediaType = "image/jpeg",
        bytes = output.toByteArray()
    )
}

// 긴 변이 maxSidePx 를 넘지 않도록 inSampleSize(2의 거듭제곱)를 골라 디코딩한다
private fun decodeSampled(contentResolver: ContentResolver, uri: Uri, maxSidePx: Int): Bitmap? {
    // inJustDecodeBounds 디코딩은 비트맵을 돌려주지 않고(항상 null) 크기만 채운다. 성공 여부는 outWidth 로 본다
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSidePx) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return source
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    if (rotated !== source) source.recycle()
    return rotated
}
