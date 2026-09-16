package com.ssafy.dib.domain.notification

import com.ssafy.dib.core.network.ApiResult

interface NotificationRepository {
    fun getNotifications(cursor: String? = null, size: Int = 30): ApiResult<NotificationPage>
    fun getUnreadCount(): ApiResult<Int>
    fun markRead(notificationId: String): ApiResult<Unit>
    fun markAllRead(): ApiResult<Unit>
}
