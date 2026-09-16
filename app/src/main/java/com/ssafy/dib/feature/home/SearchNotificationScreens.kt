package com.ssafy.dib.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.domain.notification.DomainNotification
import com.ssafy.dib.data.remote.socket.RealtimeConnectionState
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class AuctionSearchFilters(
    val query: String,
    val categoryId: String?,
    val minPrice: Long?,
    val maxPrice: Long?,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionSearchScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    remoteCategories: List<ProductCategory>?,
    remoteAuctions: List<HomeAuction>?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onSearch: (AuctionSearchFilters) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var recent by rememberSaveable { mutableStateOf(listOf("필름 카메라", "머그컵", "작가 핸드메이드")) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("전체") }
    var price by rememberSaveable { mutableStateOf("전체") }
    var status by rememberSaveable { mutableStateOf("진행 중") }
    val fallbackCategories = remember {
        listOf(ProductCategory("1", "디지털기기"), ProductCategory("8", "예술·창작"))
    }
    val categories = remoteCategories ?: fallbackCategories
    val selectedCategoryId = categories.firstOrNull { it.name == category }?.categoryId
    fun filters() = AuctionSearchFilters(
        query = query.trim(),
        categoryId = selectedCategoryId,
        minPrice = if (price == "5~10만원") 50_000 else null,
        maxPrice = when (price) { "5만원 이하" -> 50_000; "5~10만원" -> 100_000; else -> null },
        status = when (status) { "예정" -> "SCHEDULED"; "종료" -> "ENDED"; else -> "ACTIVE" }
    )
    fun submit() {
        submitted = true
        if (query.isNotBlank()) recent = (listOf(query.trim()) + recent).distinct().take(5)
        onSearch(filters())
    }
    val sourceAuctions = remoteAuctions ?: allHomeAuctions.distinctBy(HomeAuction::id).filter {
        (category == "전체" || it.category.contains(category.removeSuffix("기기"))) &&
            (price == "전체" || price == "5만원 이하" && it.price <= 50_000 || price == "5~10만원" && it.price in 50_000..100_000) &&
            it.status == filters().status
    }
    val results = when {
        !submitted -> emptyList()
        remoteAuctions != null -> sourceAuctions
        else -> sourceAuctions.filter {
            query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || query.contains("카메라") && it.id == "camera"
        }
    }

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = androidx.compose.ui.graphics.Color(0xFFF7F9FB), contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { SimpleAppBar("검색", onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it; submitted = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    placeholder = { Text("상품과 작가를 검색해보세요", color = Colors.Muted, fontSize = 14.sp) },
                    leadingIcon = { Text("⌕", fontSize = 23.sp) },
                    trailingIcon = if(query.isNotBlank()) ({ Text("×", Modifier.clickable { query=""; submitted=false }, fontSize = 20.sp) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            if (!submitted) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("최근 검색어", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("전체 삭제", Modifier.clickable { recent = emptyList() }, color = Colors.Muted, fontSize = 12.sp) } }
                if (recent.isNotEmpty()) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { recent.take(3).forEach { word -> FilterChip(selected=false,onClick={query=word; submit()},label={Text(word,fontSize=12.sp)}) } } }
                item { Text("인기 검색어", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                items(5) { index -> val word=listOf("빈티지 카메라","핸드메이드 도자기","한정판 스니커즈","원화 작품","레트로 게임기")[index]; Row(Modifier.fillMaxWidth().height(32.dp).clickable { query=word; submit() }, verticalAlignment = Alignment.CenterVertically) { Text("${index+1}", Modifier.width(32.dp), color = if(index<3) androidx.compose.ui.graphics.Color(0xFFF5634F) else Colors.Muted, fontWeight=FontWeight.Bold); Text(word,fontSize=14.sp,fontWeight=if(index<3)FontWeight.Bold else FontWeight.Normal) } }
            } else if (isLoading) {
                item { LoadingContent("경매를 불러오고 있어요") }
            } else if (errorMessage != null) {
                item { NetworkErrorContent(onRetry) }
            } else {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("검색 결과 ${results.size}개", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("조건에 맞는 경매", color = Colors.Muted, fontSize = 12.sp) } }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(true,{}, {Text(status)}); FilterChip(category!="전체",{}, {Text(category)}); FilterChip(price!="전체",{}, {Text(price)}); FilterChip(false,{showFilters=true},{Text("필터")}) } }
                if(results.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(top=80.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text("검색 결과가 없어요",fontSize=18.sp,fontWeight=FontWeight.Bold); Text("검색어나 필터를 바꿔보세요",Modifier.padding(top=8.dp),color=Colors.Muted,fontSize=12.sp) } }
                items(results.chunked(2).size) { rowIndex -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){ results.chunked(2)[rowIndex].forEach { auction -> SearchAuctionCard(auction,{onProductClick(auction.id)},Modifier.weight(1f)) }; if(results.chunked(2)[rowIndex].size==1) Spacer(Modifier.weight(1f)) } }
                if (hasNext || isLoadingMore || loadMoreError != null) item(key = "search-load-more") {
                    LaunchedEffect(results.size, hasNext, isLoadingMore, loadMoreError) {
                        if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            isLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                            loadMoreError != null -> {
                                Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp)
                                TextButton(onClick = onLoadMore) { Text("더 불러오기") }
                            }
                        }
                    }
                }
            }
        }
    }
    if(showFilters) ModalBottomSheet(onDismissRequest={showFilters=false},containerColor=androidx.compose.ui.graphics.Color.White){
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("검색 조건",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("×",Modifier.clickable{showFilters=false},fontSize=22.sp)}
            FilterGroup("카테고리",listOf("전체") + categories.map(ProductCategory::name),category){category=it}
            FilterGroup("가격 범위",listOf("전체","5만원 이하","5~10만원"),price){price=it}
            FilterGroup("경매 상태",listOf("진행 중","예정","종료"),status){status=it}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("초기화",Modifier.width(88.dp).clickable{category="전체";price="전체";status="진행 중"},color=Colors.Muted,fontWeight=FontWeight.Bold);Button({showFilters=false;submit()},Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Colors.Navy)){Text("결과 보기",fontWeight=FontWeight.Bold)}}
        }
    }
}

