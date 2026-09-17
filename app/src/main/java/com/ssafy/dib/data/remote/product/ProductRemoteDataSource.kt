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
        client.execute(client.requestBuilder(path).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductDetail)
    }

    fun getSimilarProducts(productId: String, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId/similar"
        val url = client.urlBuilder(path)
            .addQueryParameter("size", size.coerceIn(1, 20).toString())
            .build()
        client.execute(client.requestBuilder(path).url(url).get().build(), ProductListResponse.serializer())
    }

    fun getMyProducts(status: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/members/me"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        status?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("status", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductList)
    }

    fun searchProducts(query: String, categoryId: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/search"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("keyword", query)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        categoryId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("categoryId", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), JsonElement.serializer())
            .decodePayload(::decodeProductList)
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
            marketPrice = registration.marketPrice
        )
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "product",
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
            multipart.addFormDataPart("imageTypes", image.type)
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
        val imageItems = update.replacementImages?.mapIndexed { index, image -> ProductUpdateImageItem(newFileIndex = index, type = image.type) }
        val payload = ProductUpdatePayload(update.title, update.description, categoryId, update.condition, update.modelName, update.releaseYear, update.marketPrice, imageItems)
        val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "product",
                null,
                DibJson.instance.encodeToString(ProductUpdatePayload.serializer(), payload).toRequestBody("application/json".toMediaType())
            )
        update.replacementImages?.forEach { image ->
            multipartBuilder.addFormDataPart("newImages", image.fileName, image.bytes.toRequestBody(image.mediaType.toMediaTypeOrNull()))
        }
        val multipart = multipartBuilder.build()
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        client.execute(client.requestBuilder(path).patch(multipart).build(), ProductUpdateResponse.serializer())
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
