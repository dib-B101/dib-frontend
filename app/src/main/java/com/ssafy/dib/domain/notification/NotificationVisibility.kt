package com.ssafy.dib.domain.notification

fun DomainNotification.isEnabledBy(trade: Boolean, live: Boolean, bookmark: Boolean): Boolean = when (category) {
    NotificationCategory.Trade -> trade
    NotificationCategory.Live -> live
    NotificationCategory.Bookmark -> bookmark
    NotificationCategory.Other -> true
}
