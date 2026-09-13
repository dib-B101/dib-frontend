package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.order.OrderRemoteDataSource
import com.ssafy.dib.data.remote.order.OrderSummaryDto
import com.ssafy.dib.domain.order.OrderRepository
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.order.OrderShipment
import com.ssafy.dib.domain.order.OrderMessage
import com.ssafy.dib.domain.order.OrderSummary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class OrderRepositoryImpl(private val remote: OrderRemoteDataSource) : OrderRepository {
    override fun getOrders(role: OrderRole, size: Int): ApiResult<List<OrderSummary>> =
        when (val result = remote.getOrders(role, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(OrderSummaryDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun getOrder(orderId: String): ApiResult<OrderSummary> =
        when (val result = remote.getOrder(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getShipment(orderId: String): ApiResult<OrderShipment> =
        when (val result = remote.getShipment(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun registerShipment(orderId: String, trackingNumber: String, idempotencyKey: String): ApiResult<OrderShipment> =
        when (val result = remote.registerShipment(orderId, trackingNumber, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getMessages(orderId: String, beforeChattingId: String?, size: Int): ApiResult<List<OrderMessage>> =
        when (val result = remote.getMessages(orderId, beforeChattingId, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map { message ->
                OrderMessage(message.chattingId.idValue(), message.memberId.idValue(), message.content, message.time)
            }, result.status)
            is ApiResult.Failure -> result
        }

    override fun confirmPurchase(orderId: String): ApiResult<String> =
        when (val result = remote.confirmPurchase(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.status, result.status)
            is ApiResult.Failure -> result
        }
}

internal fun com.ssafy.dib.data.remote.order.ShipmentResponse.toDomain(): OrderShipment = OrderShipment(
    orderId = orderId.idValue(),
    trackingNumber = trackingNumber,
    status = status,
    carrierStatus = carrierStatus,
    lastCheckedAt = lastCheckedAt,
    isStale = isStale,
    updatedAt = updatedAt
)

internal fun OrderSummaryDto.toDomain(): OrderSummary {
    val core = order
    return OrderSummary(
        orderId = (orderId ?: core?.orderId).idValue(),
        auctionId = (auctionId ?: core?.auctionId ?: auction?.auctionId).idValue(),
        productId = (productId ?: core?.productId ?: product?.productId ?: auction?.productId).idValue(),
        title = product?.title ?: product?.name ?: auction?.title ?: "거래 상품",
        finalPrice = (finalPrice ?: amount ?: core?.finalPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        status = status ?: core?.status ?: "PENDING",
        updatedAt = updatedAt ?: core?.updatedAt ?: createdAt ?: core?.createdAt
    )
}

private fun kotlinx.serialization.json.JsonElement?.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: this?.toString()?.trim('"').orEmpty()
