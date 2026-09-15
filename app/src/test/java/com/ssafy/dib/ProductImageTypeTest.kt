package com.ssafy.dib

import com.ssafy.dib.feature.main.defaultProductImageTypes
import com.ssafy.dib.feature.main.nextProductImageType
import com.ssafy.dib.feature.main.secondaryProductImageTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductImageTypeTest {
    @Test
    fun firstImageIsTheOnlyDefaultFrontImage() {
        val types = defaultProductImageTypes(10)

        assertEquals("FRONT", types.first())
        assertEquals(1, types.count { it == "FRONT" })
        assertTrue(types.drop(1).all { it in secondaryProductImageTypes })
    }

    @Test
    fun cyclingSecondaryTypesNeverSelectsFront() {
        var type = "LEFT"
        repeat(secondaryProductImageTypes.size * 2) {
            type = nextProductImageType(type)
            assertFalse(type == "FRONT")
        }
    }
}
