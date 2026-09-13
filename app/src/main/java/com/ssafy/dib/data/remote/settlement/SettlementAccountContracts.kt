package com.ssafy.dib.data.remote.settlement

import kotlinx.serialization.Serializable

@Serializable data class SettlementAccountResponse(val bankName: String, val maskedAccountNumber: String, val accountHolder: String)
@Serializable data class SaveSettlementAccountRequest(val phoneVerificationToken: String, val bankName: String, val accountNumber: String, val accountHolder: String)
