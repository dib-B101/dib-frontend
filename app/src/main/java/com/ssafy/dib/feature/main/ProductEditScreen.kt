package com.ssafy.dib.feature.main

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductDetail
import com.ssafy.dib.domain.product.ProductUpdate
import com.ssafy.dib.domain.product.ProductUpdateResult
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.core.ui.DibNetworkImage

@Composable
fun ProductEditScreen(
    detail: ProductDetail?,
    categories: List<ProductCategory>,
    auctionStatus: String? = null,
    auctionStartPrice: Long? = null,
    auctionTimeSeconds: Long? = null,
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
        Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(72.dp).background(Colors.MintSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.check_circle), null, Modifier.size(40.dp), colorFilter = ColorFilter.tint(Colors.MintInk)) }
                Text("상품 수정을 완료했어요", Modifier.padding(top = 20.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("수정한 상품은 다시 검수 대기 상태로 전환돼요.", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("확인", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Column(Modifier.background(Colors.Background)) { Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Image(painterResource(R.drawable.back), "뒤로", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Text)) }; Text("상품 수정", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }; HorizontalDivider(color = Colors.Border) } }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null || detail == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(errorMessage ?: "상품 정보를 불러오지 못했어요.", color = Colors.Muted); OutlinedButton(onRetry, Modifier.padding(top = 12.dp)) { Text("다시 불러오기") } }
            else -> ProductEditForm(
                detail = detail,
                categories = categories,
                auctionStatus = auctionStatus,
                auctionStartPrice = auctionStartPrice,
                auctionTimeSeconds = auctionTimeSeconds,
                submitLoading = submitLoading,
                submitError = submitError,
                onSubmit = onSubmit,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ProductEditForm(
    detail: ProductDetail,
    categories: List<ProductCategory>,
    auctionStatus: String?,
    auctionStartPrice: Long?,
    auctionTimeSeconds: Long?,
    submitLoading: Boolean,
    submitError: String?,
    onSubmit: (ProductUpdate) -> Unit,
    modifier: Modifier
) {
    var title by rememberSaveable(detail.productId) { mutableStateOf(detail.title) }
    var description by rememberSaveable(detail.productId) { mutableStateOf(detail.description) }
    var categoryId by rememberSaveable(detail.productId) { mutableStateOf(detail.categoryId) }
    var condition by rememberSaveable(detail.productId) { mutableStateOf(detail.condition) }
    var modelName by rememberSaveable(detail.productId) { mutableStateOf(detail.modelName.orEmpty()) }
    var releaseYear by rememberSaveable(detail.productId) { mutableStateOf(detail.releaseYear?.toString().orEmpty()) }
    var marketPrice by rememberSaveable(detail.productId) { mutableStateOf(detail.marketPrice?.toString().orEmpty()) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    // 서버는 경매가 SCHEDULED 일 때만 시작가/경매시간 갱신을 받는다. 상태를 모르면(null) 아예 노출하지 않는다
    val auctionEditable = auctionStatus == "SCHEDULED"
    var startPrice by rememberSaveable(detail.productId) { mutableStateOf(auctionStartPrice?.toString().orEmpty()) }
    var auctionMinutes by rememberSaveable(detail.productId) { mutableStateOf(auctionTimeSeconds?.let { (it / 60).toString() }.orEmpty()) }
    val auctionInputValid = !auctionEditable ||
        ((startPrice.toLongOrNull() ?: 0L) > 0L && (auctionMinutes.toIntOrNull() ?: 0) >= 5)
    val valid = title.isNotBlank() && description.isNotBlank() && categoryId.isNotBlank() &&
        condition in setOf("GOOD", "NORMAL", "BAD") && auctionInputValid
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("상품 이미지", color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                DibNetworkImage(detail.thumbnailUrl, detail.title, Modifier.size(88.dp))
                Text("현재 백엔드 수정 API는 이미지 변경을 지원하지 않아 기존 이미지를 유지해요.", color = Colors.Muted, fontSize = 11.sp)
            }
        }
        item { EditField("상품명", title, { title = it }, KeyboardType.Text) }
        item { EditField("상품 설명", description, { description = it }, KeyboardType.Text, singleLine = false) }
        item { Text("카테고리", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); OutlinedButton({ showCategories = true }, Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(categories.firstOrNull { it.categoryId == categoryId }?.name ?: "카테고리 선택") } }
        item { Text("상품 상태", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("GOOD" to "좋음", "NORMAL" to "보통", "BAD" to "사용감 있음").forEach { (value, label) -> FilterChip(condition == value, { condition = value }, { Text(label) }) } } }
        item { EditField("모델명 (선택)", modelName, { modelName = it }, KeyboardType.Text) }
        item { EditField("출시연도 (선택)", releaseYear, { releaseYear = it.filter(Char::isDigit).take(4) }, KeyboardType.Number) }
        item { EditField("시세 (선택)", marketPrice, { marketPrice = it.filter(Char::isDigit).take(10) }, KeyboardType.Number) }
        if (auctionEditable) {
            item { EditField("경매 시작가 (원)", startPrice, { startPrice = it.filter(Char::isDigit).take(10) }, KeyboardType.Number) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EditField("경매 시간 (분)", auctionMinutes, { auctionMinutes = it.filter(Char::isDigit).take(4) }, KeyboardType.Number)
                    Text("최소 5분부터 설정할 수 있어요.", color = Colors.Muted, fontSize = 11.sp)
                }
            }
        } else if (auctionStatus != null) {
            item {
                Text(
                    "경매가 시작된 뒤에는 시작가와 경매 시간을 바꿀 수 없어요.",
                    Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(14.dp),
                    color = Colors.Muted,
                    fontSize = 12.sp
                )
            }
        }
        submitError?.let { item { Text(it, color = Colors.Urgent, fontSize = 12.sp) } }
        item { Button({ onSubmit(ProductUpdate(title.trim(), description.trim(), categoryId, condition, modelName.trim().ifBlank { null }, releaseYear.toIntOrNull(), marketPrice.toLongOrNull(), if (auctionEditable) startPrice.toLongOrNull() else null, if (auctionEditable) auctionMinutes.toIntOrNull()?.let { it * 60 } else null)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid && !submitLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (submitLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("수정 내용 등록", fontWeight = FontWeight.Bold) } }
    }
    if (showCategories) AlertDialog(onDismissRequest = { showCategories = false }, title = { Text("카테고리 선택") }, text = { LazyColumn { items(categories) { category -> Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; showCategories = false }.padding(vertical = 12.dp)) } } }, confirmButton = { TextButton({ showCategories = false }) { Text("닫기") } })
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 4, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Colors.Border,focusedContainerColor=Colors.Background,unfocusedContainerColor=Colors.Background))
    }
}
