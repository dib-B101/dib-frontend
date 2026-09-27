package com.ssafy.dib.feature.main

import android.graphics.Bitmap
import android.net.Uri
import com.ssafy.dib.core.time.formatServerTime
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.core.ui.DibProfileAvatar
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.domain.member.MemberProfile
import com.ssafy.dib.domain.support.InquiryDetail
import com.ssafy.dib.domain.support.InquirySummary
import com.ssafy.dib.domain.report.ReportSummary
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.feature.home.HomeAuctionCard
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileEditScreen(
    profile: MemberProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    saveLoading: Boolean,
    saveError: String?,
    onRetry: () -> Unit,
    onSave: (String, Uri?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nickname by rememberSaveable(profile?.memberId) { mutableStateOf(profile?.nickname.orEmpty()) }
    var selectedImage by rememberSaveable(profile?.memberId) { mutableStateOf<Uri?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageError = productImageValidationMessage(context.contentResolver, listOf(uri))
            if (imageError == null) selectedImage = uri
        }
    }
    val preview by produceState<Bitmap?>(initialValue = null, key1 = selectedImage) {
        value = selectedImage?.let { uri ->
            withContext(Dispatchers.IO) { decodeProductImagePreview(context.contentResolver, uri, targetPx = 256) }
        }
    }
    SimpleHeaderScaffold("프로필 수정", onBack, modifier) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null || profile == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(errorMessage ?: "내 정보를 불러오지 못했어요.", color = Colors.Muted)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).clip(CircleShape).clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    if (preview != null) Image(preview!!.asImageBitmap(), "선택한 프로필 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else DibProfileAvatar(profile.profileImageUrl, 88.dp)
                }
                TextButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("프로필 사진 변경", color = Colors.Navy, fontWeight = FontWeight.Bold)
                }
                imageError?.let { Text(it, color = Colors.Urgent, fontSize = 12.sp) }
                Text("닉네임", Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(nickname, { nickname = it.take(12) }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                Text("이메일", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                ReadOnlyProfileValue(profile.email)
                Text("이름 · 휴대전화", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                ReadOnlyProfileValue("${profile.name} · ${profile.phoneNumber}")
                saveError?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
                Button(
                    onClick = { onSave(nickname.trim(), selectedImage) },
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(48.dp),
                    enabled = nickname.isNotBlank() && (nickname.trim() != profile.nickname || selectedImage != null) && !saveLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) {
                    if (saveLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("변경사항 저장", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyProfileValue(value: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Color(0xFFF0F2F7), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text(value, color = Colors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun RegisteredProductsScreen(
    selectionPurpose: String?,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    remoteProducts: List<RegisteredProduct>?,
    showSampleContent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    deleteError: String?,
    deletingProductId: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onAuctionStart: (String) -> Unit,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteCandidate by remember { mutableStateOf<RegisteredProduct?>(null) }
    val products = remoteProducts ?: if (showSampleContent) {
        listOf(
            RegisteredProduct("sample-pending", "빈티지 필름 카메라", "NORMAL", "PENDING", null),
            RegisteredProduct(
                productId = "sample-registered",
                title = "달빛 유약 머그컵",
                condition = "GOOD",
                status = "REGISTERED",
                thumbnailUrl = null,
                auctionId = "sample-auction",
                startPrice = 30_000,
                auctionTimeSeconds = 300,
                auctionStatus = "SCHEDULED"
            ),
            RegisteredProduct("sample-rejected", "핸드메이드 가죽 지갑", "BAD", "REJECTED", null)
        )
    } else emptyList()
    var filter by rememberSaveable { mutableStateOf("전체") }
    val selectingAuctionProduct = selectionPurpose == "auction"
    MyListScaffold(if (selectingAuctionProduct) "경매 상품 선택" else "등록 상품 관리", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (selectingAuctionProduct) item {
                Column(Modifier.fillMaxWidth().background(Colors.NavySoft, RoundedCornerShape(16.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("시작할 경매를 선택해주세요", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("검수 승인되고 시작 전 상태인 경매만 시작할 수 있어요.", color = Colors.Muted, fontSize = 11.sp)
                }
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("전체", "검수 중", "승인", "등록 거절", "판매 완료").forEach { label -> FilterChip(label, filter == label) { filter = label } } } }
            if (isLoading) item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
            if (errorMessage != null) item { Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry) { Text("다시 불러오기") } } }
            deleteError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            val filtered = if (isLoading || (errorMessage != null && products.isEmpty())) emptyList() else products.filter { filter == "전체" || productStatusLabel(it.status) == filter }
            if (!isLoading && errorMessage == null && filtered.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("등록한 상품이 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("상품을 등록하면 검수 상태를 여기서 확인할 수 있어요", color = Colors.Muted, fontSize = 12.sp) } }
            items(filtered.size) { index ->
                val product = filtered[index]
                // 유찰(ENDED 인데 상품은 REGISTERED 로 돌아온 것)도 여기서 바로 다시 시작한다. 서버가 시작 때 재등록을 겸한다
                val unsold = product.auctionStatus?.uppercase() == "ENDED"
                val canStartAuction = product.status.uppercase() in setOf("REGISTERED", "APPROVED") &&
                    product.auctionStatus?.uppercase() in setOf("SCHEDULED", "ENDED") &&
                    !product.auctionId.isNullOrBlank() &&
                    (remoteProducts != null || showSampleContent)
                val canEdit = remoteProducts != null && isProductEditable(product.status)
                val canDelete = remoteProducts != null && product.status.uppercase() in setOf("PENDING", "PENDING_REVIEW", "REGISTERED", "APPROVED", "REJECTED", "REVIEW_REJECTED")
                Column(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (product.thumbnailUrl.isNullOrBlank()) {
                            Box(Modifier.size(76.dp).background(Color(0xFFF0F2F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.product_outline), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(Colors.Muted))
                            }
                        } else {
                            DibNetworkImage(product.thumbnailUrl, product.title, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val statusLabel = productStatusLabel(product.status)
                            Text(product.title, color = Colors.Text, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Row {
                                Text(
                                    statusLabel,
                                    Modifier.background(when (statusLabel) { "승인" -> Color(0xFFE7F8F2); "등록 거절" -> Color(0xFFFFECEE); else -> Color(0xFFF0F2F7) }, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                    color = when (statusLabel) { "승인" -> Colors.MintInk; "등록 거절" -> Color(0xFFD84352); else -> Colors.Navy },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                when {
                                    canStartAuction && unsold -> "유찰된 경매예요. 다시 시작할 수 있어요."
                                    canStartAuction -> "검수가 승인됐어요. 경매를 시작할 수 있어요."
                                    else -> productStatusDescription(product.status)
                                },
                                color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp
                            )
                        }
                    }
                    if (canStartAuction || canEdit || canDelete) {
                        HorizontalDivider(color = Colors.Border)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (canStartAuction) Button(
                                onClick = { onAuctionStart(product.productId) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                            ) { Text(if (unsold) "경매 다시 시작" else "경매 시작", fontSize = 12.sp) }
                            if (canEdit) OutlinedButton(
                                onClick = { onEditProduct(product.productId) },
                                enabled = deletingProductId == null,
                                modifier = if (canStartAuction) Modifier.height(40.dp) else Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text(if (product.status.equals("REJECTED", true) || product.status.equals("REVIEW_REJECTED", true)) "수정하기" else "수정", fontSize = 12.sp) }
                            if (!canStartAuction && !canEdit) Spacer(Modifier.weight(1f))
                            if (canDelete) TextButton(
                                onClick = { deleteCandidate = product },
                                enabled = deletingProductId == null,
                                modifier = Modifier.height(40.dp)
                            ) { Text(if (deletingProductId == product.productId) "삭제 중" else "삭제", color = Colors.Urgent, fontSize = 12.sp) }
                        }
                    }
                }
            }
            if (!isLoading && errorMessage == null && (hasNext || isLoadingMore || loadMoreError != null)) item(key = "product-load-more") {
                LaunchedEffect(products.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                HistoryLoadMore(isLoadingMore, loadMoreError, onLoadMore)
            }
            item { Button(onRegister, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("새 상품 등록", fontWeight = FontWeight.Bold) } }
            }
        }
    }
    deleteCandidate?.let { product ->
        DibDialog(
            onDismissRequest = { deleteCandidate = null },
            title = "상품을 삭제할까요?",
            text = { Text("${product.title}\n진행 중인 경매나 거래 이력이 있으면 삭제할 수 없어요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = { DibDialogConfirmButton("삭제", { deleteCandidate = null; onDeleteProduct(product.productId) }, destructive = true) },
            dismissButton = { DibDialogDismissButton({ deleteCandidate = null }) }
        )
    }
}

private fun productStatusLabel(status: String) = when (status.uppercase()) {
    "PENDING", "PENDING_REVIEW" -> "검수 중"
    "REGISTERED", "APPROVED" -> "승인"
    "REJECTED", "REVIEW_REJECTED" -> "등록 거절"
    "SOLD" -> "판매 완료"
    "ON_AUCTION" -> "경매 중"
    "CANCELED", "CANCELLED" -> "취소"
    else -> status
}
private fun productStatusDescription(status: String) = when (status.uppercase()) {
    "PENDING", "PENDING_REVIEW" -> "AI 검수 중이에요. 결과가 나오면 알림으로 알려드려요."
    "REGISTERED", "APPROVED" -> "검수 승인 · 경매 시작 가능"
    "REJECTED", "REVIEW_REJECTED" -> "등록이 거절됐어요 · 사유 확인 후 수정해주세요"
    "SOLD" -> "판매가 완료된 상품"
    "ON_AUCTION" -> "경매가 진행 중인 상품"
    "CANCELED", "CANCELLED" -> "경매가 취소된 상품"
    else -> "상품 상태 확인 필요"
}
internal fun isProductEditable(status: String): Boolean = status.uppercase() in setOf("REGISTERED", "APPROVED", "REJECTED", "REVIEW_REJECTED")

@Composable
fun FavoriteAuctionsScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    remoteFavorites: List<HomeAuction>?,
    showSampleContent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    removingAuctionId: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleFavorites = remember {
        listOf(
            HomeAuction("headphones", "무선 헤드폰", 52_000, 7, 1_080, "디지털", com.ssafy.dib.feature.home.ProductPhoto.Headphones, bookmarked = true),
            HomeAuction("camera", "빈티지 필름 카메라", 34_500, 5, 204, "라이프", com.ssafy.dib.feature.home.ProductPhoto.Camera, bookmarked = true)
        )
    }
    val favorites = remoteFavorites ?: if (showSampleContent) sampleFavorites else emptyList()
    MyListScaffold("찜한 경매", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("찜한 경매 ${favorites.size}개", Modifier.padding(vertical = 16.dp), color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            } else if (errorMessage != null) {
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(errorMessage, color = Colors.Muted, fontSize = 12.sp)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
                }
            } else if (favorites.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(R.drawable.favorite_outline), null, Modifier.size(36.dp), colorFilter = ColorFilter.tint(Color(0xFFB8C0CC)))
                        Text("찜한 경매가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("관심 있는 경매의 하트를 눌러 저장해보세요", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites, key = HomeAuction::id) { auction ->
                        // 홈과 같은 상품 카드. 하트를 끄면 찜이 해제된다
                        HomeAuctionCard(
                            auction = auction,
                            favorite = true,
                            onFavorite = { selected -> if (!selected && removingAuctionId == null) onRemove(auction.id) },
                            onClick = { onProductClick(auction.id) },
                            showStatus = true,
                            meta = if (removingAuctionId == auction.id) "찜 해제 중" else null
                        )
                    }
                    if (hasNext || isLoadingMore || loadMoreError != null) item(
                        key = "favorite-load-more",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        LaunchedEffect(favorites.size, hasNext, isLoadingMore, loadMoreError) {
                            if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                        }
                        HistoryLoadMore(isLoadingMore, loadMoreError, onLoadMore)
                    }
                }
            }
            }
        }
    }
}

private data class Inquiry(val status: String, val title: String, val date: String, val questionId: String? = null)

@Composable
fun InquiryHistoryScreen(
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    remoteInquiries: List<InquirySummary>?,
    showSampleContent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    selectedInquiry: InquiryDetail?,
    detailLoading: Boolean,
    detailError: String?,
    submitLoading: Boolean,
    submitError: String?,
    submissionRevision: Int,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onInquiryClick: (String) -> Unit,
    onDetailDismiss: () -> Unit,
    onSubmit: (title: String, content: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inquiries = remoteInquiries?.map {
        Inquiry(
            if (it.answeredAt != null) "답변 완료" else "답변 대기",
            it.title,
            formatServerTime(it.createdAt, "yyyy.MM.dd") ?: it.createdAt.take(10),
            it.questionId
        )
    } ?: if (showSampleContent) {
        listOf(
            Inquiry("답변 완료", "배송 상태가 갱신되지 않아요", "2026.09.08"),
            Inquiry("답변 대기", "자동 결제 실패 문의", "2026.09.09")
        )
    } else emptyList()
    var formOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(submissionRevision) { if (submissionRevision > 0) formOpen = false }
    MyListScaffold("문의 내역", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (isLoading) item { Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Colors.Navy) } }
            else if (errorMessage != null) item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") } } }
            else if (inquiries.isEmpty()) item { Text("등록한 문의가 없어요.", Modifier.fillMaxWidth().padding(vertical = 32.dp), color = Colors.Muted, fontSize = 13.sp) }
            else items(inquiries.size) { index -> val item = inquiries[index]; Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).clickable(enabled = item.questionId != null) { item.questionId?.let(onInquiryClick) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(item.status, color = if (item.status == "답변 완료") Colors.MintInk else Colors.Urgent, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(item.title, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(item.date, color = Colors.Muted, fontSize = 11.sp) } }
            if (!isLoading && errorMessage == null && (hasNext || isLoadingMore || loadMoreError != null)) item(key = "inquiry-load-more") {
                LaunchedEffect(inquiries.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                HistoryLoadMore(isLoadingMore, loadMoreError, onLoadMore)
            }
            item { Button({ formOpen = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("문의하기", fontWeight = FontWeight.Bold) } }
            }
        }
    }
    if (formOpen) {
        var title by rememberSaveable { mutableStateOf("") }; var body by rememberSaveable { mutableStateOf("") }
        DibDialog(
            onDismissRequest = { if (!submitLoading) formOpen = false },
            title = "문의하기",
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { title = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("제목") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = dialogFieldColors())
                    OutlinedTextField(body, { body = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("문의 내용") }, minLines = 4, shape = RoundedCornerShape(12.dp), colors = dialogFieldColors())
                    submitError?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
                }
            },
            confirmButton = { DibDialogConfirmButton("등록", onClick = { onSubmit(title.trim(), body.trim()) }, enabled = title.isNotBlank() && body.isNotBlank(), loading = submitLoading) },
            dismissButton = { DibDialogDismissButton({ formOpen = false }, enabled = !submitLoading) }
        )
    }
    if (detailLoading || detailError != null || selectedInquiry != null) {
        DibDialog(
            onDismissRequest = onDetailDismiss,
            title = selectedInquiry?.title ?: "문의 상세",
            text = {
                when {
                    detailLoading -> CircularProgressIndicator(color = Colors.Navy)
                    detailError != null -> Text(detailError, color = Colors.Urgent, fontSize = 13.sp)
                    selectedInquiry != null -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val answered = selectedInquiry.answer != null
                        Text(if (answered) "답변 완료" else "답변 대기", color = if (answered) Colors.MintInk else Colors.Urgent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(selectedInquiry.content, color = Colors.Text, fontSize = 14.sp, lineHeight = 21.sp)
                        Column(Modifier.fillMaxWidth().background(Colors.NavySoft, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("답변", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(selectedInquiry.answer ?: "아직 답변을 기다리고 있어요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            },
            confirmButton = { DibDialogConfirmButton("닫기", onDetailDismiss) }
        )
    }
}

@Composable
fun ReportHistoryScreen(
    onBack: () -> Unit,
    reports: List<ReportSummary>?,
    showSampleContent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleReports = remember {
        listOf(
            ReportSummary("sample-auction", "AUCTION", "허위 정보가 포함되어 있어요", "ACCEPTED", "빈티지 필름 카메라", "2026-09-08T09:00:00Z"),
            ReportSummary("sample-member", "MEMBER", "부적절한 메시지를 받았어요", "PENDING", "seller01", "2026-09-09T09:00:00Z")
        )
    }
    val displayedReports = reports ?: if (showSampleContent) sampleReports else emptyList()
    SimpleHeaderScaffold("신고 내역", onBack, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                isLoading -> item { Row(Modifier.fillMaxWidth().padding(40.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null -> item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") } } }
                displayedReports.isEmpty() -> item { Text("접수한 신고가 없어요.", Modifier.fillMaxWidth().padding(vertical = 40.dp), color = Colors.Muted, fontSize = 13.sp) }
                else -> items(displayedReports.size, key = { displayedReports[it].reportId }) { index ->
                    val report = displayedReports[index]
                    ReportCard(report)
                }
            }
            if (!isLoading && errorMessage == null && (hasNext || isLoadingMore || loadMoreError != null)) item(key = "report-load-more") {
                LaunchedEffect(reports?.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                HistoryLoadMore(isLoadingMore, loadMoreError, onLoadMore)
            }
            }
        }
    }
}

/** 신고 한 건을 카드 하나에 담는다. 따로 상세로 들어가지 않아도 내용과 처리 현황을 바로 본다 */
@Composable
private fun ReportCard(report: ReportSummary) {
    val pending = report.status.equals("PENDING", true)
    Column(
        Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).border(1.dp, Colors.Border, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(reportTypeLabel(report.type), Modifier.weight(1f), color = Colors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                reportStatusLabel(report.status),
                Modifier.background(if (pending) Colors.UrgentBackground else Colors.MintSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (pending) Colors.Urgent else Colors.MintInk, fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        }
        Text(report.targetLabel, color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(reportContentDetail(report.content), color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        HorizontalDivider(color = Colors.Border)
        Text(if (pending) "접수한 신고를 검토 중이에요" else "신고 처리가 완료됐어요", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        report.createdAt?.let { createdAt -> Text("접수 " + (formatServerTime(createdAt) ?: createdAt), color = Colors.Muted, fontSize = 11.sp) }
        report.processedAt?.let { processedAt -> Text("처리 " + (formatServerTime(processedAt) ?: processedAt), color = Colors.Muted, fontSize = 11.sp) }
    }
}

@Composable
internal fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Colors.Navy,
    unfocusedBorderColor = Colors.Border,
    focusedLabelColor = Colors.Navy,
    unfocusedLabelColor = Colors.Muted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun HistoryLoadMore(isLoading: Boolean, error: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
            error != null -> {
                Text(error, color = Colors.Muted, fontSize = 11.sp)
                TextButton(onClick = onRetry) { Text("더 불러오기") }
            }
        }
    }
}

private fun reportTypeLabel(type: String) = when (type.uppercase()) {
    "AUCTION" -> "경매 신고"
    "MEMBER" -> "회원 신고"
    "ORDER" -> "거래 신고"
    "CHATTING" -> "채팅 신고"
    else -> "신고"
}

private fun reportStatusLabel(status: String) = when (status.uppercase()) {
    "PENDING" -> "접수됨"
    "ACCEPTED" -> "검토 완료"
    "REFUNDED" -> "환불 완료"
    "REJECTED" -> "기각"
    else -> status
}

private fun reportContentDetail(content: String): String = content
    .replace("[신고 사유] ", "신고 사유 · ")
    .replace("[신고할 메시지] ", "신고 메시지 · ")
    .replace("[거래 채팅 메시지] ", "거래 메시지 · ")
    .replace("[상세 내용] ", "상세 내용 · ")

// Text 에 높이와 padding 을 같이 주면 글자가 위로 붙었다. Box 가운데 정렬로 세로 중앙에 둔다
@Composable private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(32.dp).background(if (selected) Colors.Navy else Color.White, RoundedCornerShape(16.dp)).border(1.dp, if (selected) Colors.Navy else Color(0xFFDBE0E8), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Colors.Muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun MyListScaffold(title: String, onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }, content = content) }
@Composable private fun SimpleHeaderScaffold(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, content = content) }
@Composable private fun Header(title: String, onBack: () -> Unit) { DibSubAppBar(title, onBack) }
