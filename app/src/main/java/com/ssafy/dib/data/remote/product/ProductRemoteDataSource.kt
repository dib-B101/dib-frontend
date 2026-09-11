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

    fun getMyProducts(status: String?, size: Int): ApiResult<ProductListResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/products"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        status?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("status", it) }
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

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
        }
}
