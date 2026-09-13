package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.member.AddressDto
import com.ssafy.dib.data.remote.member.AddressRemoteDataSource
import com.ssafy.dib.data.remote.member.UpdateAddressRequest
import com.ssafy.dib.domain.member.AddressRepository
import com.ssafy.dib.domain.member.MemberAddress
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class AddressRepositoryImpl(private val remote: AddressRemoteDataSource) : AddressRepository {
    override fun getAddresses(): ApiResult<List<MemberAddress>> = when (val result = remote.getAddresses()) {
        is ApiResult.Success -> ApiResult.Success(result.value.items.map(AddressDto::toDomain), result.status)
        is ApiResult.Failure -> result
    }

    override fun updateAddress(address: MemberAddress): ApiResult<MemberAddress> = when (val result = remote.updateAddress(
        address.addressId,
        UpdateAddressRequest(address.postalCode, address.address, address.name, address.apiAddressId.toJsonId())
    )) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }

    override fun deleteAddress(addressId: String): ApiResult<Unit> = remote.deleteAddress(addressId)
}

internal fun AddressDto.toDomain() = MemberAddress(addressId.idValue(), number.orEmpty(), address.orEmpty(), name, apiAddressId.idValue())

private fun JsonElement.idValue(): String = (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
private fun String.toJsonId(): JsonPrimitive = toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(this)
