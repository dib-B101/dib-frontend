package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.domain.order.OrderMessage
import com.ssafy.dib.ui.theme.WireframeColors as Colors

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
    onRetry: () -> Unit,
    onLoadEarlier: () -> Unit,
    onSend: (String) -> Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable(orderId) { mutableStateOf("") }
    val orderedMessages = remember(messages) { messages.distinctBy(OrderMessage::chattingId).sortedBy(OrderMessage::time) }
    val listState = rememberLazyListState()
    val latestMessageId = orderedMessages.lastOrNull()?.chattingId
    LaunchedEffect(latestMessageId) {
        if (orderedMessages.isNotEmpty()) listState.animateScrollToItem(orderedMessages.lastIndex)
    }
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFF7F9FB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(52.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(52.dp).clickable(onClick = onBack).padding(start = 15.dp, top = 9.dp), fontSize = 22.sp)
                Text("거래 채팅", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(connectionLabel(connectionState), Modifier.padding(end = 16.dp), color = Colors.Muted, fontSize = 10.sp)
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().imePadding().background(Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it.take(500) }, Modifier.weight(1f), placeholder = { Text("메시지를 입력하세요") }, singleLine = true, shape = RoundedCornerShape(22.dp))
                Button(
                    onClick = { if (onSend(input)) input = "" },
                    enabled = input.isNotBlank() && connectionState == RealtimeConnectionState.Connected,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) { Text("전송", fontWeight = FontWeight.Bold) }
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) }
            errorMessage != null && orderedMessages.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(errorMessage, color = Colors.Muted)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) { Text("다시 불러오기") }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Surface(color = if (mine) Colors.Navy else Color.White, shape = RoundedCornerShape(14.dp)) {
                            Text(message.content, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = if (mine) Color.White else Colors.Text, fontSize = 13.sp)
                        }
                        Text(message.time.take(16).replace('T', ' '), color = Colors.Muted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

private fun connectionLabel(state: RealtimeConnectionState?) = when (state) {
    RealtimeConnectionState.Connected -> "실시간 연결"
    RealtimeConnectionState.Connecting -> "연결 중"
    RealtimeConnectionState.Reconnecting -> "재연결 중"
    else -> "연결 끊김"
}
