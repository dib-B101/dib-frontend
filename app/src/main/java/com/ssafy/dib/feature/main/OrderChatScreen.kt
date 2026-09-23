package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.R
import com.ssafy.dib.core.time.formatServerTime
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.core.ui.DibNotificationBell
import com.ssafy.dib.domain.order.OrderMessage
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderChatScreen(
    orderId: String,
    currentMemberId: String,
    messages: List<OrderMessage>,
    isLoading: Boolean,
    hasMore: Boolean,
    isLoadingEarlier: Boolean,
    loadEarlierError: String?,
    errorMessage: String?,
    connectionState: RealtimeConnectionState?,
    canSend: Boolean,
    reportSubmitting: Boolean,
    reportError: String?,
    reportCompleted: Boolean,
    onRetry: () -> Unit,
    onLoadEarlier: () -> Unit,
    onSend: (String) -> Boolean,
    onReportParticipant: (String, String) -> Unit,
    onDismissReport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable(orderId) { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<OrderMessage?>(null) }
    var reportReason by rememberSaveable(orderId) { mutableStateOf<String?>(null) }
    var reportDetail by rememberSaveable(orderId) { mutableStateOf("") }
    val orderedMessages = remember(messages) { messages.distinctBy(OrderMessage::chattingId).sortedBy(OrderMessage::time) }
    val latestParticipantMessage = orderedMessages.lastOrNull {
        currentMemberId.isNotBlank() && it.memberId != currentMemberId
    }
    val listState = rememberLazyListState()
    val latestMessageId = orderedMessages.lastOrNull()?.chattingId
    LaunchedEffect(latestMessageId) {
        if (orderedMessages.isNotEmpty()) listState.animateScrollToItem(orderedMessages.lastIndex)
    }
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.background(Colors.Background)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick=onBack){Image(painterResource(R.drawable.back),"뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))}
                Column(Modifier.weight(1f)) { Text("거래 채팅", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.size(6.dp).background(if(connectionState==RealtimeConnectionState.Connected)Colors.MintInk else Colors.Muted,CircleShape));Text(connectionLabel(connectionState), color = Colors.Muted, fontSize = 10.sp)} }
                Text(
                    "신고",
                    Modifier.clickable(enabled = latestParticipantMessage != null) {
                        reportTarget = latestParticipantMessage
                        reportReason = null
                        reportDetail = ""
                        onDismissReport()
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    color = if (latestParticipantMessage != null) Colors.Navy else Colors.Muted.copy(alpha = .45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                DibNotificationBell()
            }
            HorizontalDivider(color=Colors.Border)
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().imePadding().background(Colors.Background)) { HorizontalDivider(color=Colors.Border);Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    input,
                    { input = it.take(500) },
                    Modifier.weight(1f),
                    enabled = canSend,
                    placeholder = { Text(if (canSend) "메시지를 입력하세요" else "종료된 거래에서는 채팅할 수 없어요") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Colors.Border,focusedContainerColor=Colors.Surface,unfocusedContainerColor=Colors.Surface)
                )
                Button(
                    onClick = { if (onSend(input)) input = "" },
                    enabled = canSend && input.isNotBlank() && connectionState == RealtimeConnectionState.Connected,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) { Text("전송", fontWeight = FontWeight.Bold) }
            }}
        }
    ) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null && orderedMessages.isEmpty() -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(errorMessage, color = Colors.Muted)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal=16.dp,vertical=18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasMore || isLoadingEarlier || loadEarlierError != null) {
                    item(key = "load-earlier") {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when {
                                isLoadingEarlier -> CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Colors.Navy,
                                    strokeWidth = 2.dp
                                )
                                loadEarlierError != null -> {
                                    Text(loadEarlierError, color = Colors.Urgent, fontSize = 11.sp)
                                    TextButton(onClick = onLoadEarlier) { Text("이전 메시지 다시 불러오기") }
                                }
                                hasMore -> TextButton(onClick = onLoadEarlier) { Text("이전 메시지 불러오기") }
                            }
                        }
                    }
                }
                errorMessage?.let { item { Text(it, color = Colors.Urgent, fontSize = 11.sp) } }
                items(orderedMessages, key = OrderMessage::chattingId) { message ->
                    val mine = message.memberId == currentMemberId
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                        if (!mine) {
                            Text(
                                message.memberNickname?.takeIf(String::isNotBlank) ?: "거래 상대",
                                Modifier.padding(start = 4.dp, bottom = 3.dp),
                                color = Colors.Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(color = if (mine) Colors.Navy else Colors.Background, shape = if(mine)RoundedCornerShape(16.dp,4.dp,16.dp,16.dp) else RoundedCornerShape(4.dp,16.dp,16.dp,16.dp)) {
                            Text(message.content, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = if (mine) Color.White else Colors.Text, fontSize = 14.sp,lineHeight=20.sp)
                        }
                        Text(formatServerTime(message.time) ?: message.time.take(16).replace('T', ' '),Modifier.padding(top=3.dp), color = Colors.Muted, fontSize = 9.sp)
                    }
                }
            }
            }
        }
    }
    reportTarget?.let { target ->
        val selectedReason = reportReason
        val detailLimit = orderReportDetailLimit(selectedReason.orEmpty(), target.content)
        ModalBottomSheet(
            onDismissRequest = {
                if (!reportSubmitting) {
                    reportTarget = null
                    onDismissReport()
                }
            },
            containerColor = Color.White
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("회원 신고", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFF8F9FB), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("상대 회원", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(target.memberNickname?.takeIf(String::isNotBlank) ?: "거래 상대", color = Colors.Muted, fontSize = 10.sp)
                        Text("최근 메시지 · “${target.content}”", color = Colors.Muted, fontSize = 11.sp, maxLines = 2)
                    }
                }
                if (reportCompleted) {
                    Surface(Modifier.fillMaxWidth(), color = Colors.Mint.copy(alpha = .22f), shape = RoundedCornerShape(14.dp)) {
                        Text("신고가 접수됐어요.", Modifier.padding(16.dp), color = Colors.MintInk, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("신고 이유를 선택해주세요", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Surface(Modifier.fillMaxWidth(), color = Color(0xFFF9F9FB), shape = RoundedCornerShape(16.dp)) {
                        Column {
                            orderReportReasons.forEach { reason ->
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !reportSubmitting) {
                                        reportReason = reason
                                        reportDetail = reportDetail.take(orderReportDetailLimit(reason, target.content))
                                    }.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedReason == reason, onClick = null, enabled = !reportSubmitting)
                                    Text(reason, color = Colors.Text, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reportDetail,
                        onValueChange = { reportDetail = it.take(detailLimit) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !reportSubmitting,
                        label = { Text("상세 내용 (선택)") },
                        placeholder = { Text("상황을 조금 더 자세히 알려주세요.") },
                        supportingText = { Text("${reportDetail.length}/$detailLimit") }
                    )
                    Text("허위 신고 또는 반복적인 악의적 신고는 서비스 이용에 제한이 있을 수 있어요.", color = Colors.Muted, fontSize = 11.sp)
                    reportError?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
                }
                Button(
                    onClick = {
                        if (reportCompleted) {
                            reportTarget = null
                            onDismissReport()
                        } else if (selectedReason != null) {
                            onReportParticipant(target.memberId, buildOrderReportContent(selectedReason, target.content, reportDetail))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = reportCompleted || (selectedReason != null && !reportSubmitting),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) {
                    Text(if (reportCompleted) "확인" else if (reportSubmitting) "접수 중" else "신고하기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val orderReportReasons = listOf(
    "비매너·욕설 등 부적절한 언행",
    "거래 약속 불이행",
    "사기 또는 외부 거래 유도",
    "반복적인 허위·부적절 판매",
    "기타"
)

private fun orderReportPrefix(reason: String, message: String): String = buildString {
    append("[신고 사유] ").append(reason).append('\n')
    append("[거래 채팅 메시지] ").append(message.take(220))
    if (message.length > 220) append('…')
    append("\n[상세 내용] ")
}

private fun orderReportDetailLimit(reason: String, message: String): Int =
    (500 - orderReportPrefix(reason, message).length).coerceAtLeast(0)

private fun buildOrderReportContent(reason: String, message: String, detail: String): String =
    (orderReportPrefix(reason, message) + detail.trim()).take(500)

private fun connectionLabel(state: RealtimeConnectionState?) = when (state) {
    RealtimeConnectionState.Connected -> "실시간 연결"
    RealtimeConnectionState.Connecting -> "연결 중"
    RealtimeConnectionState.Reconnecting -> "재연결 중"
    else -> "연결 끊김"
}
