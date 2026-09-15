package com.ssafy.dib

import com.ssafy.dib.core.navigation.auctionCommandError
import com.ssafy.dib.core.navigation.liveControlError
import com.ssafy.dib.core.navigation.signupErrorMessage
import com.ssafy.dib.core.network.ApiFailure
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingErrorMappingTest {
    @Test
    fun currentAuctionOwnershipAndApprovalCodesHaveActionableMessages() {
        assertTrue(auctionCommandError(failure("PRODUCT_NOT_OWNED")).contains("본인이 등록한"))
        assertTrue(auctionCommandError(failure("PRODUCT_NOT_APPROVED")).contains("검수"))
        assertTrue(auctionCommandError(failure("PRODUCT_ALREADY_LISTED")).contains("이미"))
        assertTrue(auctionCommandError(failure("INVALID_AUCTION")).contains("시작가"))
    }

    @Test
    fun currentLiveStateCodesExplainRecoveryAction() {
        assertTrue(liveControlError(failure("LIVE_ITEMS_LIMIT_EXCEEDED")).contains("최대 10개"))
        assertTrue(liveControlError(failure("LIVE_AUCTION_ALREADY_ACTIVE")).contains("현재 경매"))
        assertTrue(liveControlError(failure("LIVE_AUCTION_ALREADY_PROCESSED")).contains("다른 예정 경매"))
    }

    @Test
    fun authRecoveryCodesDoNotExposeRawCode() {
        assertTrue(signupErrorMessage(failure("NICKNAME_DUPLICATED")).contains("닉네임"))
        assertTrue(signupErrorMessage(failure("INVALID_RESET_TOKEN")).contains("링크"))
        assertTrue(signupErrorMessage(failure("INVALID_VERIFICATION_ID")).contains("다시 요청"))
    }

    private fun failure(code: String) = ApiFailure(status = 400, code = code, message = "")
}
