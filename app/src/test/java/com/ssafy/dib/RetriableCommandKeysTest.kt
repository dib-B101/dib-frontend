package com.ssafy.dib

import com.ssafy.dib.core.network.RetriableCommandKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RetriableCommandKeysTest {
    @Test
    fun `같은 명령은 성공 처리 전까지 같은 키를 사용한다`() {
        var sequence = 0
        val keys = RetriableCommandKeys { "key-${++sequence}" }

        assertEquals("key-1", keys.keyFor("create:payload-a"))
        assertEquals("key-1", keys.keyFor("create:payload-a"))
        assertEquals("key-2", keys.keyFor("create:payload-b"))
    }

    @Test
    fun `성공 처리한 명령은 다음 실행에 새 키를 사용한다`() {
        var sequence = 0
        val keys = RetriableCommandKeys { "key-${++sequence}" }
        val first = keys.keyFor("delete:1")

        keys.complete("delete:1")

        assertNotEquals(first, keys.keyFor("delete:1"))
    }
}
