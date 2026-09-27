package com.ssafy.dib

import com.ssafy.dib.domain.product.ProductAttributeSpec
import com.ssafy.dib.feature.main.productAttributeError
import com.ssafy.dib.feature.main.productAttributesValid
import com.ssafy.dib.feature.main.purchaseYearValid
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CategoryProductFieldsTest {
    @Test
    fun optionalPurchaseYearCanBeUnknownButMustBeInRangeWhenSet() {
        assertTrue(purchaseYearValid(""))
        assertTrue(purchaseYearValid("2024"))
        assertFalse(purchaseYearValid("1800"))
        assertFalse(purchaseYearValid((java.time.Year.now().value + 1).toString()))
    }

    @Test
    fun voucherRequiresFutureDateAndUnusedConfirmation() {
        val expiry = ProductAttributeSpec("expiryDate", "유효기간", "DATE", true, "YYYY-MM-DD")
        val unused = ProductAttributeSpec("unused", "미사용 상품입니다", "CONFIRM", true, "")
        assertFalse(productAttributesValid(listOf(expiry, unused), emptyMap()))
        assertTrue(productAttributeError(expiry, LocalDate.now().minusDays(1).toString()) != null)
        assertTrue(productAttributeError(unused, "false") != null)
        assertNull(productAttributeError(expiry, LocalDate.now().plusDays(1).toString()))
        assertTrue(productAttributesValid(listOf(expiry, unused), mapOf(
            "expiryDate" to LocalDate.now().plusDays(1).toString(), "unused" to "true")))
    }
}
