package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.report.ReportListResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportContractTest {
    @Test
    fun mapsAuctionAndMemberReportTargets() {
        val response = DibJson.instance.decodeFromString(
            ReportListResponse.serializer(),
            """{"items":[{"reportId":1,"content":"허위 정보","type":"AUCTION","status":"PENDING","auctionId":22},{"reportId":"r-2","content":"부적절한 언행","type":"MEMBER","status":"ACCEPTED","targetMemberId":"member-7"}]}"""
        )

        val auctionReport = response.items[0].toDomain()
        val memberReport = response.items[1].toDomain()

        assertEquals("1", auctionReport.reportId)
        assertEquals("경매 22", auctionReport.targetLabel)
        assertEquals("PENDING", auctionReport.status)
        assertEquals("회원 member-7", memberReport.targetLabel)
        assertEquals("ACCEPTED", memberReport.status)
    }
}
