package com.ssafy.dib.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.feature.home.HomeAuction

/** 카테고리 상세에서 경매를 보고 돌아와도 같은 목록과 페이지를 유지한다. */
internal class CategoryUiState : ViewModel() {
    val categories = mutableStateOf<List<ProductCategory>?>(null)
    val auctions = mutableStateOf<List<HomeAuction>?>(null)
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val selectedCategoryId = mutableStateOf<String?>(null)
    val cursor = mutableStateOf<String?>(null)
    val hasNext = mutableStateOf(false)
    val loadingMore = mutableStateOf(false)
    val loadMoreError = mutableStateOf<String?>(null)
}
