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
import com.ssafy.dib.domain.product.RegisteredProduct
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

    override fun getMyProducts(status: String?, size: Int): ApiResult<List<RegisteredProduct>> =
        when (val result = remote.getMyProducts(status, size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(ProductCardDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun registerProduct(registration: ProductRegistration): ApiResult<ProductRegistrationResult> =
        when (val result = remote.registerProduct(registration)) {
            is ApiResult.Success -> ApiResult.Success(
                ProductRegistrationResult(
                    productId = result.value.productId.idValue(),
                    status = result.value.status,
                    thumbnailUrl = result.value.thumbnailUrl,
                    createdAt = result.value.createdAt
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }
}

internal fun CategoryDto.toDomain() = ProductCategory(categoryId.idValue(), name)
internal fun ProductCardDto.toDomain() = RegisteredProduct(productId.idValue(), title ?: name ?: "등록 상품", condition, status, thumbnailUrl)

internal fun ProductDetailResponse.toDomain(): ProductDetail {
    val imageUrls = product.images.mapNotNull { image ->
        (image as? JsonPrimitive)?.contentOrNull ?: runCatching {
            val objectValue = image.jsonObject
            (objectValue["imageUrl"] as? JsonPrimitive)?.contentOrNull
                ?: (objectValue["url"] as? JsonPrimitive)?.contentOrNull
        }.getOrNull()
    }.filter(String::isNotBlank).distinct()
    return ProductDetail(
        productId = product.productId.idValue(),
        memberId = product.memberId.idValue(),
        categoryId = product.categoryId.idValue(),
        title = product.title,
        description = product.description,
        condition = product.condition,
        modelName = product.modelName,
        releaseYear = product.releaseYear,
        marketPrice = product.marketPrice,
        thumbnailUrl = product.thumbnailUrl,
        status = product.status,
        imageUrls = imageUrls.ifEmpty { listOfNotNull(product.thumbnailUrl?.takeIf(String::isNotBlank)) },
        sellerNickname = sellerSummary?.nickname,
        sellerRating = sellerSummary?.rating,
        sellerTradeCount = sellerSummary?.tradeCount ?: sellerSummary?.completedTradeCount
    )
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
