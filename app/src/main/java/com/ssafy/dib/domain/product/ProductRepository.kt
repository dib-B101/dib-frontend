package com.ssafy.dib.domain.product

import com.ssafy.dib.core.network.ApiResult

data class ProductCategory(val categoryId: String, val name: String)

/** 서버 카테고리를 받지 못했을 때 사용하는 기본 카테고리 목록. ID와 이름은 CategoryScreen과 동일하다. */
val DefaultProductCategories: List<ProductCategory> = listOf(
    ProductCategory("1", "디지털기기"),
    ProductCategory("2", "생활가전"),
    ProductCategory("3", "가구·인테리어"),
    ProductCategory("4", "스포츠·레저"),
    ProductCategory("5", "패션·잡화"),
    ProductCategory("6", "뷰티"),
    ProductCategory("7", "취미·게임"),
    ProductCategory("8", "예술·창작")
)

data class ProductImageUpload(
    val fileName: String,
    val mediaType: String,
    val bytes: ByteArray
)

data class ProductRegistration(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
    val startPrice: Long? = null,
    val auctionTime: Int? = null,
    val images: List<ProductImageUpload>
)

data class ProductRegistrationResult(
    val productId: String,
    // 검수가 켜져 있으면 등록 직후 상태는 PENDING 이다
    val status: String,
    val thumbnailUrl: String?,
    val createdAt: String
)

data class ProductUpdate(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val modelName: String?,
    val releaseYear: Int?,
    val marketPrice: Long? = null,
    // 검수 통과 전 상품에는 경매 행이 없다. 경매가 SCHEDULED 일 때만 보내고, 그 외에는 null 로 보내 404 AUCTION_NOT_FOUND 를 피한다
    val startPrice: Long? = null,
    val auctionTime: Int? = null
)

data class ProductUpdateResult(
    val productId: String,
    val status: String,
    val thumbnailUrl: String?,
    val updatedAt: String?,
    val moderationReason: String? = null,
    val moderationStage: String? = null,
    val moderatedAt: String? = null
)

data class ProductDetail(
    val productId: String,
    val memberId: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val condition: String,
    val modelName: String?,
    val releaseYear: Int?,
    val marketPrice: Long?,
    val thumbnailUrl: String?,
    val status: String,
    val imageUrls: List<String>,
    val sellerNickname: String?,
    val sellerRating: Double?,
    // 받은 평가 건수. 0 이면 평점을 화면에 그리지 않는다
    val sellerReviewCount: Int? = null,
    val sellerTradeCount: Int?,
    val moderationReason: String? = null,
    val moderationStage: String? = null,
    val moderatedAt: String? = null
)

data class RegisteredProduct(
    val productId: String,
    val title: String,
    val condition: String,
    val status: String,
    val thumbnailUrl: String?,
    // 검수 전·거절 상품에는 경매 행이 없어 아래 값이 모두 null 로 내려온다
    val auctionId: String? = null,
    val startPrice: Long? = null,
    val currentPrice: Long? = null,
    val auctionTimeSeconds: Long? = null,
    val auctionStatus: String? = null,
    val bidCount: Int? = null
)

data class RegisteredProductPage(
    val items: List<RegisteredProduct>,
    val nextCursor: String?,
    val hasNext: Boolean
)

// 검색 필터. 전부 선택이고 기본값이면 조건 없이 전체를 훑는다.
// 정렬은 여기 없다 — 커서 페이징이 product_id 역순을 전제해서 가격순을 섞으면 "더 보기" 가 깨진다
data class ProductSearchFilter(
    val categoryId: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    /** GOOD | NORMAL | BAD */
    val condition: String? = null,
    /** 지금 입찰할 수 있는 것만 */
    val onAuctionOnly: Boolean = false
) {
    val isActive: Boolean
        get() = categoryId != null || minPrice != null || maxPrice != null ||
            condition != null || onAuctionOnly
}

interface ProductRepository {
    fun getCategories(): ApiResult<List<ProductCategory>>
    fun getMyProducts(status: String? = null, cursor: String? = null, size: Int = 30): ApiResult<RegisteredProductPage>
    fun getProduct(productId: String): ApiResult<ProductDetail>
    fun getSellerProducts(memberId: String): ApiResult<List<RegisteredProduct>>
    fun getSimilarProducts(productId: String, size: Int = 20): ApiResult<List<RegisteredProduct>>
    fun searchProducts(
        query: String,
        filter: ProductSearchFilter = ProductSearchFilter(),
        cursor: String? = null,
        size: Int = 100
    ): ApiResult<RegisteredProductPage>
    fun registerProduct(registration: ProductRegistration, idempotencyKey: String): ApiResult<ProductRegistrationResult>
    fun updateProduct(productId: String, update: ProductUpdate): ApiResult<ProductUpdateResult>
    fun deleteProduct(productId: String, idempotencyKey: String): ApiResult<Unit>
}
