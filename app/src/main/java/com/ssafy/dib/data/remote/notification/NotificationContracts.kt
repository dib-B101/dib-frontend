package com.ssafy.dib.data.remote.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NotificationPageResponse(
    val items: List<NotificationDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable data class NotificationUnreadCountResponse(val unreadCount: Int = 0)

@Serializable
data class NotificationDto(
    val notificationId: JsonElement,
    val type: String,
    val title: String,
    val content: String,
    @SerialName("isRead") val read: Boolean = false,
    val auctionId: JsonElement? = null,
    val productId: JsonElement? = null,
    val liveBroadcastId: JsonElement? = null,
    val bidId: JsonElement? = null,
    val orderId: JsonElement? = null,
    val createdAt: String
)
