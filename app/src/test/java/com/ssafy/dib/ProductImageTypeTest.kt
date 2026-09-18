package com.ssafy.dib

import com.ssafy.dib.feature.main.moveProductImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductImageTypeTest {
    @Test
    fun movingPhotoReordersTheList() {
        val images = mutableListOf("a", "b", "c")

        assertTrue(moveProductImage(images, 2, 1))

        assertEquals(listOf("a", "c", "b"), images)
    }

    @Test
    fun movingPhotoToFirstMakesItRepresentative() {
        val images = mutableListOf("a", "b", "c")

        assertTrue(moveProductImage(images, 1, 0))

        assertEquals(listOf("b", "a", "c"), images)
        assertEquals("b", images.first())
    }

    @Test
    fun movingWithInvalidIndexDoesNothing() {
        val images = mutableListOf("a", "b")

        assertFalse(moveProductImage(images, 0, 0))
        assertFalse(moveProductImage(images, -1, 1))
        assertFalse(moveProductImage(images, 0, 5))
        assertEquals(listOf("a", "b"), images)
    }
}
