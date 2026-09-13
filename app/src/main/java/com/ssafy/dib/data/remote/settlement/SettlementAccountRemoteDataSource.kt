package com.ssafy.dib.data.remote.settlement

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class SettlementAccountRemoteDataSource(private val client: DibHttpClient) {
    fun getAccount(): ApiResult<SettlementAccountResponse> = configured {
        client.execute(client.requestBuilder(ApiRoutes.SETTLEMENT_ACCOUNT).get().build(), SettlementAccountResponse.serializer())
    }

    fun saveAccount(request: SaveSettlementAccountRequest): ApiResult<SettlementAccountResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.SETTLEMENT_ACCOUNT).put(client.jsonBody(request, SaveSettlementAccountRequest.serializer())).build(),
            SettlementAccountResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
    }
}
