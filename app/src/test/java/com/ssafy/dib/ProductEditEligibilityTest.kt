package com.ssafy.dib

import com.ssafy.dib.feature.main.isProductEditable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductEditEligibilityTest {
    @Test
    fun onlyRegisteredAndRejectedProductsCanEnterEditFlow() {
        assertTrue(isProductEditable("REGISTERED"))
        assertTrue(isProductEditable("rejected"))
        assertFalse(isProductEditable("PENDING"))
        assertFalse(isProductEditable("SOLD"))
    }
}
