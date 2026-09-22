package com.ssafy.dib.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
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
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.R
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.live.LiveBroadcastSummary
import com.ssafy.dib.domain.live.LiveItemPlan
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun LiveManagementScreen(
    broadcasts: List<LiveBroadcastSummary>?,
    assignedAuctions: Map<String, List<AuctionSummary>>,
    availableAuctions: List<AuctionSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    actionLoading: Boolean,
    actionError: String?,
    actionMessage: String?,
    actionRevision: Int,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    availableAuctionsHasNext: Boolean,
    availableAuctionsLoadingMore: Boolean,
    availableAuctionsLoadMoreError: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onLoadMoreAvailableAuctions: () -> Unit,
    onCreate: (title: String, description: String?, scheduledAt: String, streamUrl: String?) -> Unit,
    onUpdate: (liveBroadcastId: String, title: String, description: String?, scheduledAt: String, streamUrl: String?) -> Unit,
    onSetItems: (liveBroadcastId: String, items: List<LiveItemPlan>) -> Unit,
    onPrepareStream: (liveBroadcastId: String) -> Unit,
    onStartLive: (liveBroadcastId: String) -> Unit,
    onStartAuction: (liveBroadcastId: String, auctionId: String) -> Unit,
    onEndLive: (liveBroadcastId: String) -> Unit,
    onBack: () -> Unit,
    onOpenConsole: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var editingBroadcastId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingLiveId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(actionRevision) {
        if (actionRevision > 0) {
            showCreate = false
            editingBroadcastId = null
            editingLiveId = null
        }
    }
    val items = broadcasts.orEmpty()
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Surface,
        topBar = {
            Column(Modifier.background(Colors.Background)){Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick=onBack){Image(painterResource(R.drawable.back),"뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))};Text("Live 방송 관리", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) };HorizontalDivider(color=Colors.Border)}
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }, containerColor = Colors.Navy, contentColor = Color.White) {
                Image(painterResource(R.drawable.add), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                Spacer(Modifier.width(7.dp))
                Text("새 방송", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(errorMessage, color = Colors.Muted)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 92.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("예약한 방송 ${items.size}개", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                if (items.isEmpty()) item { EmptyLiveCard() }
                items(items, key = LiveBroadcastSummary::liveBroadcastId) { live ->
                    val auctions = assignedAuctions[live.liveBroadcastId].orEmpty()
                    val activeAuction = auctions.firstOrNull { it.status == "ACTIVE" }
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(live.status)
                            Spacer(Modifier.weight(1f))
                            if (live.status == "SCHEDULED") Text("수정", Modifier.clickable { editingBroadcastId = live.liveBroadcastId }.padding(horizontal = 10.dp, vertical = 6.dp), color = Colors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(formatLiveScheduledAt(live.scheduledAt), color = Colors.Muted, fontSize = 11.sp)
                        }
                        Text(live.title, color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        live.description?.let { Text(it, color = Colors.Muted, fontSize = 12.sp, maxLines = 2) }
                        Text("편성 상품 ${auctions.size}개", color = Colors.MintInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        when (live.status) {
                            "SCHEDULED" -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { editingLiveId = live.liveBroadcastId }, Modifier.weight(1f), enabled = !actionLoading) { Text("상품 편성") }
                                    OutlinedButton(onClick = { onPrepareStream(live.liveBroadcastId) }, Modifier.weight(1f), enabled = !actionLoading) { Text(if (live.streamUrl.isNullOrBlank()) "송출 준비" else "송출 다시 준비") }
                                }
                                Button(
                                    onClick = { onStartLive(live.liveBroadcastId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = auctions.isNotEmpty() && !live.streamUrl.isNullOrBlank() && !actionLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) { Text("Live 시작", fontWeight = FontWeight.Bold) }
                                Text(
                                    when {
                                        auctions.isEmpty() -> "방송할 상품을 한 개 이상 편성해주세요."
                                        live.streamUrl.isNullOrBlank() -> "송출 준비에서 방송 연결 정보를 먼저 받아주세요."
                                        else -> "상품과 송출 연결이 준비됐어요. 방송을 시작할 수 있어요."
                                    },
                                    color = if (auctions.isNotEmpty() && !live.streamUrl.isNullOrBlank()) Colors.MintInk else Colors.Muted,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            "LIVE" -> {
                                Button(
                                    onClick = { onOpenConsole(live.liveBroadcastId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) { Text("방송 관리하기", fontWeight = FontWeight.Bold) }
                                if (activeAuction != null) Text("현재 경매 중 · ${activeAuction.title}", color = Colors.Live, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                else auctions.filter { it.status == "SCHEDULED" }.forEach { auction ->
                                    OutlinedButton(onClick = { onStartAuction(live.liveBroadcastId, auction.auctionId) }, Modifier.fillMaxWidth(), enabled = !actionLoading) { Text("${auction.title} 경매 시작") }
                                }
                                Button(
                                    onClick = { onEndLive(live.liveBroadcastId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = activeAuction == null && !actionLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Live)
                                ) { Text("Live 종료", fontWeight = FontWeight.Bold) }
                                if (activeAuction != null) Text("진행 중인 경매가 끝난 뒤 방송을 종료할 수 있어요.", color = Colors.Muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
                if (hasNext || isLoadingMore || loadMoreError != null) item(key = "live-management-load-more") {
                    LaunchedEffect(items.size, hasNext, isLoadingMore, loadMoreError) {
                        if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            isLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            loadMoreError != null -> {
                                Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                TextButton(onClick = onLoadMore) { Text("다시 불러오기") }
                            }
                        }
                    }
                }
                actionMessage?.let { item { Text(it, Modifier.fillMaxWidth().background(Color(0xFFDDF8F0), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.MintInk, fontSize = 12.sp) } }
                actionError?.let { item { Text(it, Modifier.fillMaxWidth().background(Color(0xFFFFE9E9), RoundedCornerShape(10.dp)).padding(12.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            }
        }
    }

    if (showCreate) LiveFormDialog(null, actionLoading, actionError, { showCreate = false }) { title, description, scheduledAt, streamUrl ->
        onCreate(title, description, scheduledAt, streamUrl)
    }
    editingBroadcastId?.let { liveId -> broadcasts?.firstOrNull { it.liveBroadcastId == liveId }?.let { live ->
        LiveFormDialog(live, actionLoading, actionError, { editingBroadcastId = null }) { title, description, scheduledAt, streamUrl ->
            onUpdate(liveId, title, description, scheduledAt, streamUrl)
        }
    } }
    editingLiveId?.let { liveId ->
        LiveItemDialog(
            current = assignedAuctions[liveId].orEmpty(),
            available = availableAuctions,
            loading = actionLoading,
            error = actionError,
            hasNext = availableAuctionsHasNext,
            loadingMore = availableAuctionsLoadingMore,
            loadMoreError = availableAuctionsLoadMoreError,
            onLoadMore = onLoadMoreAvailableAuctions,
            onDismiss = { editingLiveId = null },
            onSave = { plans -> onSetItems(liveId, plans) }
        )
    }
}

internal fun formatLiveScheduledAt(
    scheduledAt: String?,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (scheduledAt.isNullOrBlank()) return "일정 확인 필요"
    return runCatching {
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
            .format(Instant.parse(scheduledAt).atZone(zoneId))
    }.getOrElse { scheduledAt.replace('T', ' ').take(16) }
}

@Composable private fun EmptyLiveCard() { Column(Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("예약한 Live가 없어요", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("새 방송에서 일정과 상품을 준비해보세요", Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 12.sp) } }

@Composable private fun StatusBadge(status: String) { val label = when(status) { "SCHEDULED" -> "예정"; "LIVE" -> "방송 중"; "ENDED" -> "종료"; else -> status }; Surface(color = if(status == "LIVE") Colors.Live else Colors.Navy, shape = RoundedCornerShape(10.dp)) { Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun LiveFormDialog(initial: LiveBroadcastSummary?, loading: Boolean, error: String?, onDismiss: () -> Unit, onSubmit: (String, String?, String, String?) -> Unit) {
    val initialDateTime = remember(initial?.liveBroadcastId, initial?.scheduledAt) {
        initial?.scheduledAt?.let { value ->
            runCatching { LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault()) }.getOrNull()
                ?: runCatching { LocalDateTime.parse(value) }.getOrNull()
        }
            ?: LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)
    }
    var title by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initial?.description.orEmpty()) }
    var date by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initialDateTime.toLocalDate().toString()) }
    var time by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initialDateTime.toLocalTime().toString().take(5)) }
    val scheduledLocalDateTime = runCatching { LocalDateTime.parse("${date}T${time}") }.getOrNull()
    val scheduledAt = scheduledLocalDateTime?.takeIf { it.isAfter(LocalDateTime.now()) }?.toString()
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text(if (initial == null) "새 Live 예약" else "Live 예약 수정", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("방송 제목") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = liveDialogFieldColors())
            OutlinedTextField(description, { description = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("방송 설명") }, minLines = 3, shape = RoundedCornerShape(12.dp), colors = liveDialogFieldColors())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it.take(10) }, Modifier.weight(1.2f), label = { Text("날짜") }, placeholder = { Text("2026-09-14") }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = liveDialogFieldColors())
                OutlinedTextField(time, { time = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, Modifier.weight(.8f), label = { Text("시간") }, placeholder = { Text("19:30") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), colors = liveDialogFieldColors())
            }
            if (scheduledAt == null) Text("현재 이후의 날짜와 시간을 입력해주세요.", color = Colors.Urgent, fontSize = 11.sp)
            error?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
        } },
        confirmButton = { Button({ scheduledAt?.let { onSubmit(title.trim(), description.trim().ifBlank { null }, it, null) } }, enabled = title.isNotBlank() && scheduledAt != null && !loading, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (initial == null) "예약" else "저장") } },
        dismissButton = { TextButton(onDismiss, enabled = !loading) { Text("취소", color = Colors.Muted) } }
    )
}

@Composable
private fun LiveItemDialog(
    current: List<AuctionSummary>,
    available: List<AuctionSummary>,
    loading: Boolean,
    error: String?,
    hasNext: Boolean,
    loadingMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (List<LiveItemPlan>) -> Unit
) {
    val choices = remember(current, available) { (current + available).distinctBy(AuctionSummary::auctionId) }
    // 편성 목록이 갱신될 때마다 초안을 통째로 새로 만들면 입력 중인 값이 사라진다.
    // 사용자가 건드린 항목은 그대로 두고, 새로 들어온 항목만 서버 값으로 채운다.
    val drafts = remember { mutableStateMapOf<String, LiveItemDraft>() }
    var touchedIds by remember { mutableStateOf(setOf<String>()) }
    fun touch(id: String) { touchedIds = touchedIds + id }
    LaunchedEffect(current) {
        current.forEach { auction ->
            if (auction.auctionId !in drafts && auction.auctionId !in touchedIds) drafts[auction.auctionId] = auction.toLiveItemDraft()
        }
    }
    val invalidCount = choices.count { auction ->
        val draft = drafts[auction.auctionId]
        draft != null && !auction.isLiveAuctionActive() && !draft.isValid()
    }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("Live 상품 편성", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("${drafts.size}/10개 선택", color = Colors.Muted, fontSize = 11.sp) } },
        text = { Column(Modifier.fillMaxWidth()) {
            if (choices.isEmpty() && !hasNext) Text("편성 가능한 예약 경매가 없어요.\n등록 상품에서 경매를 먼저 예약해주세요.", color = Colors.Muted, fontSize = 12.sp)
            else LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(choices, key = AuctionSummary::auctionId) { auction ->
                    val draft = drafts[auction.auctionId]
                    val active = auction.isLiveAuctionActive()
                    Column(Modifier.fillMaxWidth().background(Colors.Surface, RoundedCornerShape(12.dp)).padding(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                touch(auction.auctionId)
                                if (draft != null) drafts.remove(auction.auctionId)
                                else if (drafts.size < 10) drafts[auction.auctionId] = auction.toLiveItemDraft()
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(draft != null, onCheckedChange = null)
                            if (auction.imageUrls.firstOrNull().isNullOrBlank()) Box(Modifier.size(48.dp).background(Color(0xFFE9EDF2), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.product_outline), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Muted)) }
                            else DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(48.dp))
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(auction.title, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("시작가 " + (auction.startPriceOrNull?.let { "%,d원".format(it) } ?: "가격 미정"), color = Colors.Muted, fontSize = 11.sp)
                            }
                        }
                        if (draft != null && active) Text("진행 중인 경매라 시작가와 시간을 바꿀 수 없어요.", Modifier.padding(start = 54.dp, bottom = 6.dp), color = Colors.Muted, fontSize = 11.sp)
                        if (draft != null && !active) Column(Modifier.padding(start = 54.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    draft.startPrice,
                                    { value -> touch(auction.auctionId); drafts[auction.auctionId] = draft.copy(startPrice = value.filter(Char::isDigit).take(10)) },
                                    Modifier.weight(1.2f),
                                    label = { Text("시작가", fontSize = 11.sp) },
                                    suffix = { Text("원") },
                                    singleLine = true,
                                    isError = draft.startPrice.isNotBlank() && !draft.isStartPriceValid(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    draft.seconds,
                                    { value -> touch(auction.auctionId); drafts[auction.auctionId] = draft.copy(seconds = value.filter(Char::isDigit).take(3)) },
                                    Modifier.weight(.8f),
                                    label = { Text("시간", fontSize = 11.sp) },
                                    suffix = { Text("초") },
                                    singleLine = true,
                                    isError = draft.seconds.isNotBlank() && !draft.isSecondsValid(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(30L to "30초", 60L to "1분", 120L to "2분", 180L to "3분", 300L to "5분").forEach { (s, label) ->
                                    FilterChip(
                                        selected = draft.seconds == s.toString(),
                                        onClick = { touch(auction.auctionId); drafts[auction.auctionId] = draft.copy(seconds = s.toString()) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, labelColor = Colors.Muted, selectedContainerColor = Colors.Navy, selectedLabelColor = Color.White),
                                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = draft.seconds == s.toString(), borderColor = Colors.Border, selectedBorderColor = Colors.Navy)
                                    )
                                }
                            }
                            if (!draft.isValid()) Text("시작가는 1,000원 이상, 경매 시간은 30초 이상 5분(300초) 이하여야 해요.", color = Colors.Urgent, fontSize = 11.sp)
                        }
                    }
                }
                if (hasNext || loadingMore || loadMoreError != null) item(key = "available-auction-load-more") {
                    LaunchedEffect(choices.size, hasNext, loadingMore, loadMoreError) {
                        if (hasNext && !loadingMore && loadMoreError == null) onLoadMore()
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            loadingMore -> CircularProgressIndicator(Modifier.size(22.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            loadMoreError != null -> {
                                Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                TextButton(onClick = onLoadMore) { Text("다시 불러오기") }
                            }
                        }
                    }
                }
            }
            error?.let { Text(it, Modifier.padding(top = 8.dp), color = Colors.Urgent, fontSize = 11.sp) }
        } },
        confirmButton = {
            Button(
                {
                    // 목록에서 빠진 경매는 서버가 편성을 해제한다
                    onSave(
                        choices.mapNotNull { auction ->
                            val draft = drafts[auction.auctionId] ?: return@mapNotNull null
                            if (auction.isLiveAuctionActive()) LiveItemPlan(auction.auctionId)
                            else LiveItemPlan(auction.auctionId, draft.startPrice.toLongOrNull(), draft.seconds.toLongOrNull())
                        }
                    )
                },
                enabled = drafts.isNotEmpty() && drafts.size <= 10 && invalidCount == 0 && !loading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
            ) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("저장") }
        },
        dismissButton = { TextButton(onDismiss, enabled = !loading) { Text("취소", color = Colors.Muted) } }
    )
}

private data class LiveItemDraft(val startPrice: String, val seconds: String) {
    companion object { const val MIN_SECONDS = 30L; const val MAX_SECONDS = 300L }
    fun isStartPriceValid(): Boolean = (startPrice.toLongOrNull() ?: 0L) >= 1_000L
    fun isSecondsValid(): Boolean = (seconds.toLongOrNull() ?: 0L) in MIN_SECONDS..MAX_SECONDS
    fun isValid(): Boolean = isStartPriceValid() && isSecondsValid()
}

private fun AuctionSummary.toLiveItemDraft() = LiveItemDraft(
    startPrice = startPriceOrNull?.toString().orEmpty(),
    // 서버가 준 초 값을 그대로 보여준다. 분 단위로 올리면 2분 저장이 5분으로 보였다.
    seconds = auctionTimeSeconds.takeIf { it > 0 }?.toString().orEmpty()
)

private fun AuctionSummary.isLiveAuctionActive(): Boolean = status.equals("ACTIVE", ignoreCase = true)

@Composable
private fun liveDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Colors.Navy,
    unfocusedBorderColor = Colors.Border,
    focusedLabelColor = Colors.Navy,
    unfocusedLabelColor = Colors.Muted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