@Composable private fun SearchAuctionCard(auction:HomeAuction,onClick:()->Unit,modifier:Modifier=Modifier){val statusLabel=when(auction.status){"SCHEDULED"->"예정";"ENDED"->"종료";else->"진행중"};Column(modifier.clickable(onClick=onClick),verticalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.fillMaxWidth().height(122.dp)){ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.fillMaxSize());Surface(Modifier.padding(8.dp),color=Colors.Navy,shape=RoundedCornerShape(12.dp)){Text(statusLabel,Modifier.padding(horizontal=8.dp,vertical=5.dp),color=androidx.compose.ui.graphics.Color.White,fontSize=9.sp)}};Text(auction.name,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${auction.pricePrefix} ${auction.priceLabel}",color=Colors.Navy,fontSize=13.sp,fontWeight=FontWeight.Bold);Text("입찰 ${auction.bidCount}회",color=Colors.Muted,fontSize=9.sp)}}
@Composable private fun FilterGroup(title:String,values:List<String>,selected:String,onSelect:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontSize=13.sp,fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){values.forEach{FilterChip(selected=selected==it,onClick={onSelect(it)},label={Text(it,fontSize=12.sp)})}}}}

@Composable
fun NotificationCenterScreen(
    notifications: List<DomainNotification>,
    connectionState: RealtimeConnectionState?,
    isLoading: Boolean,
    errorMessage: String?,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    actionNotificationId: String?,
    actionError: String?,
    isNotificationActionable: (DomainNotification) -> Boolean,
    onNotificationClick: (DomainNotification) -> Unit,
    onMarkRead: (DomainNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onAcceptOffer: (DomainNotification) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
){
    var filter by rememberSaveable{mutableStateOf("전체")}
    var offerToConfirm by remember { mutableStateOf<DomainNotification?>(null) }
    val shown = if (filter == "전체") notifications else notifications.filter { it.category.label == filter }
    Scaffold(modifier.fillMaxSize().safeDrawingPadding(),containerColor=androidx.compose.ui.graphics.Color.White,contentWindowInsets=WindowInsets(0,0,0,0),topBar={SimpleAppBar("알림",onBack,"설정",onSettingsClick)},bottomBar={DibBottomNavigation(DibMainTab.Home,onTabSelected)}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("전체","라이브","찜","거래").forEach{FilterChip(filter==it,{filter=it},{Text(it)})}}}
            if (notifications.any { !it.isRead }) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onMarkAllRead, enabled = actionNotificationId == null) { Text("모두 읽음", color = Colors.Navy) } } }
            }
            actionError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFFFFEEF0), RoundedCornerShape(12.dp)).padding(12.dp), color = Colors.Urgent, fontSize = 11.sp) } }
            if (connectionState == RealtimeConnectionState.Connecting || connectionState == RealtimeConnectionState.Reconnecting) {
                item { Text(if(connectionState == RealtimeConnectionState.Connecting) "실시간 알림에 연결하고 있어요" else "실시간 알림을 다시 연결하고 있어요", color=Colors.Muted, fontSize=11.sp) }
            }
            when {
                isLoading && notifications.isEmpty() -> item { LoadingContent("알림을 불러오고 있어요") }
                errorMessage != null && notifications.isEmpty() -> item { NetworkErrorContent(onRetry) }
                shown.isEmpty() -> item { EmptyContent("새로운 알림이 없어요", "새 알림과 거래 안내가 여기에 표시돼요") }
            }
            items(shown.size, key = { shown[it].eventId }) { index ->
                val item = shown[index]
                val actionable = isNotificationActionable(item)
                Column(
                    Modifier.fillMaxWidth()
                        .background(if (item.isRead) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFF5FBF9), RoundedCornerShape(14.dp))
                        .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
                        .clickable(enabled = actionable) { onMarkRead(item); onNotificationClick(item) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isLive = item.category.label == "라이브"
                        Box(Modifier.size(44.dp).background(if(isLive)androidx.compose.ui.graphics.Color(0xFFFFE4E9)else androidx.compose.ui.graphics.Color(0xFFE8FAF5),CircleShape),contentAlignment=Alignment.Center){Text(if(isLive)"●" else "d",color=if(isLive)androidx.compose.ui.graphics.Color(0xFFEF596B)else Colors.Navy,fontWeight=FontWeight.Bold)}
                        Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
                            Text(item.category.label,color=if(isLive)androidx.compose.ui.graphics.Color(0xFFEF596B)else Colors.Navy,fontSize=9.sp,fontWeight=FontWeight.Bold)
                            Text(item.title,color=Colors.Navy,fontSize=13.sp,fontWeight=FontWeight.Bold)
                            Text(item.body,color=Colors.Muted,fontSize=10.sp, lineHeight = 15.sp)
                        }
                        Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(notificationTimeLabel(item.occurredAt),color=Colors.Muted,fontSize=9.sp)
                            if (!item.isRead) Text("새 알림", color=androidx.compose.ui.graphics.Color(0xFF27806E),fontSize=9.sp,fontWeight=FontWeight.Bold)
                            else if(actionable) Text("›",color=Colors.Navy,fontSize=18.sp)
                        }
                    }
                    if (item.isRunnerUpOffer) {
                        Button(
                            onClick = { offerToConfirm = item },
                            enabled = actionNotificationId == null,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                        ) {
                            if (actionNotificationId == item.eventId) CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                            else Text("차순위 구매 제안 수락", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (hasNext || isLoadingMore || loadMoreError != null) item(key = "notification-load-more") {
                LaunchedEffect(notifications.size, hasNext, isLoadingMore, loadMoreError) {
                    if (hasNext && !isLoadingMore && loadMoreError == null) onLoadMore()
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        isLoadingMore -> CircularProgressIndicator(Modifier.size(24.dp), color = Colors.Navy, strokeWidth = 2.dp)
                        loadMoreError != null -> { Text(loadMoreError, color = Colors.Muted, fontSize = 11.sp); TextButton(onClick = onLoadMore) { Text("더 불러오기") } }
                    }
                }
            }
        }
    }
    offerToConfirm?.let { offer ->
        AlertDialog(
            onDismissRequest = { offerToConfirm = null },
            title = { Text("차순위 구매 제안을 수락할까요?") },
            text = { Text("수락하면 주문이 생성되고 등록 카드로 즉시 자동결제를 요청합니다. 제안은 알림 생성 후 24시간 동안 유효해요.") },
            confirmButton = { TextButton({ offerToConfirm = null; onMarkRead(offer); onAcceptOffer(offer) }) { Text("수락하고 결제", color = Colors.Navy, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ offerToConfirm = null }) { Text("나중에") } }
        )
    }
}

