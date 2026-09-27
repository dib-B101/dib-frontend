package com.ssafy.dib.feature.main

import com.ssafy.dib.domain.member.MemberAddress

internal data class SavedShippingFields(
    val postalCode: String,
    val address: String,
    val detailAddress: String,
    val receiverName: String,
    val receiverPhone: String
)

internal fun MemberAddress.toShippingFields(defaultReceiverName: String = "", defaultReceiverPhone: String = ""): SavedShippingFields {
    val parts = if (detailAddress != null) address to detailAddress else splitStoredAddress(address)
    return SavedShippingFields(
        postalCode = postalCode,
        address = parts.first.ifBlank { address },
        detailAddress = if (parts.first.isBlank()) "" else parts.second,
        receiverName = receiverName?.takeIf(String::isNotBlank) ?: defaultReceiverName,
        receiverPhone = receiverPhone?.takeIf(String::isNotBlank) ?: defaultReceiverPhone
    )
}

// 기존 배송지는 도로명주소와 상세주소가 한 문자열이다. 구분할 수 없으면 원문을 보존한다.
internal fun splitStoredAddress(stored: String): Pair<String, String> {
    val match = Regex("^(.+?(?:로|길)\\s*\\d+(?:-\\d+)?)(?:\\s+(.+))?$").matchEntire(stored.trim())
    return if (match == null) "" to stored else match.groupValues[1] to match.groupValues[2]
}
