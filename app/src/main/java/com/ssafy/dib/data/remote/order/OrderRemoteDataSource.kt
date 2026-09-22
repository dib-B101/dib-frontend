package com.ssafy.dib.data.remote.order

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.domain.order.OrderRole
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.builtins.ListSerializer

class OrderRemoteDataSource(private val client: DibHttpClient) {
    fun getOrders(role: OrderRole, cursor: String?, size: Int): ApiResult<OrderListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.ORDERS)
            .addQueryParameter("role", role.name)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(
            client.requestBuilder(ApiRoutes.ORDERS).url(url).get().build(),
            OrderListResponse.serializer()
        )
    }

    fun getOrder(orderId: String): ApiResult<OrderSummaryDto> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId"
        client.execute(client.requestBuilder(path).get().build(), OrderSummaryDto.serializer())
    }

    fun getShipment(orderId: String): ApiResult<ShipmentResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/shipment"
        client.execute(client.requestBuilder(path).get().build(), ShipmentResponse.serializer())
    }

    fun getShippingAddress(orderId: String): ApiResult<OrderShippingAddressResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/shipping-address"
        client.execute(client.requestBuilder(path).get().build(), OrderShippingAddressResponse.serializer())
    }

    fun updateShippingAddress(orderId: String, request: UpdateOrderAddressRequest): ApiResult<OrderShippingAddressResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/address"
        when (val updated = client.executeUnit(
            client.requestBuilder(path).patch(client.jsonBody(request, UpdateOrderAddressRequest.serializer())).build()
        )) {
            is ApiResult.Failure -> updated
            is ApiResult.Success -> getShippingAddress(orderId)
        }
    }

    fun getShippingCarriers(): ApiResult<List<CarrierDto>> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.CARRIERS).get().build(),
            ListSerializer(CarrierDto.serializer())
        )
    }

    fun registerShipment(orderId: String, carrier: String, trackingNumber: String, idempotencyKey: String): ApiResult<ShipmentResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/shipment"
        val body = DibJson.instance.encodeToString(
            ShipmentRegistrationRequest.serializer(),
            ShipmentRegistrationRequest(carrier, trackingNumber)
        ).toRequestBody("application/json".toMediaType())
        when (val registered = client.executeUnit(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKey)
                .post(body)
                .build()
        )) {
            is ApiResult.Failure -> registered
            is ApiResult.Success -> getShipment(orderId)
        }
    }

    fun getMessages(orderId: String, beforeChattingId: String?, size: Int): ApiResult<OrderMessageListResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/messages"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        beforeChattingId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("beforeChattingId", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), OrderMessageListResponse.serializer())
    }

    fun confirmPurchase(orderId: String): ApiResult<OrderConfirmationResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/confirm"
        client.execute(
            client.requestBuilder(path).post(okhttp3.RequestBody.EMPTY).build(),
            OrderConfirmationResponse.serializer()
        )
    }

    // 후기는 별점만. 서버가 구매확정 상태·구매자 본인·중복 여부를 검사한다
    fun writeReview(orderId: String, rating: Int): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/review"
        client.executeUnit(
            client.requestBuilder(path)
                .post(client.jsonBody(WriteReviewRequest(rating), WriteReviewRequest.serializer()))
                .build()
        )
    }

    fun acceptRunnerUpOffer(auctionId: String): ApiResult<OrderOfferAcceptanceResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/orders/accept"
        client.execute(
            client.requestBuilder(path).post(okhttp3.RequestBody.EMPTY).build(),
            OrderOfferAcceptanceResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(
                ApiFailure(
                    status = null,
                    code = ApiErrorCodes.CLIENT_NOT_CONFIGURED,
                    message = error.message ?: "네트워크 주소 설정을 확인해주세요.",
                    cause = error
                )
            )
        }
}
