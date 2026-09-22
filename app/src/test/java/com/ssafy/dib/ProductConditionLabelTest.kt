package com.ssafy.dib

import com.ssafy.dib.feature.main.PRODUCT_CONDITIONS
import com.ssafy.dib.feature.main.conditionLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductConditionLabelTest {
    // 등록 화면 RegisterSelect 가 표시 문자열을 contains("상 ·") 로 판정해서
    // 선택값 "상 · 사용감 적음" 이 안내문과 같이 걸려 회색으로 남았다.
    // 라벨이 안내문과 접두사를 공유해도 되도록, 색 판정은 상태값으로만 한다
    @Test
    fun selectedLabelsSharePrefixWithPlaceholder() {
        assertEquals("상 · 중 · 하", conditionLabel(""))
        assertTrue(conditionLabel("GOOD").startsWith("상 ·"))
    }

    @Test
    fun everyConditionHasItsOwnLabel() {
        val labels = PRODUCT_CONDITIONS.map(::conditionLabel)

        assertEquals(listOf("상 · 사용감 적음", "중 · 일반 사용감", "하 · 하자 있음"), labels)
        assertTrue(labels.none { it == conditionLabel("") })
    }
}
