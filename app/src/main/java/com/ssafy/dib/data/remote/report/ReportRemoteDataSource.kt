package com.ssafy.dib.data.remote.report

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class ReportRemoteDataSource(private val client: DibHttpClient) {
    fun getMyReports(cursor: String?, size: Int): ApiResult<ReportListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.REPORTS)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(
            client.requestBuilder(ApiRoutes.REPORTS).url(url).get().build(),
            ReportListResponse.serializer()
        )
    }

    fun reportAuction(auctionId: String, content: String, idempotencyKey: String): ApiResult<CreateReportResponse> =
        create("${ApiRoutes.AUCTIONS}/$auctionId/reports", CreateReportRequest(content), idempotencyKey)

    fun reportMember(memberId: String, content: String, idempotencyKey: String): ApiResult<CreateReportResponse> =
        create("/api/v1/members/$memberId/reports", CreateReportRequest(content), idempotencyKey)

    fun reportOrder(orderId: String, content: String, type: String, idempotencyKey: String): ApiResult<CreateReportResponse> = configured {
        val body = CreateOrderReportRequest(content, type.uppercase().takeIf { it == "CHATTING" } ?: "ORDER")
        client.execute(
            client.requestBuilder("${ApiRoutes.ORDERS}/$orderId/reports")
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(body, CreateOrderReportRequest.serializer()))
                .build(),
            CreateReportResponse.serializer()
        )
    }

    // 백엔드에 라이브 전용 신고 경로가 없어, 관리자가 판단 근거로 볼 수 있도록 방송 id를 회원 신고 본문 끝에 덧붙여 보낸다.
    fun reportLiveParticipant(liveBroadcastId: String, memberId: String, content: String, idempotencyKey: String): ApiResult<CreateReportResponse> =
        create("/api/v1/members/$memberId/reports", CreateReportRequest(withLiveBroadcast(content, liveBroadcastId)), idempotencyKey)

    private fun withLiveBroadcast(content: String, liveBroadcastId: String): String =
        if (liveBroadcastId.isBlank()) content else content.trimEnd() + "\n[라이브 방송] " + liveBroadcastId

    private fun create(path: String, body: CreateReportRequest, idempotencyKey: String): ApiResult<CreateReportResponse> = configured {
        client.execute(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(body, CreateReportRequest.serializer()))
                .build(),
            CreateReportResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
        }
}
