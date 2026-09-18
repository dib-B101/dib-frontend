package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.product.CategoryDto
import com.ssafy.dib.data.remote.product.ProductRemoteDataSource
import com.ssafy.dib.data.remote.product.ProductDetailResponse
import com.ssafy.dib.data.remote.product.ProductCardDto
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductRegistrationResult
import com.ssafy.dib.domain.product.ProductDetail
import com.ssafy.dib.domain.product.ProductUpdate
import com.ssafy.dib.domain.product.ProductUpdateResult
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.domain.product.RegisteredProductPage
import com.ssafy.dib.domain.product.ProductRepository
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class ProductRepositoryImpl(private val remote: ProductRemoteDataSource) : ProductRepository {
    override fun getCategories(): ApiResult<List<ProductCategory>> =
        when (val result = remote.getCategories()) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(CategoryDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun getProduct(productId: String): ApiResult<ProductDetail> =
        when (val result = remote.getProduct(productId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getSellerProducts(memberId: String): ApiResult<List<RegisteredProduct>> =
        when (val result = remote.getSellerProducts(memberId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.map(ProductCardDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun getSimilarProducts(productId: String, size: Int): ApiResult<List<RegisteredProduct>> =
        when (val result = remote.getSimilarProducts(productId, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(ProductCardDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun getMyProducts(status: String?, cursor: String?, size: Int): ApiResult<RegisteredProductPage> =
        when (val result = remote.getMyProducts(status, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                RegisteredProductPage(
                    items = result.value.items.map(ProductCardDto::toDomain),
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun searchProducts(query: String, categoryId: String?, cursor: String?, size: Int): ApiResult<RegisteredProductPage> =
        when (val result = remote.searchProducts(query, categoryId, cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                RegisteredProductPage(
                    items = result.value.items.map(ProductCardDto::toDomain),
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun registerProduct(registration: ProductRegistration, idempotencyKey: String): ApiResult<ProductRegistrationResult> =
        when (val result = remote.registerProduct(registration, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(
                ProductRegistrationResult(
                    productId = result.value.productId?.idValue().orEmpty(),
                    status = result.value.status.orEmpty(),
                    thumbnailUrl = result.value.thumbnailUrl,
                    createdAt = result.value.createdAt.orEmpty()
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun deleteProduct(productId: String, idempotencyKey: String): ApiResult<Unit> =
        remote.deleteProduct(productId, idempotencyKey)

    override fun updateProduct(productId: String, update: ProductUpdate): ApiResult<ProductUpdateResult> =
        when (val result = remote.updateProduct(productId, update)) {
            is ApiResult.Success -> ApiResult.Success(
                ProductUpdateResult(
                    productId = result.value.productId?.idValue().orEmpty(),
                    status = result.value.status.orEmpty(),
                    thumbnailUrl = result.value.thumbnailUrl,
                    updatedAt = result.value.updatedAt,
                    moderationReason = result.value.moderationReason,
                    moderationStage = result.value.moderationStage,
                    moderatedAt = result.value.moderatedAt
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }
}

internal fun CategoryDto.toDomain() = ProductCategory(categoryId.idValue(), name)
internal fun ProductCardDto.toDomain() = RegisteredProduct(
    productId = productId?.idValue().orEmpty(),
    title = title ?: name ?: "등록 상품",
    condition = condition.orEmpty(),
    status = (productStatus ?: status).orEmpty(),
    thumbnailUrl = thumbnailUrl,
    // 경매가 없는 상품은 아래 값을 null 로 유지해야 화면에서 "가격 미정" 으로 표시된다
    auctionId = auctionId?.idValue(),
    startPrice = startPrice,
    currentPrice = currentPrice,
    auctionTimeSeconds = auctionTime,
    auctionStatus = auctionStatus,
    bidCount = bidCount
)

internal fun ProductDetailResponse.toDomain(): ProductDetail {
    val imageUrls = product.images.mapNotNull { image ->
        (image as? JsonPrimitive)?.contentOrNull ?: runCatching {
            val objectValue = image.jsonObject
            (objectValue["imageUrl"] as? JsonPrimitive)?.contentOrNull
                ?: (objectValue["url"] as? JsonPrimitive)?.contentOrNull
        }.getOrNull()
    }.filter(String::isNotBlank).distinct()
    return ProductDetail(
        productId = product.productId?.idValue().orEmpty(),
        memberId = product.memberId?.idValue().orEmpty(),
        categoryId = product.categoryId?.idValue().orEmpty(),
        title = product.title,
        description = product.description,
        condition = product.condition,
        modelName = product.modelName,
        releaseYear = product.releaseYear,
        marketPrice = product.marketPrice,
        thumbnailUrl = product.thumbnailUrl,
        status = product.status,
        imageUrls = imageUrls.ifEmpty { listOfNotNull(product.thumbnailUrl?.takeIf(String::isNotBlank)) },
        sellerNickname = sellerSummary?.nickname ?: product.nickname,
        sellerRating = sellerSummary?.rating,
        sellerTradeCount = sellerSummary?.tradeCount ?: sellerSummary?.completedTradeCount,
        moderationReason = product.moderationReason,
        moderationStage = product.moderationStage,
        moderatedAt = product.moderatedAt
    )
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
