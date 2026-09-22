package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.settlement.SaveSettlementAccountRequest
import com.ssafy.dib.data.remote.settlement.SettlementAccountRemoteDataSource
import com.ssafy.dib.data.remote.settlement.SettlementAccountResponse
import com.ssafy.dib.domain.settlement.SettlementAccount
import com.ssafy.dib.domain.settlement.SettlementAccountRepository

class SettlementAccountRepositoryImpl(private val remote: SettlementAccountRemoteDataSource) : SettlementAccountRepository {
    override fun getAccount(): ApiResult<SettlementAccount> = when (val result = remote.getAccount()) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }

    override fun saveAccount(bankName: String, accountNumber: String, accountHolder: String): ApiResult<SettlementAccount> =
        when (val result = remote.saveAccount(SaveSettlementAccountRequest(bankName, accountNumber, accountHolder))) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }
}

private fun SettlementAccountResponse.toDomain() = SettlementAccount(bankName, maskedAccountNumber, accountHolder)
