package com.ssafy.dib.data.remote.member

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class AddressRemoteDataSource(private val client: DibHttpClient) {
    fun getAddresses(): ApiResult<AddressListResponse> = configured {
        client.execute(client.requestBuilder(ApiRoutes.MEMBER_ADDRESSES).get().build(), AddressListResponse.serializer())
    }

    fun createAddress(request: CreateAddressRequest): ApiResult<AddressDto> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.MEMBER_ADDRESSES)
                .post(client.jsonBody(request, CreateAddressRequest.serializer()))
                .build(),
            AddressDto.serializer()
        )
    }

    fun updateAddress(addressId: String, request: UpdateAddressRequest): ApiResult<AddressDto> = configured {
        val path = "${ApiRoutes.MEMBER_ADDRESSES}/$addressId"
        client.execute(client.requestBuilder(path).patch(client.jsonBody(request, UpdateAddressRequest.serializer())).build(), AddressDto.serializer())
    }

    fun deleteAddress(addressId: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.MEMBER_ADDRESSES}/$addressId"
        client.executeUnit(client.requestBuilder(path).delete().build())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
    }
}
