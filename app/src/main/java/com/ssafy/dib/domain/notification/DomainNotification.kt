package com.ssafy.dib.domain.notification

data class DomainNotification(
    val eventId: String,
    val type: String,
    val resourceType: String,
    val resourceId: String,
    val title: String,
    val body: String,
    val occurredAt: String
) {
    val category: NotificationCategory
        get() {
            val key = "$type $resourceType".uppercase()
            return when {
                "LIVE" in key -> NotificationCategory.Live
                listOf("BOOKMARK", "FAVORITE", "WISH").any(key::contains) -> NotificationCategory.Bookmark
                listOf("ORDER", "PAYMENT", "SHIP", "DELIVERY", "SETTLEMENT", "TRANSACTION").any(key::contains) -> NotificationCategory.Trade
                else -> NotificationCategory.Other
            }
        }
}

enum class NotificationCategory(val label: String) {
    Live("라이브"),
    Bookmark("찜"),
    Trade("거래"),
    Other("기타")
}
