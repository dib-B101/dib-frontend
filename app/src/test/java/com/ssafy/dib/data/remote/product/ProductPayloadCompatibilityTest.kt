package com.ssafy.dib.data.remote.product

import com.ssafy.dib.core.network.DibJson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductPayloadCompatibilityTest {
    @Test
    fun `backend array category payload is accepted`() {
        val payload = DibJson.instance.parseToJsonElement("""[{"categoryId":1,"name":"디지털"}]""")

        val result = decodeCategoryList(payload)

        assertEquals(1, result.items.size)
        assertEquals("디지털", result.items.single().name)
    }

    @Test
    fun `backend direct product detail payload is accepted`() {
        val payload = DibJson.instance.parseToJsonElement(
            """{"productId":3,"memberId":7,"categoryId":1,"title":"카메라","status":"PENDING","nickname":"판매자"}"""
        )

        val result = decodeProductDetail(payload)

        assertEquals("카메라", result.product.title)
        assertEquals("판매자", result.product.nickname)
    }

    @Test
    fun `backend array product payload keeps productStatus`() {
        val payload = DibJson.instance.parseToJsonElement(
            """[{"productId":3,"title":"카메라","condition":"USED","productStatus":"REGISTERED"}]"""
        )

        val result = decodeProductList(payload)

        assertEquals("REGISTERED", result.items.single().productStatus)
    }

    @Test
    fun `page payload from target contract is still accepted`() {
        val payload = DibJson.instance.parseToJsonElement(
            """{"items":[{"productId":3,"title":"카메라","status":"PENDING"}],"nextCursor":"next","hasNext":true}"""
        )

        val result = decodeProductList(payload)

        assertEquals("next", result.nextCursor)
        assertEquals(true, result.hasNext)
    }
}
