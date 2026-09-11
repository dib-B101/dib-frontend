package com.ssafy.dib.data.remote.product

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CategoryListResponse(val items: List<CategoryDto> = emptyList())

@Serializable
data class CategoryDto(val categoryId: JsonElement, val name: String)

@Serializable
data class ProductCreatePayload(
    val title: String,
    val description: String,
    val categoryId: JsonElement,
    val condition: String
)

@Serializable
data class ProductCreateResponse(
    val productId: JsonElement,
    val status: String,
    val thumbnailUrl: String? = null,
    val createdAt: String
)
