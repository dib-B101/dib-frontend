package com.ssafy.dib

import com.ssafy.dib.feature.main.PRODUCT_CONDITIONS
import com.ssafy.dib.feature.main.conditionLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductConditionLabelTest {
    // 등록 화면의 토글과 확인 화면이 같은 상태 설명을 사용한다.
    @Test
    fun selectedLabelsSharePrefixWithPlaceholder() {
        assertEquals("상 · 중 · 하", conditionLabel(""))
        assertTrue(conditionLabel("GOOD").startsWith("상 ·"))
    }

    @Test
    fun everyConditionHasItsOwnLabel() {
        val labels = PRODUCT_CONDITIONS.map(::conditionLabel)

        assertEquals(listOf("상 · 사용감 적음", "중 · 사용감 있음", "하 · 하자 있음"), labels)
        assertTrue(labels.none { it == conditionLabel("") })
    }
}
