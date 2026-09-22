package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.notification.NotificationDto
import com.ssafy.dib.data.remote.notification.NotificationRemoteDataSource
import com.ssafy.dib.domain.notification.DomainNotification
import com.ssafy.dib.domain.notification.NotificationPage
import com.ssafy.dib.domain.notification.NotificationRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class NotificationRepositoryImpl(private val remote: NotificationRemoteDataSource) : NotificationRepository {
    override fun getNotifications(cursor: String?, size: Int): ApiResult<NotificationPage> =
        when (val result = remote.getNotifications(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                NotificationPage(result.value.items.map(NotificationDto::toDomain), result.value.nextCursor, result.value.hasNext),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun markRead(notificationId: String): ApiResult<Unit> = remote.markRead(notificationId)
    override fun getUnreadCount(): ApiResult<Int> = when (val result = remote.getUnreadCount()) {
        is ApiResult.Success -> ApiResult.Success(result.value.unreadCount.coerceAtLeast(0), result.status)
        is ApiResult.Failure -> result
    }
    override fun markAllRead(): ApiResult<Unit> = remote.markAllRead()
}

internal fun NotificationDto.toDomain(): DomainNotification {
    // 우선순위는 서버 Notification.resourceType() 과 같다. 소켓 푸시는 서버가 계산해 주지만 목록 조회는 여기서 만든다
    val (resourceType, resourceId) = when {
        orderId != null -> "ORDER" to orderId.idValue()
        liveBroadcastId != null -> "LIVE" to liveBroadcastId.idValue()
        auctionId != null -> "AUCTION" to auctionId.idValue()
        productId != null -> "PRODUCT" to productId.idValue()
        else -> "SYSTEM" to notificationId.idValue()
    }
    return DomainNotification(
        eventId = notificationId.idValue(),
        type = type,
        resourceType = resourceType,
        resourceId = resourceId,
        title = title,
        body = content,
        occurredAt = createdAt,
        isRead = read
    )
}

private fun JsonElement.idValue(): String = (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
