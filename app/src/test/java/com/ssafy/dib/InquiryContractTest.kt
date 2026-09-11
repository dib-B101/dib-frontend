package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.support.InquiryDetailDto
import com.ssafy.dib.data.remote.support.InquiryListResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InquiryContractTest {
    @Test
    fun mapsInquiryListAndAnswerState() {
        val response = DibJson.instance.decodeFromString(
            InquiryListResponse.serializer(),
            """{"items":[{"questionId":7,"title":"배송 문의","createdAt":"2026-09-11T01:00:00Z","answeredAt":null}]}"""
        )

        val inquiry = response.items.single().toDomain()

        assertEquals("7", inquiry.questionId)
        assertEquals("배송 문의", inquiry.title)
        assertNull(inquiry.answeredAt)
    }

    @Test
    fun mapsInquiryDetailWithAnswer() {
        val dto = DibJson.instance.decodeFromString(
            InquiryDetailDto.serializer(),
            """{"questionId":"q-1","memberId":2,"title":"결제 문의","content":"재결제가 안 됩니다.","createdAt":"2026-09-11T01:00:00Z","answer":"확인했습니다.","answeredAt":"2026-09-11T02:00:00Z"}"""
        )

        val inquiry = dto.toDomain()

        assertEquals("q-1", inquiry.questionId)
        assertEquals("재결제가 안 됩니다.", inquiry.content)
        assertEquals("확인했습니다.", inquiry.answer)
    }
}
