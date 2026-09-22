package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.product.RegisteredProduct
import com.ssafy.dib.feature.main.AuctionConditionDialog
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun AuctionRegisterScreen(
    productId: String,
    product: RegisteredProduct?,
    started: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onStart: (startPrice: Long, auctionTimeSeconds: Long) -> Unit,
    onOpenAuction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar("경매 시작", onBack) }
    ) { padding ->
        when {
            started -> StartedContent(
                auctionId = product?.auctionId.orEmpty(),
                onOpenAuction = onOpenAuction,
                modifier = Modifier.fillMaxSize().padding(padding)
            )

            product == null && isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Colors.Navy)
            }

            product == null -> LoadFailureContent(
                productId = productId,
                message = errorMessage ?: "등록된 경매 정보를 찾지 못했어요.",
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize().padding(padding)
            )

            else -> StartConfirmationContent(
                product = product,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onRetry = onRetry,
                onStart = onStart,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun StartConfirmationContent(
    product: RegisteredProduct,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onStart: (startPrice: Long, auctionTimeSeconds: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConditionDialog by rememberSaveable(product.auctionId) { mutableStateOf(false) }
    val productStatus = product.status.uppercase()
    // 검수 통과 전에는 경매 행 자체가 없다. 검수 상태와 예정 경매를 모두 확인해야 한다
    val approved = productStatus in setOf("REGISTERED", "APPROVED")
    val awaitingModeration = productStatus in setOf("PENDING", "PENDING_REVIEW")
    val rejected = productStatus in setOf("REJECTED", "REVIEW_REJECTED")
    val ready = approved && !product.auctionId.isNullOrBlank() && product.auctionStatus?.uppercase() == "SCHEDULED"
    val conditionsFixed = isScheduledAuctionReady(
        auctionId = product.auctionId,
        auctionStatus = product.auctionStatus,
        startPrice = product.startPrice,
        auctionTimeSeconds = product.auctionTimeSeconds
    )
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("시작가와 시간을 정하고\n경매를 시작할까요?", color = Colors.Text, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            productModerationStatusLabel(product.status, product.moderationStage, product.moderatedAt),
            color = if (approved) Colors.MintInk else Colors.Urgent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(product.title, color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Colors.Border)
            AuctionConditionRow("시작가", product.startPrice?.let { "%,d원".format(it) } ?: "가격 미정")
            AuctionConditionRow("경매 시간", product.auctionTimeSeconds?.let(::formatAuctionDuration) ?: "미정")
            AuctionConditionRow("경매 상태", if (product.auctionStatus?.uppercase() == "SCHEDULED") "시작 전" else "확인 필요")
        }
        Text(
            if (conditionsFixed) "저장된 조건이 기본값으로 채워져요. 시작하면 즉시 카운트다운이 진행되며 조건은 바꿀 수 없어요."
            else "시작가와 경매 시간은 지금 정해요. 시작하면 즉시 카운트다운이 진행되며 조건은 바꿀 수 없어요.",
            Modifier.fillMaxWidth().background(Colors.NavySoft, RoundedCornerShape(15.dp)).padding(16.dp),
            color = Colors.Muted,
            fontSize = 12.sp,
            lineHeight = 19.sp
        )
        if (!ready) {
            Text(
                when {
                    awaitingModeration -> "아직 검수 중이에요. 승인되면 경매를 시작할 수 있어요. 결과 알림이 없으니 새로고침으로 확인해주세요."
                    rejected -> "등록이 거절된 상품이에요. 내용을 수정해 다시 검수를 받아주세요."
                    else -> "예정 상태의 경매를 찾지 못했어요. 상품 목록을 새로고침해주세요."
                },
                color = Colors.Urgent,
                fontSize = 12.sp
            )
            OutlinedButton(onRetry, Modifier.fillMaxWidth().height(48.dp), enabled = !isLoading, shape = RoundedCornerShape(12.dp)) {
                Text("다시 불러오기", color = Colors.Navy, fontWeight = FontWeight.Bold)
            }
        }
        errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { showConditionDialog = true },
            enabled = ready && !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("지금 경매 시작", fontWeight = FontWeight.Bold)
        }
    }
    if (showConditionDialog) {
        AuctionConditionDialog(
            dialogKey = "auction-register-${product.auctionId.orEmpty()}",
            title = "경매를 시작할까요?",
            confirmLabel = "경매 시작",
            description = "시작가와 경매 시간을 정하면 바로 경매가 시작돼요.",
            startPrice = product.startPrice,
            auctionTimeSeconds = product.auctionTimeSeconds,
            loading = isLoading,
            onDismiss = { showConditionDialog = false },
            onConfirm = { price, seconds -> showConditionDialog = false; onStart(price, seconds) }
        )
    }
}

@Composable
private fun AuctionConditionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Colors.Muted, fontSize = 13.sp)
        Text(value, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StartedContent(auctionId: String, onOpenAuction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(Modifier.size(78.dp), color = Colors.MintSoft, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Text("시작", color = Colors.MintInk, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Text("경매를 시작했어요", Modifier.padding(top = 20.dp), color = Colors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("경매 번호 $auctionId", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 13.sp)
        Button(onOpenAuction, Modifier.fillMaxWidth().padding(top = 28.dp).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(15.dp)) {
            Text("경매 상세 보기", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadFailureContent(productId: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("경매 준비 정보를 불러오지 못했어요", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("상품 번호 $productId", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 12.sp)
        Text(message, Modifier.padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp)
        OutlinedButton(onRetry, Modifier.fillMaxWidth().padding(top = 24.dp).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("다시 불러오기") }
    }
}

internal fun isScheduledAuctionReady(
    auctionId: String?,
    auctionStatus: String?,
    startPrice: Long?,
    auctionTimeSeconds: Long?
): Boolean = !auctionId.isNullOrBlank() &&
    auctionStatus?.uppercase() == "SCHEDULED" &&
    (startPrice ?: 0L) >= 1_000L &&
    (auctionTimeSeconds ?: 0L) >= 300L

internal fun formatAuctionDuration(seconds: Long): String = when {
    seconds % 3_600L == 0L -> "${seconds / 3_600L}시간"
    seconds >= 3_600L -> "${seconds / 3_600L}시간 ${(seconds % 3_600L) / 60L}분"
    else -> "${seconds / 60L}분"
}
