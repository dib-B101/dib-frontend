package com.ssafy.dib.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ssafy.dib.core.ui.DibNetworkImage
import com.ssafy.dib.domain.auction.AuctionSummary
import com.ssafy.dib.domain.live.LiveBroadcastSummary
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
    onSetItems: (liveBroadcastId: String, auctionIds: List<String>) -> Unit,
    onPrepareStream: (liveBroadcastId: String) -> Unit,
    onStartLive: (liveBroadcastId: String) -> Unit,
    onStartAuction: (liveBroadcastId: String, auctionId: String) -> Unit,
    onEndLive: (liveBroadcastId: String) -> Unit,
    onBack: () -> Unit,
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
            Row(Modifier.fillMaxWidth().height(52.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(52.dp).clickable(onClick = onBack).wrapContentSize(), fontSize = 24.sp)
                Text("Live 방송 관리", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }, containerColor = Colors.Navy, contentColor = Color.White) {
                Text("+ 새 방송", fontWeight = FontWeight.Bold)
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
                                    OutlinedButton(onClick = { onPrepareStream(live.liveBroadcastId) }, Modifier.weight(1f), enabled = !actionLoading) { Text(if (live.streamUrl.isNullOrBlank()) "송출 준비" else "송출 갱신") }
                                }
                                Button(
                                    onClick = { onStartLive(live.liveBroadcastId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = auctions.isNotEmpty() && !live.streamUrl.isNullOrBlank() && !actionLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                                ) { Text("Live 시작", fontWeight = FontWeight.Bold) }
                                if (auctions.isEmpty() || live.streamUrl.isNullOrBlank()) Text("상품 편성과 송출 준비를 완료하면 시작할 수 있어요.", color = Colors.Muted, fontSize = 10.sp)
                            }
                            "LIVE" -> {
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
            onSave = { ids -> onSetItems(liveId, ids) }
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
        initial?.scheduledAt?.let { runCatching { LocalDateTime.ofInstant(Instant.parse(it), ZoneId.systemDefault()) }.getOrNull() }
            ?: LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)
    }
    var title by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initial?.description.orEmpty()) }
    var date by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initialDateTime.toLocalDate().toString()) }
    var time by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initialDateTime.toLocalTime().toString().take(5)) }
    var streamUrl by rememberSaveable(initial?.liveBroadcastId) { mutableStateOf(initial?.streamUrl.orEmpty()) }
    val scheduledInstant = runCatching { LocalDateTime.parse("${date}T${time}").atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
    val scheduledAt = scheduledInstant?.takeIf { it.isAfter(Instant.now()) }?.toString()
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (initial == null) "새 Live 예약" else "Live 예약 수정") },
        text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("방송 제목") }, singleLine = true)
            OutlinedTextField(description, { description = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("방송 설명") }, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it.take(10) }, Modifier.weight(1.2f), label = { Text("날짜") }, placeholder = { Text("2026-09-14") }, singleLine = true)
                OutlinedTextField(time, { time = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, Modifier.weight(.8f), label = { Text("시간") }, placeholder = { Text("19:30") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            OutlinedTextField(streamUrl, { streamUrl = it }, Modifier.fillMaxWidth(), label = { Text("스트림 URL (선택)") }, singleLine = true)
            if (scheduledAt == null) Text("현재 이후의 날짜와 시간을 입력해주세요.", color = Colors.Urgent, fontSize = 11.sp)
            error?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
        } },
        confirmButton = { TextButton({ scheduledAt?.let { onSubmit(title.trim(), description.trim().ifBlank { null }, it, streamUrl.trim().ifBlank { null }) } }, enabled = title.isNotBlank() && scheduledAt != null && !loading) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(if (initial == null) "예약" else "저장") } },
        dismissButton = { TextButton(onDismiss, enabled = !loading) { Text("취소") } }
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
    onSave: (List<String>) -> Unit
) {
    val choices = remember(current, available) { (current + available).distinctBy(AuctionSummary::auctionId) }
    val selected = remember(current) { mutableStateListOf<String>().apply { addAll(current.map(AuctionSummary::auctionId)) } }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Live 상품 편성 (${selected.size}/10)") },
        text = { Column(Modifier.fillMaxWidth()) {
            if (choices.isEmpty() && !hasNext) Text("편성 가능한 예약 경매가 없어요.\n등록 상품에서 경매를 먼저 예약해주세요.", color = Colors.Muted, fontSize = 12.sp)
            else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(choices, key = AuctionSummary::auctionId) { auction ->
                    Row(Modifier.fillMaxWidth().clickable { if (auction.auctionId in selected) selected.remove(auction.auctionId) else if (selected.size < 10) selected.add(auction.auctionId) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(auction.auctionId in selected, onCheckedChange = null)
                        DibNetworkImage(auction.imageUrls.firstOrNull(), auction.title, Modifier.size(44.dp))
                        Column(Modifier.padding(start = 10.dp)) { Text(auction.title, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("시작가 ${"%,d".format(auction.startPrice)}원", color = Colors.Muted, fontSize = 11.sp) }
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
        confirmButton = { TextButton({ onSave(selected.toList()) }, enabled = selected.isNotEmpty() && selected.size <= 10 && !loading) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("저장") } },
        dismissButton = { TextButton(onDismiss, enabled = !loading) { Text("취소") } }
    )
}
