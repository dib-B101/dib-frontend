package com.ssafy.dib.data.remote.notification

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import okhttp3.RequestBody.Companion.toRequestBody

class NotificationRemoteDataSource(private val client: DibHttpClient) {
    fun getNotifications(cursor: String?, size: Int): ApiResult<NotificationPageResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.NOTIFICATIONS)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
            .apply { cursor?.takeIf(String::isNotBlank)?.let { addQueryParameter("cursor", it) } }
            .build()
        client.execute(
            client.requestBuilder(ApiRoutes.NOTIFICATIONS).url(url).get().build(),
            NotificationPageResponse.serializer()
        )
    }

    fun getUnreadCount(): ApiResult<NotificationUnreadCountResponse> = configured {
        val path = "${ApiRoutes.NOTIFICATIONS}/unread-count"
        client.execute(client.requestBuilder(path).get().build(), NotificationUnreadCountResponse.serializer())
    }

    fun markRead(notificationId: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.NOTIFICATIONS}/$notificationId/read"
        client.executeUnit(client.requestBuilder(path).patch("".toRequestBody(null)).build())
    }

    fun markAllRead(): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.NOTIFICATIONS}/read-all"
        client.executeUnit(client.requestBuilder(path).patch("".toRequestBody(null)).build())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message ?: "네트워크 주소 설정을 확인해주세요.", cause = error))
    }
}
