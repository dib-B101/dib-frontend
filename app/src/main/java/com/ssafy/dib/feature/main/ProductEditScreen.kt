package com.ssafy.dib.feature.main

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onSubmit: (ProductUpdate, List<ProductImageSelection>?) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val replacementUris = remember(detail?.productId) { mutableStateListOf<Uri>() }
    val replacementTypes = remember(detail?.productId) { mutableStateListOf<String>() }
    var showPhotoReorder by remember(detail?.productId) { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)) { uris ->
        replacementUris.clear()
        replacementUris.addAll(uris.take(10))
        replacementTypes.clear()
        replacementTypes.addAll(defaultProductImageTypes(replacementUris.size))
    }
    if (result != null) {
        Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(72.dp).background(Colors.MintSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = Colors.MintInk, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                Text("상품 수정을 완료했어요", Modifier.padding(top = 20.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("변경된 상품은 다시 AI 검수를 진행해요.", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("확인", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    if (showPhotoReorder) {
        ProductPhotoReorderScreen(
            images = replacementUris.toList(),
            imageTypes = replacementTypes.toList(),
            onSave = { reorderedImages, reorderedTypes ->
                replacementUris.clear()
                replacementUris.addAll(reorderedImages)
                replacementTypes.clear()
                replacementTypes.addAll(reorderedTypes)
                showPhotoReorder = false
            },
            onBack = { showPhotoReorder = false },
            modifier = modifier
        )
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
                submitLoading = submitLoading,
                submitError = submitError,
                replacementUris = replacementUris,
                replacementTypes = replacementTypes,
                onPickImages = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onReorderImages = { showPhotoReorder = true },
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
    submitLoading: Boolean,
    submitError: String?,
    replacementUris: List<Uri>,
    replacementTypes: MutableList<String>,
    onPickImages: () -> Unit,
    onReorderImages: () -> Unit,
    onSubmit: (ProductUpdate, List<ProductImageSelection>?) -> Unit,
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
    val valid = title.isNotBlank() && description.isNotBlank() && categoryId.isNotBlank() && condition in setOf("GOOD", "NORMAL", "BAD")
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("상품 이미지", color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (replacementUris.isEmpty()) {
                    DibNetworkImage(detail.thumbnailUrl, detail.title, Modifier.size(88.dp))
                    Text("새 사진을 선택하지 않으면 기존 이미지를 유지해요.", color = Colors.Muted, fontSize = 11.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(replacementUris) { index, uri ->
                            EditImageThumbnail(
                                uri = uri,
                                representative = index == 0,
                                imageType = replacementTypes[index],
                                onCycleType = if (index == 0) null else ({
                                    replacementTypes[index] = nextProductImageType(replacementTypes[index])
                                })
                            )
                        }
                    }
                    Text("새 이미지 ${replacementUris.size}장 · 사진별 촬영 방향을 확인해주세요", color = Colors.MintInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (replacementUris.size > 1) {
                        OutlinedButton(onClick = onReorderImages, modifier = Modifier.fillMaxWidth()) {
                            Text("사진 순서 편집", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedButton(onPickImages, Modifier.fillMaxWidth()) { Text(if (replacementUris.isEmpty()) "새 이미지 선택" else "이미지 다시 선택") }
            }
        }
        item { EditField("상품명", title, { title = it }, KeyboardType.Text) }
        item { EditField("상품 설명", description, { description = it }, KeyboardType.Text, singleLine = false) }
        item { Text("카테고리", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); OutlinedButton({ showCategories = true }, Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(categories.firstOrNull { it.categoryId == categoryId }?.name ?: "카테고리 선택") } }
        item { Text("상품 상태", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("GOOD" to "좋음", "NORMAL" to "보통", "BAD" to "사용감 있음").forEach { (value, label) -> FilterChip(condition == value, { condition = value }, { Text(label) }) } } }
        item { EditField("모델명 (선택)", modelName, { modelName = it }, KeyboardType.Text) }
        item { EditField("출시연도 (선택)", releaseYear, { releaseYear = it.filter(Char::isDigit).take(4) }, KeyboardType.Number) }
        item { EditField("시세 (선택)", marketPrice, { marketPrice = it.filter(Char::isDigit).take(10) }, KeyboardType.Number) }
        submitError?.let { item { Text(it, color = Colors.Urgent, fontSize = 12.sp) } }
        item { Button({ onSubmit(ProductUpdate(title.trim(), description.trim(), categoryId, condition, modelName.trim().ifBlank { null }, releaseYear.toIntOrNull(), marketPrice.toLongOrNull()), replacementUris.indices.map { ProductImageSelection(replacementUris[it], replacementTypes[it]) }.takeIf { it.isNotEmpty() }) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid && replacementTypes.size == replacementUris.size && !submitLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (submitLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("수정하고 다시 검수 요청", fontWeight = FontWeight.Bold) } }
    }
    if (showCategories) AlertDialog(onDismissRequest = { showCategories = false }, title = { Text("카테고리 선택") }, text = { LazyColumn { items(categories) { category -> Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; showCategories = false }.padding(vertical = 12.dp)) } } }, confirmButton = { TextButton({ showCategories = false }) { Text("닫기") } })
}

@Composable
private fun EditImageThumbnail(uri: Uri, representative: Boolean, imageType: String, onCycleType: (() -> Unit)?) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap() }
    }
    Column(Modifier.width(94.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(88.dp).background(Colors.Image, RoundedCornerShape(10.dp))) {
            bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (representative) Surface(Modifier.align(Alignment.TopStart).padding(5.dp), color = Colors.Navy, shape = RoundedCornerShape(8.dp)) { Text("대표", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp) }
        }
        Surface(
            modifier = Modifier.clickable(enabled = onCycleType != null) { onCycleType?.invoke() },
            color = if (representative) Colors.Navy else Color(0xFFF1F5FA),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(
                if (representative) "정면 고정" else "${productImageTypeLabel(imageType)} ›",
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (representative) Color.White else Colors.Navy,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 4, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Colors.Border,focusedContainerColor=Colors.Background,unfocusedContainerColor=Colors.Background))
    }
}
