package com.ssafy.dib.data.remote.member

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable data class AddressListResponse(val items: List<AddressDto> = emptyList())

@Serializable
data class AddressDto(
    val addressId: JsonElement,
    val number: String? = null,
    val address: String? = null,
    val name: String,
    val apiAddressId: JsonElement
)

@Serializable
data class UpdateAddressRequest(
    val number: String? = null,
    val address: String? = null,
    val name: String? = null,
    val apiAddressId: JsonElement? = null
)
