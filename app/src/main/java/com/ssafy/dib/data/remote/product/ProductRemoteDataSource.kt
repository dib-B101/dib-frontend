package com.ssafy.dib.data.remote.product

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.core.network.IdempotencyKeyProvider
import com.ssafy.dib.core.network.UuidIdempotencyKeyProvider
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.domain.product.ProductRegistration
import com.ssafy.dib.domain.product.ProductUpdate
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProductRemoteDataSource(
    private val client: DibHttpClient,
    private val idempotencyKeys: IdempotencyKeyProvider = UuidIdempotencyKeyProvider
) {
    fun getCategories(): ApiResult<CategoryListResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.CATEGORIES).get().build(),
            CategoryListResponse.serializer()
        )
    }

    fun getProduct(productId: String): ApiResult<ProductDetailResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        client.execute(client.requestBuilder(path).get().build(), ProductDetailResponse.serializer())
    }

    fun getMyProducts(status: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/products"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        status?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("status", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), ProductListResponse.serializer())
    }

    fun searchProducts(query: String, categoryId: String?, cursor: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.PRODUCTS}/search"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("q", query)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        categoryId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("categoryId", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), ProductListResponse.serializer())
    }

    fun registerProduct(registration: ProductRegistration): ApiResult<ProductCreateResponse> = configured {
        val categoryId = registration.categoryId.toLongOrNull()?.let(::JsonPrimitive)
            ?: JsonPrimitive(registration.categoryId)
        val payload = ProductCreatePayload(
            title = registration.title,
            description = registration.description,
            categoryId = categoryId,
            condition = registration.condition
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
                .header("Idempotency-Key", idempotencyKeys.newKey())
                .post(multipart.build())
                .build(),
            ProductCreateResponse.serializer()
        )
    }

    fun deleteProduct(productId: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId"
        client.executeUnit(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKeys.newKey())
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
