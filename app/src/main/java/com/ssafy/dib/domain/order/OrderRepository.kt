package com.ssafy.dib.domain.order

import com.ssafy.dib.core.network.ApiResult

enum class OrderRole { BUYER, SELLER }

data class OrderSummary(
    val orderId: String,
    val auctionId: String,
    val productId: String,
    val title: String,
    val finalPrice: Int,
    val status: String,
    val updatedAt: String?
)

interface OrderRepository {
    fun getOrders(role: OrderRole, size: Int = 30): ApiResult<List<OrderSummary>>
}
