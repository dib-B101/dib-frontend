package com.ssafy.dib.feature.main

import com.ssafy.dib.core.time.formatServerTime
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.domain.member.MemberProfile
import com.ssafy.dib.domain.support.InquiryDetail
import com.ssafy.dib.domain.support.InquirySummary
import com.ssafy.dib.domain.report.ReportSummary
import com.ssafy.dib.feature.home.HomeAuction
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun ProfileEditScreen(
    profile: MemberProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    saveLoading: Boolean,
    saveError: String?,
    onRetry: () -> Unit,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nickname by rememberSaveable(profile?.memberId) { mutableStateOf(profile?.nickname.orEmpty()) }
    SimpleHeaderScaffold("프로필 수정", onBack, modifier) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null || profile == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(errorMessage ?: "내 정보를 불러오지 못했어요.", color = Colors.Muted)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
            }
            else -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).background(Color(0xFFD6F5ED), CircleShape), contentAlignment = Alignment.Center) { Text(profile.nickname.take(1).ifBlank { "d" }, color = Colors.Navy, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
                Text("닉네임", Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(nickname, { nickname = it.take(12) }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                Text("이메일", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                ReadOnlyProfileValue(profile.email)
                Text("이름 · 휴대전화", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                ReadOnlyProfileValue("${profile.name} · ${profile.phoneNumber}")
                saveError?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
                Button(
                    onClick = { onSave(nickname.trim()) },
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(48.dp),
                    enabled = nickname.isNotBlank() && nickname != profile.nickname && !saveLoading,
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
    Box(Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFF0F2F7), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text(value, color = Colors.Muted, fontSize = 14.sp)
    }
}

@Composable
fun RegisteredProductsScreen(
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
    onAuctionRegister: (String) -> Unit,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteCandidate by remember { mutableStateOf<RegisteredProduct?>(null) }
    val products = remoteProducts ?: if (showSampleContent) {
        listOf(
            RegisteredProduct("sample-pending", "빈티지 필름 카메라", "NORMAL", "PENDING", null),
            RegisteredProduct("sample-registered", "달빛 유약 머그컵", "GOOD", "REGISTERED", null),
            RegisteredProduct("sample-rejected", "핸드메이드 가죽 지갑", "BAD", "REJECTED", null)
        )
    } else emptyList()
    var filter by rememberSaveable { mutableStateOf("전체") }
    MyListScaffold("등록 상품 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("전체", "검수 중", "승인", "거부", "판매 완료").forEach { label -> FilterChip(label, filter == label) { filter = label } } } }
            if (isLoading) item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
            if (errorMessage != null) item { Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry) { Text("다시 불러오기") } } }
            deleteError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            val filtered = if (isLoading || errorMessage != null) emptyList() else products.filter { filter == "전체" || productStatusLabel(it.status) == filter }
            if (!isLoading && errorMessage == null && filtered.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("등록한 상품이 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("상품을 등록하면 검수 상태를 여기서 확인할 수 있어요", color = Colors.Muted, fontSize = 12.sp) } }
            items(filtered.size) { index ->
                val product = filtered[index]
                val canRegisterAuction = remoteProducts != null && product.status == "REGISTERED"
                val canEdit = remoteProducts != null && isProductEditable(product.status)
                val canDelete = remoteProducts != null && product.status in setOf("PENDING", "REGISTERED", "REJECTED")
                Row(Modifier.fillMaxWidth().height(84.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).clickable(enabled = canRegisterAuction) { onAuctionRegister(product.productId) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DibNetworkImage(product.thumbnailUrl, product.title, Modifier.size(56.dp))
                    val statusLabel = productStatusLabel(product.status)
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(product.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (canRegisterAuction) "경매 등록 가능 · 눌러서 등록" else productStatusDescription(product.status), color = Colors.Muted, fontSize = 11.sp) }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(statusLabel, color = when (statusLabel) { "승인" -> Color(0xFF61D1B2); "거부" -> Color(0xFFF5636E); else -> Color(0xFFF26B47) }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (canEdit || canDelete) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canEdit) Text("수정", Modifier.clickable(enabled = deletingProductId == null) { onEditProduct(product.productId) }.padding(3.dp), color = Colors.Navy, fontSize = 10.sp)
                            if (canDelete) Text(if (deletingProductId == product.productId) "삭제 중" else "삭제", Modifier.clickable(enabled = deletingProductId == null) { deleteCandidate = product }.padding(3.dp), color = Colors.Muted, fontSize = 10.sp)
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
    deleteCandidate?.let { product ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("상품을 삭제할까요?") },
            text = { Text("${product.title}\n진행 중인 경매나 거래 이력이 있으면 삭제할 수 없어요.") },
            confirmButton = { TextButton({ deleteCandidate = null; onDeleteProduct(product.productId) }) { Text("삭제", color = Colors.Urgent) } },
            dismissButton = { TextButton({ deleteCandidate = null }) { Text("취소") } }
        )
    }
}

private fun productStatusLabel(status: String) = when (status.uppercase()) { "PENDING" -> "검수 중"; "REGISTERED" -> "승인"; "REJECTED" -> "거부"; "SOLD" -> "판매 완료"; else -> status }
private fun productStatusDescription(status: String) = when (status.uppercase()) { "PENDING" -> "AI 검수 중"; "REGISTERED" -> "경매 등록 가능"; "REJECTED" -> "검수 결과 확인 필요"; "SOLD" -> "판매가 완료된 상품"; else -> "상품 상태 확인 필요" }
internal fun isProductEditable(status: String): Boolean = status.uppercase() in setOf("REGISTERED", "REJECTED")

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
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
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
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favorites, key = HomeAuction::id) { auction ->
                        Column(Modifier.clickable { onProductClick(auction.id) }) {
                            Box(Modifier.fillMaxWidth().height(122.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(10.dp))) {
                                DibNetworkImage(auction.imageUrls.firstOrNull(), auction.name, Modifier.fillMaxSize())
                                DibWishlistButton(
                                    selected = true,
                                    onSelectedChange = { selected -> if (!selected && removingAuctionId == null) onRemove(auction.id) },
                                    productName = auction.name,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                            Text(auction.name, Modifier.padding(top = 6.dp), color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${auction.pricePrefix} ${auction.priceLabel}", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(if (removingAuctionId == auction.id) "찜 해제 중" else auction.meta, color = Colors.Muted, fontSize = 9.sp)
                        }
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
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("문의 답변은 등록한 이메일로도 알려드려요", color = Colors.Muted, fontSize = 12.sp) }
            if (isLoading) item { Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Colors.Navy) } }
            else if (errorMessage != null) item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") } } }
            else if (inquiries.isEmpty()) item { Text("등록한 문의가 없어요.", Modifier.fillMaxWidth().padding(vertical = 32.dp), color = Colors.Muted, fontSize = 13.sp) }
            else items(inquiries.size) { index -> val item = inquiries[index]; Column(Modifier.fillMaxWidth().height(104.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).clickable(enabled = item.questionId != null) { item.questionId?.let(onInquiryClick) }.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(item.status, color = if (item.status == "답변 완료") Color(0xFF61D1B2) else Color(0xFFF26B47), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(item.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(item.date, color = Colors.Muted, fontSize = 11.sp) } }
            if (!isLoading && errorMessage == null && (hasNext || isLoadingMore || loadMoreError != null)) item(key = "inquiry-load-more") {
                LaunchedEffect(inquiries.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                HistoryLoadMore(isLoadingMore, loadMoreError, onLoadMore)
            }
            item { Button({ formOpen = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("문의하기", fontWeight = FontWeight.Bold) } }
        }
    }
    if (formOpen) {
        var title by rememberSaveable { mutableStateOf("") }; var body by rememberSaveable { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!submitLoading) formOpen = false }, title = { Text("문의하기") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("제목") }); OutlinedTextField(body, { body = it }, label = { Text("문의 내용") }); submitError?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) } } }, confirmButton = { TextButton({ onSubmit(title.trim(), body.trim()) }, enabled = title.isNotBlank() && body.isNotBlank() && !submitLoading) { if (submitLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("등록") } }, dismissButton = { TextButton({ formOpen = false }, enabled = !submitLoading) { Text("취소") } })
    }
    if (detailLoading || detailError != null || selectedInquiry != null) {
        AlertDialog(
            onDismissRequest = onDetailDismiss,
            title = { Text(selectedInquiry?.title ?: "문의 상세") },
            text = {
                when {
                    detailLoading -> CircularProgressIndicator(color = Colors.Navy)
                    detailError != null -> Text(detailError, color = Colors.Urgent)
                    selectedInquiry != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(selectedInquiry.content, color = Colors.Navy)
                        Text(selectedInquiry.answer ?: "아직 답변을 기다리고 있어요.", Modifier.fillMaxWidth().background(Color(0xFFF1F5FA), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.Muted)
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDetailDismiss) { Text("닫기") } }
        )
    }
}

@Composable
fun ReportHistoryScreen(
    onBack: () -> Unit,
    reports: List<ReportSummary>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReport by remember { mutableStateOf<ReportSummary?>(null) }
    val sampleReports = remember {
        listOf(
            ReportSummary("sample-auction", "AUCTION", "허위 정보가 포함되어 있어요", "ACCEPTED", "빈티지 필름 카메라", "2026-09-08T09:00:00Z"),
            ReportSummary("sample-member", "MEMBER", "부적절한 메시지를 받았어요", "PENDING", "seller01", "2026-09-09T09:00:00Z")
        )
    }
    val displayedReports = reports ?: sampleReports
    BackHandler(enabled = selectedReport != null) { selectedReport = null }
    SimpleHeaderScaffold(
        if (selectedReport == null) "신고 내역" else "신고 상세",
        onBack = { if (selectedReport == null) onBack() else selectedReport = null },
        modifier = modifier
    ) { padding ->
        selectedReport?.let { report ->
            ReportDetailContent(report, Modifier.fillMaxSize().padding(padding))
            return@SimpleHeaderScaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                isLoading -> item { Row(Modifier.fillMaxWidth().padding(40.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null -> item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("다시 불러오기") } } }
                reports != null && reports.isEmpty() -> item { Text("접수한 신고가 없어요.", Modifier.fillMaxWidth().padding(vertical = 40.dp), color = Colors.Muted, fontSize = 13.sp) }
                else -> items(displayedReports.size, key = { displayedReports[it].reportId }) { index ->
                    val report = displayedReports[index]
                    HistoryCard(
                        reportTypeLabel(report.type),
                        report.targetLabel,
                        reportContentPreview(report.content),
                        reportStatusLabel(report.status),
                        onClick = { selectedReport = report }
                    )
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

@Composable
private fun ReportDetailContent(report: ReportSummary, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(reportTypeLabel(report.type), Modifier.weight(1f), color = Colors.Live, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(reportStatusLabel(report.status), color = Colors.Live, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(report.targetLabel, color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(reportContentDetail(report.content), color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("처리 상태", color = Colors.Live, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (report.status.equals("PENDING", true)) "신고 내용을 검토하고 있어요" else "신고 처리가 완료됐어요",
                    color = Colors.Navy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (report.status.equals("PENDING", true)) "처리 결과는 알림에서 확인할 수 있어요." else "현재 상태는 ${reportStatusLabel(report.status)}입니다.",
                    color = Colors.Muted,
                    fontSize = 12.sp
                )
                report.createdAt?.let { createdAt ->
                    Text(formatServerTime(createdAt) ?: createdAt, Modifier.fillMaxWidth(), color = Colors.MintInk, fontSize = 11.sp)
                }
            }
        }
    }
}

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
    else -> status
}

private fun reportContentPreview(content: String): String =
    content.lineSequence().firstOrNull { it.isNotBlank() }?.removePrefix("[신고 사유] ") ?: "신고 내용"

private fun reportContentDetail(content: String): String = content
    .replace("[신고 사유] ", "신고 사유 · ")
    .replace("[신고할 메시지] ", "신고 메시지 · ")
    .replace("[거래 채팅 메시지] ", "거래 메시지 · ")
    .replace("[상세 내용] ", "상세 내용 · ")

@Composable private fun HistoryCard(type: String, target: String, reason: String, status: String, onClick: () -> Unit) { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(Modifier.fillMaxWidth()) { Text(type, Modifier.weight(1f), color = Colors.Muted, fontSize = 11.sp); Text(status, color = Color(0xFF41AA8E), fontSize = 11.sp, fontWeight = FontWeight.Bold) }; Text(target, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(reason, color = Colors.Muted, fontSize = 12.sp) } }

@Composable private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) { Text(label, Modifier.height(32.dp).background(if (selected) Colors.Navy else Color.White, RoundedCornerShape(16.dp)).border(1.dp, if (selected) Colors.Navy else Color(0xFFDBE0E8), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp), color = if (selected) Color.White else Colors.Muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }

@Composable private fun MyListScaffold(title: String, onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB), contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }, content = content) }
@Composable private fun SimpleHeaderScaffold(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB), contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, content = content) }
@Composable private fun Header(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.height(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text(title, color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
