package com.ssafy.dib.domain.settlement

import com.ssafy.dib.core.network.ApiResult

data class SettlementAccount(val bankName: String, val maskedAccountNumber: String, val accountHolder: String)

interface SettlementAccountRepository {
    fun getAccount(): ApiResult<SettlementAccount>
    fun saveAccount(bankName: String, accountNumber: String, accountHolder: String): ApiResult<SettlementAccount>
}
