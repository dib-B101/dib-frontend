package com.ssafy.dib.core.navigation

internal fun hasUsableNextCursor(
    hasNext: Boolean,
    nextCursor: String?,
    requestedCursor: String?
): Boolean = hasNext && !nextCursor.isNullOrBlank() && nextCursor != requestedCursor
