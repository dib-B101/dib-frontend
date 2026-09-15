package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.order.OrderRemoteDataSource
import com.ssafy.dib.data.remote.order.OrderSummaryDto
import com.ssafy.dib.domain.order.OrderRepository
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.order.OrderShipment
import com.ssafy.dib.domain.order.OrderMessage
import com.ssafy.dib.domain.order.OrderMessagePage
import com.ssafy.dib.domain.order.OrderPage
import com.ssafy.dib.domain.order.OrderSummary
import com.ssafy.dib.domain.order.OrderShippingAddress
import com.ssafy.dib.domain.order.ShippingCarrier
import com.ssafy.dib.domain.order.isOrderChatWritable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class OrderRepositoryImpl(private val remote: OrderRemoteDataSource) : OrderRepository {
    override fun getOrders(role: OrderRole, cursor: String?, size: Int): ApiResult<OrderPage> =
        when (val result = remote.getOrders(role, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                OrderPage(
                    items = result.value.items.map(OrderSummaryDto::toDomain),
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
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

    override fun getShippingAddress(orderId: String): ApiResult<OrderShippingAddress> =
        when (val result = remote.getShippingAddress(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getShippingCarriers(): ApiResult<List<ShippingCarrier>> =
        when (val result = remote.getShippingCarriers()) {
            is ApiResult.Success -> ApiResult.Success(
                result.value.map { carrier -> ShippingCarrier(carrier.code, carrier.name ?: carrierName(carrier.code)) },
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun registerShipment(orderId: String, carrier: String, trackingNumber: String, idempotencyKey: String): ApiResult<OrderShipment> =
        when (val result = remote.registerShipment(orderId, carrier, trackingNumber, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getMessages(orderId: String, beforeChattingId: String?, size: Int): ApiResult<OrderMessagePage> =
        when (val result = remote.getMessages(orderId, beforeChattingId, size)) {
            is ApiResult.Success -> ApiResult.Success(
                OrderMessagePage(
                    items = result.value.items.map { message ->
                        OrderMessage(message.chattingId.idValue(), message.memberId.idValue(), message.content, message.time)
                    },
                    hasMore = result.value.hasMore
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun confirmPurchase(orderId: String): ApiResult<String> =
        when (val result = remote.confirmPurchase(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.status, result.status)
            is ApiResult.Failure -> result
        }
}

private fun carrierName(code: String): String = when (code.uppercase()) {
    "CJ" -> "CJ대한통운"
    "HANJIN" -> "한진택배"
    "LOGEN" -> "로젠택배"
    "LOTTE" -> "롯데택배"
    "EPOST" -> "우체국택배"
    else -> code
}

internal fun com.ssafy.dib.data.remote.order.ShipmentResponse.toDomain(): OrderShipment = OrderShipment(
    orderId = orderId.idValue(),
    trackingNumber = trackingNumber,
    status = status,
    carrierStatus = carrierStatus,
    lastCheckedAt = lastCheckedAt,
    isStale = isStale,
    updatedAt = updatedAt,
    carrier = carrier
)

internal fun com.ssafy.dib.data.remote.order.OrderShippingAddressResponse.toDomain(): OrderShippingAddress {
    val value = address.jsonObject
    return OrderShippingAddress(
        name = value.stringValue("name").ifBlank { "배송지" },
        postalCode = value.stringValue("number"),
        address = value.stringValue("address")
    )
}

internal fun OrderSummaryDto.toDomain(): OrderSummary {
    val core = order
    val resolvedStatus = status ?: core?.status ?: "PENDING"
    return OrderSummary(
        orderId = (orderId ?: core?.orderId).idValue(),
        auctionId = (auctionId ?: core?.auctionId ?: auction?.auctionId).idValue(),
        productId = (productId ?: core?.productId ?: product?.productId ?: auction?.productId).idValue(),
        title = product?.title ?: product?.name ?: auction?.title ?: "거래 상품",
        finalPrice = (finalPrice ?: amount ?: core?.finalPrice ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        status = resolvedStatus,
        updatedAt = updatedAt ?: core?.updatedAt ?: createdAt ?: core?.createdAt,
        paymentId = payment?.paymentId.idValue().takeIf(String::isNotBlank),
        chattingReadOnly = chattingReadOnly ?: !isOrderChatWritable(resolvedStatus)
    )
}

private fun kotlinx.serialization.json.JsonElement?.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: this?.toString()?.trim('"').orEmpty()

private fun kotlinx.serialization.json.JsonObject.stringValue(key: String): String =
    (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