@Composable private fun LoadingContent(label: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { CircularProgressIndicator(color = Colors.Navy); Text(label, color = Colors.Muted, fontSize = 13.sp) } }
@Composable private fun EmptyContent(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 12.sp) } }
@Composable private fun NetworkErrorContent(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("연결이 원활하지 않아요", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("네트워크를 확인하고 다시 시도해주세요", color = Colors.Muted, fontSize = 12.sp); Button(onRetry, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) { Text("다시 시도") } } }
private fun notificationTimeLabel(occurredAt: String): String = runCatching {
    val occurredInstant = runCatching { Instant.parse(occurredAt) }.getOrElse {
        LocalDateTime.parse(occurredAt).atZone(ZoneId.systemDefault()).toInstant()
    }
    val seconds = Duration.between(occurredInstant, Instant.now()).seconds.coerceAtLeast(0)
    when {
        seconds < 60 -> "방금"
        seconds < 3_600 -> "${seconds / 60}분 전"
        seconds < 86_400 -> "${seconds / 3_600}시간 전"
        else -> "${seconds / 86_400}일 전"
    }
}.getOrDefault(occurredAt)

@Composable private fun SimpleAppBar(title:String,onBack:()->Unit,action:String="",onActionClick:(()->Unit)?=null){Row(Modifier.fillMaxWidth().height(48.dp).background(androidx.compose.ui.graphics.Color.White),verticalAlignment=Alignment.CenterVertically){Text("←",Modifier.size(48.dp).clickable(onClick=onBack).wrapContentSize(),fontSize=24.sp);Text(title,Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.Bold);if(action.isNotBlank())Text(action,Modifier.clickable(enabled=onActionClick!=null){onActionClick?.invoke()}.padding(horizontal=16.dp,vertical=14.dp),color=Colors.Muted,fontSize=12.sp)}}
