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
