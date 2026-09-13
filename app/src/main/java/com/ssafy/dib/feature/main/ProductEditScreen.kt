package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.product.ProductDetail
import com.ssafy.dib.domain.product.ProductUpdate
import com.ssafy.dib.domain.product.ProductUpdateResult
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun ProductEditScreen(
    detail: ProductDetail?,
    categories: List<ProductCategory>,
    isLoading: Boolean,
    errorMessage: String?,
    submitLoading: Boolean,
    submitError: String?,
    result: ProductUpdateResult?,
    onRetry: () -> Unit,
    onSubmit: (ProductUpdate) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result != null) {
        Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Background) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("✓", color = Colors.MintInk, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("상품 수정을 완료했어요", Modifier.padding(top = 16.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("변경된 상품은 다시 AI 검수를 진행해요.", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("확인", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(52.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(52.dp).clickable(onClick = onBack).wrapContentSize(), fontSize = 24.sp); Text("상품 수정", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null || detail == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(errorMessage ?: "상품 정보를 불러오지 못했어요.", color = Colors.Muted); OutlinedButton(onRetry, Modifier.padding(top = 12.dp)) { Text("다시 불러오기") } }
            else -> ProductEditForm(detail, categories, submitLoading, submitError, onSubmit, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProductEditForm(detail: ProductDetail, categories: List<ProductCategory>, submitLoading: Boolean, submitError: String?, onSubmit: (ProductUpdate) -> Unit, modifier: Modifier) {
    var title by rememberSaveable(detail.productId) { mutableStateOf(detail.title) }
    var description by rememberSaveable(detail.productId) { mutableStateOf(detail.description) }
    var categoryId by rememberSaveable(detail.productId) { mutableStateOf(detail.categoryId) }
    var condition by rememberSaveable(detail.productId) { mutableStateOf(detail.condition) }
    var modelName by rememberSaveable(detail.productId) { mutableStateOf(detail.modelName.orEmpty()) }
    var releaseYear by rememberSaveable(detail.productId) { mutableStateOf(detail.releaseYear?.toString().orEmpty()) }
    var marketPrice by rememberSaveable(detail.productId) { mutableStateOf(detail.marketPrice?.toString().orEmpty()) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val valid = title.isNotBlank() && description.isNotBlank() && categoryId.isNotBlank() && condition in setOf("GOOD", "NORMAL", "BAD")
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("기존 이미지는 유지돼요", Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Muted, fontSize = 12.sp) }
        item { EditField("상품명", title, { title = it }, KeyboardType.Text) }
        item { EditField("상품 설명", description, { description = it }, KeyboardType.Text, singleLine = false) }
        item { Text("카테고리", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); OutlinedButton({ showCategories = true }, Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(categories.firstOrNull { it.categoryId == categoryId }?.name ?: "카테고리 선택") } }
        item { Text("상품 상태", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("GOOD" to "좋음", "NORMAL" to "보통", "BAD" to "사용감 있음").forEach { (value, label) -> FilterChip(condition == value, { condition = value }, { Text(label) }) } } }
        item { EditField("모델명 (선택)", modelName, { modelName = it }, KeyboardType.Text) }
        item { EditField("출시연도 (선택)", releaseYear, { releaseYear = it.filter(Char::isDigit).take(4) }, KeyboardType.Number) }
        item { EditField("시세 (선택)", marketPrice, { marketPrice = it.filter(Char::isDigit).take(10) }, KeyboardType.Number) }
        submitError?.let { item { Text(it, color = Colors.Urgent, fontSize = 12.sp) } }
        item { Button({ onSubmit(ProductUpdate(title.trim(), description.trim(), categoryId, condition, modelName.trim().ifBlank { null }, releaseYear.toIntOrNull(), marketPrice.toLongOrNull())) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid && !submitLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (submitLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("수정하고 다시 검수 요청", fontWeight = FontWeight.Bold) } }
    }
    if (showCategories) AlertDialog(onDismissRequest = { showCategories = false }, title = { Text("카테고리 선택") }, text = { LazyColumn { items(categories) { category -> Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; showCategories = false }.padding(vertical = 12.dp)) } } }, confirmButton = { TextButton({ showCategories = false }) { Text("닫기") } })
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 4, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(12.dp))
    }
}
