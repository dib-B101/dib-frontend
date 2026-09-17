package com.ssafy.dib.feature.main

import android.content.ContentResolver
import android.net.Uri

data class ProductImageSelection(val uri: Uri, val type: String)

internal const val MAX_PRODUCT_IMAGE_BYTES = 10L * 1024L * 1024L
internal const val PRODUCT_IMAGE_POLICY_LABEL = "JPG, PNG, WEBP · 장당 10MB 이하"

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

internal val secondaryProductImageTypes = listOf("LEFT", "RIGHT", "BACK", "TOP", "BOTTOM")

internal fun defaultProductImageTypes(count: Int): List<String> = List(count.coerceAtLeast(0)) { index ->
    if (index == 0) "FRONT" else secondaryProductImageTypes[(index - 1) % secondaryProductImageTypes.size]
}

internal fun nextProductImageType(current: String): String {
    val index = secondaryProductImageTypes.indexOf(current)
    return secondaryProductImageTypes[(index + 1).mod(secondaryProductImageTypes.size)]
}

internal fun productImageTypeLabel(type: String): String = when (type) {
    "FRONT" -> "정면"
    "LEFT" -> "왼쪽"
    "RIGHT" -> "오른쪽"
    "BACK" -> "뒷면"
    "TOP" -> "윗면"
    "BOTTOM" -> "아랫면"
    else -> "방향 선택"
}

internal fun <T> moveProductImage(
    images: MutableList<T>,
    imageTypes: MutableList<String>,
    fromIndex: Int,
    toIndex: Int
): Boolean {
    if (images.size != imageTypes.size || fromIndex !in images.indices || toIndex !in images.indices || fromIndex == toIndex) {
        return false
    }
    val image = images.removeAt(fromIndex)
    images.add(toIndex, image)
    val type = imageTypes.removeAt(fromIndex)
    imageTypes.add(toIndex, type)

    val newRepresentativeOriginalType = imageTypes.first()
    imageTypes.indices.drop(1).filter { imageTypes[it] == "FRONT" }.forEachIndexed { index, position ->
        imageTypes[position] = newRepresentativeOriginalType.takeIf { it != "FRONT" }
            ?: secondaryProductImageTypes[index % secondaryProductImageTypes.size]
    }
    imageTypes[0] = "FRONT"
    return true
}
