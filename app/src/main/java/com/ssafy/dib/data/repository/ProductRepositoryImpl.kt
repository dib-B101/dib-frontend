package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.product.CategoryDto
import com.ssafy.dib.data.remote.product.ProductRemoteDataSource
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductRegistrationResult
import com.ssafy.dib.domain.product.ProductRepository
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ProductRepositoryImpl(private val remote: ProductRemoteDataSource) : ProductRepository {
    override fun getCategories(): ApiResult<List<ProductCategory>> =
        when (val result = remote.getCategories()) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(CategoryDto::toDomain), result.status)
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

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
