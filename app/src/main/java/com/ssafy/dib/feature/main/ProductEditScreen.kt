package com.ssafy.dib.feature.main

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
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.feature.auction.productModerationStatusLabel

@Composable
fun ProductEditScreen(
    detail: ProductDetail?,
    categories: List<ProductCategory>,
    auctionStatus: String? = null,
    auctionStartPrice: Long? = null,
    auctionTimeSeconds: Long? = null,
    productStatus: String? = null,
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
                // 제목·설명·카테고리를 바꾸면 서버가 PENDING 으로 되돌린다. 가격만 바꾸면 상태는 그대로다
                Text(
                    if (result.status.uppercase() in setOf("PENDING", "PENDING_REVIEW"))
                        "내용이 바뀌었으니 다시 검수 대기 상태로 돌아갔어요.\n승인되면 경매를 시작할 수 있어요."
                    else "검수 상태는 그대로 유지돼요.",
                    Modifier.padding(top = 10.dp),
                    color = Colors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                Button(onComplete, Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("확인", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { DibSubAppBar("상품 수정", onBack) }
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
                productStatus = productStatus ?: detail.status,
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
    productStatus: String?,
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
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var showConditions by rememberSaveable { mutableStateOf(false) }
    val moderationStatus = (productStatus ?: detail.status).uppercase()
    val awaitingModeration = moderationStatus in setOf("PENDING", "PENDING_REVIEW")
    val rejected = moderationStatus in setOf("REJECTED", "REVIEW_REJECTED")
    // 검수 통과 전에는 경매 행이 없어서 시작가·경매시간을 보내면 404 AUCTION_NOT_FOUND 가 난다
    val auctionEditable = auctionStatus == "SCHEDULED" && !awaitingModeration && !rejected
    var startPrice by rememberSaveable(detail.productId) { mutableStateOf(auctionStartPrice?.toString().orEmpty()) }
    var auctionMinutes by rememberSaveable(detail.productId) { mutableStateOf(auctionTimeSeconds?.let { (it / 60).toString() }.orEmpty()) }
    val auctionInputValid = !auctionEditable ||
        ((startPrice.toLongOrNull() ?: 0L) > 0L && (auctionMinutes.toIntOrNull() ?: 0) >= 5)
    val valid = title.isNotBlank() && description.isNotBlank() && categoryId.isNotBlank() &&
        condition in setOf("GOOD", "NORMAL", "BAD") && auctionInputValid
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (rejected || awaitingModeration) item {
            Column(
                Modifier.fillMaxWidth()
                    .background(if (rejected) Colors.UrgentBackground else Colors.NavySoft, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    productModerationStatusLabel(moderationStatus, detail.moderationStage, detail.moderatedAt),
                    color = if (rejected) Colors.Urgent else Colors.Navy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    detail.moderationReason?.takeIf(String::isNotBlank)
                        ?: if (rejected) "거절 사유가 전달되지 않았어요. 내용을 수정한 뒤 다시 검수를 받아주세요."
                        else "검수가 끝나면 경매를 시작할 수 있어요.",
                    color = Colors.Text,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val images = detail.imageUrls.ifEmpty { listOfNotNull(detail.thumbnailUrl) }
                Text("상품 사진 ${images.size}장 · 최대 10장", color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (images.isEmpty()) {
                    Text("등록된 사진이 없어요.", color = Colors.Muted, fontSize = 12.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(images) { index, imageUrl ->
                            Box(Modifier.size(88.dp)) {
                                DibNetworkImage(imageUrl, detail.title, Modifier.fillMaxSize())
                                if (index == 0) Surface(
                                    Modifier.align(Alignment.TopStart).padding(5.dp),
                                    color = Colors.Navy,
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("대표", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp) }
                            }
                        }
                    }
                    Text("첫 번째 사진이 대표 이미지(썸네일)로 사용돼요.", color = Colors.Muted, fontSize = 11.sp)
                }
                Text("현재 백엔드 수정 API는 이미지 변경을 지원하지 않아 기존 사진을 그대로 유지해요.", color = Colors.Muted, fontSize = 11.sp)
            }
        }
        item { EditField("상품명", title, { title = it }, KeyboardType.Text) }
        item { EditField("상품 설명", description, { description = it }, KeyboardType.Text, singleLine = false) }
        item { Text("카테고리", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold); OutlinedButton({ showCategories = true }, Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(categories.firstOrNull { it.categoryId == categoryId }?.name ?: "카테고리 선택") } }
        item {
            Text("상품 상태", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedButton({ showConditions = true }, Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(if (condition.isBlank()) "상품 상태 선택" else conditionLabel(condition))
            }
        }
        item { EditField("모델명 (선택)", modelName, { modelName = it }, KeyboardType.Text) }
        item { EditField("출시연도 (선택)", releaseYear, { releaseYear = it.filter(Char::isDigit).take(4) }, KeyboardType.Number) }
        if (auctionEditable) {
            item { EditField("경매 시작가 (원)", startPrice, { startPrice = it.filter(Char::isDigit).take(10) }, KeyboardType.Number) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EditField("경매 시간 (분)", auctionMinutes, { auctionMinutes = it.filter(Char::isDigit).take(4) }, KeyboardType.Number)
                    Text("최소 5분부터 설정할 수 있어요.", color = Colors.Muted, fontSize = 11.sp)
                }
            }
        } else if (awaitingModeration || rejected || auctionStatus != null) {
            item {
                Text(
                    when {
                        awaitingModeration -> "검수 통과 후에 가격과 경매 시간을 정할 수 있어요."
                        rejected -> "등록이 거절된 상품이에요. 내용을 수정하면 다시 검수를 받고, 승인 후에 가격과 경매 시간을 정할 수 있어요."
                        else -> "경매가 시작된 뒤에는 시작가와 경매 시간을 바꿀 수 없어요."
                    },
                    Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(14.dp),
                    color = Colors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        item {
            val contentChanged = title.trim() != detail.title ||
                description.trim() != detail.description ||
                categoryId != detail.categoryId
            Text(
                if (contentChanged) "상품명·설명·카테고리를 바꾸면 다시 검수 대기(PENDING)로 돌아가요. 승인 후에 경매를 시작할 수 있어요."
                else "가격 정보만 바꾸면 검수 상태는 그대로 유지돼요.",
                Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(12.dp)).padding(14.dp),
                color = if (contentChanged) Colors.Urgent else Colors.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        submitError?.let { item { Text(it, color = Colors.Urgent, fontSize = 12.sp) } }
        item { Button({ onSubmit(ProductUpdate(title.trim(), description.trim(), categoryId, condition, modelName.trim().ifBlank { null }, releaseYear.toIntOrNull(), null, if (auctionEditable) startPrice.toLongOrNull() else null, if (auctionEditable) auctionMinutes.toIntOrNull()?.let { it * 60 } else null)) }, Modifier.fillMaxWidth().height(52.dp), enabled = valid && !submitLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (submitLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("수정 내용 등록", fontWeight = FontWeight.Bold) } }
    }
    if (showCategories) DibDialog(
        onDismissRequest = { showCategories = false },
        title = "카테고리 선택",
        text = { LazyColumn { items(categories) { category -> Text(category.name, Modifier.fillMaxWidth().clickable { categoryId = category.categoryId; showCategories = false }.padding(vertical = 12.dp), color = Colors.Text, fontWeight = if (categoryId == category.categoryId) FontWeight.Bold else FontWeight.Normal) } } },
        confirmButton = { DibDialogConfirmButton("닫기", { showCategories = false }) }
    )
    if (showConditions) ProductConditionDialog(
        selected = condition,
        onSelect = { condition = it },
        onDismiss = { showConditions = false }
    )
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 4, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Colors.Border,focusedContainerColor=Colors.Background,unfocusedContainerColor=Colors.Background))
    }
}
