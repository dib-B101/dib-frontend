package com.ssafy.dib.data.remote.product

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductUpdate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProductRemoteDataSource(private val client: DibHttpClient) {
    fun getCategories(): ApiResult<CategoryListResponse> = configured {
        client.execute(client.requestBuilder(ApiRoutes.CATEGORIES).get().build(), JsonElement.serializer())
            .decodePayload(::decodeCategoryList)
    }

    fun getProduct(productId: String): ApiResult<ProductDetailResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        when (val detail = client.execute(client.requestBuilder(path).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductDetail)) {
            is ApiResult.Failure -> detail
            is ApiResult.Success -> {
                val imagePath = "$path/images"
                when (val images = client.execute(client.requestBuilder(imagePath).get().build(), ListSerializer(ProductImageDto.serializer()))) {
                    is ApiResult.Failure -> detail
                    is ApiResult.Success -> {
                        val imageElements = images.value.map { image -> JsonPrimitive(image.imageUrl) }
                        val value = detail.value.copy(product = detail.value.product.copy(images = imageElements))
                        ApiResult.Success(value, detail.status)
                    }
                }
            }
        }
    }

    fun getSimilarProducts(productId: String, size: Int): ApiResult<ProductListResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.PRODUCTS)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
            .build()
        when (val result = client.execute(client.requestBuilder(ApiRoutes.PRODUCTS).url(url).get().build(), ProductListResponse.serializer())) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                result.value.copy(items = result.value.items.filterNot { it.productId.toString().trim('"') == productId }),
                result.status
            )
        }
    }

    fun getSellerProducts(memberId: String): ApiResult<List<ProductCardDto>> = configured {
        val path = "${ApiRoutes.PRODUCTS}/members/$memberId"
        client.execute(client.requestBuilder(path).get().build(), ListSerializer(ProductCardDto.serializer()))
    }

    fun getMyProducts(status: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/members/me"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        when (val result = client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductList)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val filtered = status?.takeIf(String::isNotBlank)?.let { wanted ->
                    result.value.items.filter { (it.productStatus ?: it.status) == wanted }
                } ?: result.value.items
                ApiResult.Success(result.value.copy(items = filtered), result.status)
            }
        }
    }

    fun searchProducts(query: String, categoryId: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/search"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("keyword", query)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        when (val result = client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductList)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val filtered = categoryId?.takeIf(String::isNotBlank)?.let { wanted ->
                    result.value.items.filter { it.categoryId?.toString()?.trim('"') == wanted }
                } ?: result.value.items
                ApiResult.Success(result.value.copy(items = filtered), result.status)
            }
        }
    }

    fun registerProduct(registration: ProductRegistration, idempotencyKey: String): ApiResult<ProductCreateResponse> = configured {
        val categoryId = registration.categoryId.toLongOrNull()?.let(::JsonPrimitive)
            ?: JsonPrimitive(registration.categoryId)
        val payload = ProductCreatePayload(
            title = registration.title,
            description = registration.description,
            categoryId = categoryId,
            condition = registration.condition,
            modelName = registration.modelName,
            releaseYear = registration.releaseYear,
            marketPrice = registration.marketPrice,
            startPrice = registration.startPrice,
            auctionTime = registration.auctionTime
        )
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "request",
                null,
                DibJson.instance.encodeToString(ProductCreatePayload.serializer(), payload)
                    .toRequestBody("application/json".toMediaType())
            )
        registration.images.forEach { image ->
            multipart.addFormDataPart(
                "images",
                image.fileName,
                image.bytes.toRequestBody(image.mediaType.toMediaTypeOrNull())
            )
        }
        client.execute(
            client.requestBuilder(ApiRoutes.PRODUCTS)
                .header("Idempotency-Key", idempotencyKey)
                .post(multipart.build())
                .build(),
            ProductCreateResponse.serializer()
        )
    }

    fun deleteProduct(productId: String, idempotencyKey: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        client.executeUnit(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKey)
                .delete()
                .build()
        )
    }

    fun updateProduct(productId: String, update: ProductUpdate): ApiResult<ProductUpdateResponse> = configured {
        val categoryId = update.categoryId.toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(update.categoryId)
        val payload = ProductUpdatePayload(update.title, update.description, categoryId, update.condition, update.modelName, update.releaseYear, update.marketPrice, update.startPrice, update.auctionTime)
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        client.execute(
            client.requestBuilder(path).patch(client.jsonBody(payload, ProductUpdatePayload.serializer())).build(),
            ProductUpdateResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
        }
}

internal fun decodeCategoryList(payload: JsonElement): CategoryListResponse = when (payload) {
    is JsonArray -> CategoryListResponse(DibJson.instance.decodeFromJsonElement(ListSerializer(CategoryDto.serializer()), payload))
    else -> DibJson.instance.decodeFromJsonElement(CategoryListResponse.serializer(), payload)
}

internal fun decodeProductDetail(payload: JsonElement): ProductDetailResponse =
    if (payload is JsonObject && "product" in payload) {
        DibJson.instance.decodeFromJsonElement(ProductDetailResponse.serializer(), payload)
    } else {
        ProductDetailResponse(DibJson.instance.decodeFromJsonElement(ProductDetailDto.serializer(), payload))
    }

internal fun decodeProductList(payload: JsonElement): ProductListResponse = when (payload) {
    is JsonArray -> ProductListResponse(DibJson.instance.decodeFromJsonElement(ListSerializer(ProductCardDto.serializer()), payload))
    else -> DibJson.instance.decodeFromJsonElement(ProductListResponse.serializer(), payload)
}

private inline fun <T, R> ApiResult<T>.decodePayload(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value), status)
    is ApiResult.Failure -> this
}
