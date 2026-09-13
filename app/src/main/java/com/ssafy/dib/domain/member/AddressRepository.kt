package com.ssafy.dib.domain.member

import com.ssafy.dib.core.network.ApiResult

data class MemberAddress(
    val addressId: String,
    val postalCode: String,
    val address: String,
    val name: String,
    val apiAddressId: String
)

interface AddressRepository {
    fun getAddresses(): ApiResult<List<MemberAddress>>
    fun updateAddress(address: MemberAddress): ApiResult<MemberAddress>
    fun deleteAddress(addressId: String): ApiResult<Unit>
}
