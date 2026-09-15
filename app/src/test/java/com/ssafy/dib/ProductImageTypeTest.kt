package com.ssafy.dib

import com.ssafy.dib.feature.main.defaultProductImageTypes
import com.ssafy.dib.feature.main.nextProductImageType
import com.ssafy.dib.feature.main.moveProductImage
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

    @Test
    fun movingPhotoKeepsImageAndDirectionTogether() {
        val images = mutableListOf("front-photo", "left-photo", "back-photo")
        val types = mutableListOf("FRONT", "LEFT", "BACK")

        assertTrue(moveProductImage(images, types, 2, 1))

        assertEquals(listOf("front-photo", "back-photo", "left-photo"), images)
        assertEquals(listOf("FRONT", "BACK", "LEFT"), types)
    }

    @Test
    fun movingPhotoToFirstMakesItTheOnlyRepresentative() {
        val images = mutableListOf("front-photo", "left-photo", "back-photo")
        val types = mutableListOf("FRONT", "LEFT", "BACK")

        assertTrue(moveProductImage(images, types, 1, 0))

        assertEquals(listOf("left-photo", "front-photo", "back-photo"), images)
        assertEquals(listOf("FRONT", "LEFT", "BACK"), types)
        assertEquals(1, types.count { it == "FRONT" })
    }
}
