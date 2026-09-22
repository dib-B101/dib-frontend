package com.ssafy.dib

import com.ssafy.dib.feature.main.detectProductImageMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductImagePolicyTest {
    @Test
    fun detectsSupportedImageSignatures() {
        assertEquals(
            "image/jpeg",
            detectProductImageMediaType(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()))
        )
        assertEquals(
            "image/png",
            detectProductImageMediaType(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
        )
        assertEquals(
            "image/webp",
            detectProductImageMediaType("RIFF1234WEBP".toByteArray())
        )
    }

    @Test
    fun rejectsUnsupportedImageSignature() {
        assertNull(detectProductImageMediaType("GIF89a".toByteArray()))
    }
}
