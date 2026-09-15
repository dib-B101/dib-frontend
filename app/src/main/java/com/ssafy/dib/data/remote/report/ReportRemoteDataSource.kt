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
        create("${ApiRoutes.AUCTIONS}/$auctionId/reports", CreateReportRequest(content, "AUCTION"), idempotencyKey)

    fun reportMember(memberId: String, content: String, idempotencyKey: String): ApiResult<CreateReportResponse> =
        create("/api/v1/members/$memberId/reports", CreateReportRequest(content, "MEMBER"), idempotencyKey)

    fun reportLiveParticipant(liveBroadcastId: String, memberId: String, content: String, idempotencyKey: String): ApiResult<CreateReportResponse> =
        create("${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/participants/$memberId/reports", CreateReportRequest(content, "MEMBER"), idempotencyKey)

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
