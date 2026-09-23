package com.ssafy.dib.feature.live

import com.ssafy.dib.domain.live.LiveChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNoticeTest {
    private val seller = "7"

    private fun chat(id: String, memberId: String, content: String, time: String) =
        LiveChatMessage(id, memberId, null, content, time)

    @Test
    fun ordinarySellerChatIsNotNotice() {
        // 판매자가 그냥 인사한 채팅은 더 이상 공지가 되지 않는다
        val messages = listOf(chat("1", seller, "안녕하세요", "2026-09-23T10:00:00Z"))

        assertNull(currentLiveNotice(messages, seller))
    }

    @Test
    fun latestNoticeCommandWinsAndPrefixIsRemoved() {
        val messages = listOf(
            chat("1", seller, liveNoticeCommand("첫 공지"), "2026-09-23T10:00:00Z"),
            chat("2", seller, "일반 채팅", "2026-09-23T10:01:00Z"),
            chat("3", seller, liveNoticeCommand("배송은 내일 출발해요"), "2026-09-23T10:02:00Z")
        )

        assertEquals("배송은 내일 출발해요", currentLiveNotice(messages, seller)?.content)
    }

    @Test
    fun clearCommandRemovesNotice() {
        val messages = listOf(
            chat("1", seller, liveNoticeCommand("공지"), "2026-09-23T10:00:00Z"),
            chat("2", seller, LIVE_NOTICE_CLEAR, "2026-09-23T10:01:00Z")
        )

        assertNull(currentLiveNotice(messages, seller))
    }

    @Test
    fun viewerCannotPostNotice() {
        // 시청자가 머리말을 흉내 내도 공지가 아니고 채팅 목록에서도 숨기지 않는다
        val fake = chat("1", "99", liveNoticeCommand("가짜 공지"), "2026-09-23T10:00:00Z")

        assertNull(currentLiveNotice(listOf(fake), seller))
        assertFalse(isLiveNoticeControl(fake, seller))
        assertTrue(isLiveNoticeControl(chat("2", seller, LIVE_NOTICE_CLEAR, "2026-09-23T10:01:00Z"), seller))
    }
}
