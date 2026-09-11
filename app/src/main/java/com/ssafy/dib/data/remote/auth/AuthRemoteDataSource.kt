package com.ssafy.dib.data.remote.auth

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class AuthRemoteDataSource(private val client: DibHttpClient) {
    fun requestPhoneVerification(request: PhoneVerificationRequest): ApiResult<PhoneVerificationResponse> = configured {
        val httpRequest = client.requestBuilder(ApiRoutes.PHONE_VERIFICATIONS)
            .post(client.jsonBody(request, PhoneVerificationRequest.serializer()))
            .build()
        client.execute(httpRequest, PhoneVerificationResponse.serializer())
    }

    fun confirmPhoneVerification(
        verificationId: String,
        request: PhoneVerificationConfirmRequest
    ): ApiResult<PhoneVerificationConfirmResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.PHONE_VERIFICATIONS)
            .addPathSegment(verificationId)
            .addPathSegment("confirm")
            .build()
        val httpRequest = client.requestBuilder(ApiRoutes.PHONE_VERIFICATIONS)
            .url(url)
            .post(client.jsonBody(request, PhoneVerificationConfirmRequest.serializer()))
            .build()
        client.execute(httpRequest, PhoneVerificationConfirmResponse.serializer())
    }

    fun checkEmailAvailability(email: String): ApiResult<EmailAvailabilityResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.EMAIL_AVAILABILITY)
            .addQueryParameter("email", email)
            .build()
        client.execute(
            client.requestBuilder(ApiRoutes.EMAIL_AVAILABILITY).url(url).get().build(),
            EmailAvailabilityResponse.serializer()
        )
    }

    fun signUp(request: SignUpRequest): ApiResult<SignUpResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.SIGN_UP)
                .post(client.jsonBody(request, SignUpRequest.serializer()))
                .build(),
            SignUpResponse.serializer()
        )
    }

    fun login(request: LoginRequest): ApiResult<LoginResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.LOGIN)
                .post(client.jsonBody(request, LoginRequest.serializer()))
                .build(),
            LoginResponse.serializer()
        )
    }

    fun refresh(request: RefreshTokenRequest): ApiResult<RefreshTokenResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.TOKEN_REFRESH)
                .post(client.jsonBody(request, RefreshTokenRequest.serializer()))
                .build(),
            RefreshTokenResponse.serializer()
        )
    }

    fun logout(request: LogoutRequest): ApiResult<Unit> = configured {
        client.executeUnit(
            client.requestBuilder(ApiRoutes.LOGOUT)
                .post(client.jsonBody(request, LogoutRequest.serializer()))
                .build()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: IllegalStateException) {
            configurationFailure(error)
        } catch (error: IllegalArgumentException) {
            configurationFailure(error)
        }

    private fun configurationFailure(error: RuntimeException) = ApiResult.Failure(
        ApiFailure(
            status = null,
            code = ApiErrorCodes.CLIENT_NOT_CONFIGURED,
            message = error.message ?: "네트워크 주소 설정을 확인해주세요.",
            cause = error
        )
    )
}
