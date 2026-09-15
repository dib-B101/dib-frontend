package com.ssafy.dib.feature.main

import android.net.Uri

data class ProductImageSelection(val uri: Uri, val type: String)

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
