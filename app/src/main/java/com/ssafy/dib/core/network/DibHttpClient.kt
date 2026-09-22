package com.ssafy.dib.core.network

import java.io.IOException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DibHttpClient(
    private val config: NetworkConfig,
    accessTokenProvider: AccessTokenProvider,
    guestSessionProvider: GuestSessionProvider,
    tokenRefresher: AccessTokenRefresher? = null,
    private val json: Json = DibJson.instance,
    private val errorParser: ApiErrorParser = ApiErrorParser(json),
    baseClient: OkHttpClient = OkHttpClient()
) {
    private val client = baseClient.newBuilder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder().header("Accept", JSON_MEDIA_TYPE)
            accessTokenProvider.accessToken()?.takeIf(String::isNotBlank)?.let {
                builder.header("Authorization", "Bearer $it")
            } ?: guestSessionProvider.guestSessionId()?.takeIf(String::isNotBlank)?.let {
                // 백엔드가 아직 읽지 않는 헤더지만, 비회원 조회 추적에 쓸 여지가 있어 남겨둔다
                builder.header("X-Guest-Session-Id", it)
            }
            chain.proceed(builder.build())
        }
        .authenticator { _, response ->
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.takeIf(String::isNotBlank)
            if (
                tokenRefresher == null ||
                failedToken == null ||
                response.request.url.encodedPath == TOKEN_REFRESH_PATH ||
                response.retryCount() >= MAX_AUTH_ATTEMPTS
            ) {
                null
            } else {
                tokenRefresher.refresh(failedToken)?.let { refreshedToken ->
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $refreshedToken")
                        .build()
                }
            }
        }
        .build()

    fun requestBuilder(path: String): Request.Builder {
        return Request.Builder().url(urlBuilder(path).build())
    }

    fun urlBuilder(path: String): HttpUrl.Builder =
        "${config.requireApiBaseUrl()}/${path.trimStart('/')}".toHttpUrl().newBuilder()

    fun <T> execute(request: Request, responseSerializer: DeserializationStrategy<T>): ApiResult<T> =
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (response.isSuccessful) {
                    runCatching {
                        json.decodeFromJsonElement(responseSerializer, json.responsePayload(body))
                    }
                        .fold(
                            onSuccess = { ApiResult.Success(it, response.code) },
                            onFailure = {
                                ApiResult.Failure(
                                    ApiFailure(
                                        status = response.code,
                                        code = ApiErrorCodes.INVALID_RESPONSE,
                                        message = "서버 응답을 해석하지 못했습니다.",
                                        path = request.url.encodedPath,
                                        cause = it
                                    )
                                )
                            }
                        )
                } else {
                    ApiResult.Failure(errorParser.parse(response.code, body, request.url.encodedPath))
                }
            }
        } catch (error: IllegalStateException) {
            ApiResult.Failure(
                ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error)
            )
        } catch (error: IOException) {
            ApiResult.Failure(
                ApiFailure(null, ApiErrorCodes.NETWORK_UNAVAILABLE, "네트워크 연결을 확인해주세요.", cause = error)
            )
        }

    fun executeUnit(request: Request): ApiResult<Unit> =
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ApiResult.Success(Unit, response.code)
                } else {
                    ApiResult.Failure(
                        errorParser.parse(response.code, response.body.string(), request.url.encodedPath)
                    )
                }
            }
        } catch (error: IllegalStateException) {
            ApiResult.Failure(
                ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error)
            )
        } catch (error: IOException) {
            ApiResult.Failure(
                ApiFailure(null, ApiErrorCodes.NETWORK_UNAVAILABLE, "네트워크 연결을 확인해주세요.", cause = error)
            )
        }

    fun <T> jsonBody(value: T, serializer: SerializationStrategy<T>) =
        json.encodeToString(serializer, value).toRequestBody(JSON_MEDIA_TYPE.toMediaType())

    companion object {
        private const val JSON_MEDIA_TYPE = "application/json"
        private const val TOKEN_REFRESH_PATH = "/api/v1/auth/token/refresh"
        private const val MAX_AUTH_ATTEMPTS = 2
    }
}

internal fun Json.responsePayload(body: String): JsonElement {
    val root = parseToJsonElement(body)
    val objectRoot = root as? JsonObject
    return objectRoot?.get("data") ?: objectRoot?.get("response") ?: root
}

fun interface AccessTokenRefresher {
    fun refresh(failedAccessToken: String): String?
}

private fun okhttp3.Response.retryCount(): Int {
    var count = 1
    var previous = priorResponse
    while (previous != null) {
        count++
        previous = previous.priorResponse
    }
    return count
}
