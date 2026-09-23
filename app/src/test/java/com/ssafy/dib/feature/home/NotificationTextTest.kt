package com.ssafy.dib.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {
    @Test
    fun orderNumberBecomesProductTitleWhenKnown() {
        assertEquals(
            "‘아이리버 버티컬 마우스’ 결제가 완료되었습니다. 배송지를 입력해 주세요.",
            humanizeNotificationText("주문 #12 결제가 완료되었습니다. 배송지를 입력해 주세요.", mapOf("12" to "아이리버 버티컬 마우스"))
        )
    }

    @Test
    fun unknownOrderNumberIsDroppedNotShown() {
        assertEquals("주문 상품이 발송되었습니다.", humanizeNotificationText("주문 #7 상품이 발송되었습니다.", emptyMap()))
    }

    @Test
    fun trailingOrderReferenceIsRemovedAndOtherNumbersStripped() {
        assertEquals(
            "‘귤’ 거래가 끝났습니다. 별점으로 평가해 주세요.",
            humanizeNotificationText("‘귤’ 거래가 끝났습니다. 별점으로 평가해 주세요. (주문 #3)", emptyMap())
        )
        assertEquals("문의에 관리자 답변이 등록되었습니다.", humanizeNotificationText("문의 #5에 관리자 답변이 등록되었습니다.", emptyMap()))
        assertEquals(listOf("12", "9"), orderIdsInNotificationText("주문 #12 와 주문 #9"))
        assertEquals("다른 참가자가 87,000원으로 입찰했습니다.", humanizeNotificationText("다른 참가자가 87000원으로 입찰했습니다.", emptyMap()))
    }
}
