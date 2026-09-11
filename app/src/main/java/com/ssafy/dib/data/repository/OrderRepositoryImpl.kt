package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.order.OrderRemoteDataSource
import com.ssafy.dib.data.remote.order.OrderSummaryDto
import com.ssafy.dib.domain.order.OrderRepository
import com.ssafy.dib.domain.order.OrderRole
import com.ssafy.dib.domain.order.OrderSummary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class OrderRepositoryImpl(private val remote: OrderRemoteDataSource) : OrderRepository {
    override fun getOrders(role: OrderRole, size: Int): ApiResult<List<OrderSummary>> =
        when (val result = remote.getOrders(role, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(OrderSummaryDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }
}

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
