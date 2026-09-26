package com.ssafy.dib.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.core.ui.DibSearchField
import com.ssafy.dib.core.ui.DibSubAppBar
import com.ssafy.dib.domain.auction.matchesAuctionStatusFilter
import com.ssafy.dib.core.ui.DibContentView
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibViewModeToggle
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.R
import com.ssafy.dib.domain.product.ProductCategory
import com.ssafy.dib.core.ui.categoryDisplayName
import com.ssafy.dib.core.ui.categoryOrder
import com.ssafy.dib.domain.product.DefaultProductCategories
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
    val status: String,
    val sort: String = "LATEST"
)

// 서버 sort 파라미터 값과 화면 라벨
val auctionSortOptions = listOf(
    "LATEST" to "최신순",
    "ENDING_SOON" to "마감임박순",
    "POPULAR" to "인기순",
    "PRICE_ASC" to "낮은가격순",
    "PRICE_DESC" to "높은가격순",
    "BID_COUNT" to "입찰많은순"
)

/** 가격 필터 구간. [minExclusive]보다 크고 [maxInclusive] 이하인 가격을 포함한다. null은 제한 없음. */
private data class PriceRange(val label: String, val minExclusive: Long?, val maxInclusive: Long?) {
    /** 서버 minPrice는 이상(>=) 조건이므로 하한을 1원 올려 보낸다. */
    val minPriceParam: Long? get() = minExclusive?.plus(1)
    val maxPriceParam: Long? get() = maxInclusive
    fun contains(price: Long): Boolean = (minExclusive == null || price > minExclusive) && (maxInclusive == null || price <= maxInclusive)
}

private val PriceRanges = listOf(
    PriceRange("전체", null, null),
    PriceRange("1만원 이하", null, 10_000),
    PriceRange("1~5만원", 10_000, 50_000),
    PriceRange("5~10만원", 50_000, 100_000),
    PriceRange("10~30만원", 100_000, 300_000),
    PriceRange("30만원 이상", 300_000, null)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionSearchScreen(
    browseOnOpen: Boolean,
    closingSoonOnOpen: Boolean,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    // 최근 검색어는 기기에 저장된 실제 기록, 인기 검색어는 서버 집계(null 이면 불러오는 중)
    recentSearches: List<String>,
    onAddRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    popularKeywords: List<String>?,
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
    var submitted by rememberSaveable(browseOnOpen, closingSoonOnOpen) { mutableStateOf(browseOnOpen) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var contentView by rememberSaveable { mutableStateOf(DibContentView.Grid) }
    var category by rememberSaveable { mutableStateOf("전체") }
    var price by rememberSaveable { mutableStateOf("전체") }
    var draftCategory by rememberSaveable { mutableStateOf("전체") }
    var draftPrice by rememberSaveable { mutableStateOf("전체") }
    // 기본은 "전체"(진행 중 + 예정). 예정 경매도 검색·카테고리에서는 보이기로 했고, 추천에서만 뺀다
    var status by rememberSaveable(closingSoonOnOpen) { mutableStateOf(if (closingSoonOnOpen) "진행 중" else "전체") }
    var sort by rememberSaveable(closingSoonOnOpen) { mutableStateOf(if (closingSoonOnOpen) "ENDING_SOON" else "LATEST") }
    val selectedPriceRange = PriceRanges.firstOrNull { it.label == price } ?: PriceRanges.first()
    val fallbackCategories = DefaultProductCategories
    val categories = (remoteCategories ?: fallbackCategories).sortedBy { categoryOrder(it.name) }
    fun filters(selectedCategory: String = category, selectedPrice: String = price): AuctionSearchFilters {
        val priceRange = PriceRanges.firstOrNull { it.label == selectedPrice } ?: PriceRanges.first()
        return AuctionSearchFilters(
            query = query.trim(),
            categoryId = categories.firstOrNull { it.name == selectedCategory }?.categoryId,
            minPrice = priceRange.minPriceParam,
            maxPrice = priceRange.maxPriceParam,
            // OPEN 은 서버 목록 API 의 "진행 중 + 예정" 값이다
            status = when (status) { "진행 중" -> "ACTIVE"; "예정" -> "SCHEDULED"; "종료" -> "ENDED"; else -> "OPEN" },
            sort = sort
        )
    }
    fun submit(selectedCategory: String = category, selectedPrice: String = price, recordQuery: Boolean = false) {
        submitted = true
        if (recordQuery && query.isNotBlank()) onAddRecentSearch(query.trim())
        onSearch(filters(selectedCategory, selectedPrice))
    }
    LaunchedEffect(browseOnOpen, closingSoonOnOpen) {
        if (browseOnOpen) submit()
    }
    val sourceAuctions = remoteAuctions ?: allHomeAuctions.distinctBy(HomeAuction::id).filter {
        (category == "전체" || category.removeSuffix("기기").split("·").any { token -> it.category.contains(token) || token.contains(it.category) }) &&
            selectedPriceRange.contains(it.price.toLong()) &&
            it.status.matchesAuctionStatusFilter(filters().status)
    }
    val results = when {
        !submitted -> emptyList()
        remoteAuctions != null -> sourceAuctions
        else -> sourceAuctions.filter {
            query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || query.contains("카메라") && it.id == "camera"
        }
    }

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { DiscoveryAppBar(if (closingSoonOnOpen) "마감 임박 경매" else if (browseOnOpen) "전체 경매" else "검색", onBack) },
        bottomBar = { DibBottomNavigation(DibMainTab.Home, onTabSelected) }
    ) { padding ->
        DibPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                // 홈·카테고리와 같은 검색 상자
                DibSearchField(
                    value = query,
                    onValueChange = { query = it; submitted = false },
                    onSearch = { submit(recordQuery = true) },
                    hint = "어떤 상품을 찾고 있나요?",
                    onClear = { query = ""; submitted = false }
                )
            }
            if (!submitted) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionTitle("최근 검색어"); if (recentSearches.isNotEmpty()) Text("전체 삭제", Modifier.clickable { onClearRecentSearches() }.padding(8.dp), color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
                if (recentSearches.isNotEmpty()) item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { recentSearches.forEach { word -> DiscoveryFilterChip(selected=false,onClick={query=word; submit(recordQuery = true)},label=word) } } }
                else item { Text("최근 검색어가 없어요", color = Colors.Muted, fontSize = 13.sp) }
                item { SectionTitle("인기 검색어") }
                // 서버가 검색 키워드를 누적한 상위 목록. 비어 있으면 아직 아무도 검색하지 않은 것
                when {
                    popularKeywords == null -> item { Text("인기 검색어를 불러오고 있어요", color = Colors.Muted, fontSize = 13.sp) }
                    popularKeywords.isEmpty() -> item { Text("아직 인기 검색어가 없어요. 첫 검색을 남겨보세요", color = Colors.Muted, fontSize = 13.sp) }
                    else -> items(popularKeywords.size) { index -> val word = popularKeywords[index]; Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable { query=word; submit(recordQuery = true) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("${index+1}", Modifier.width(32.dp), color = if(index<3) Colors.Live else Colors.Muted, fontWeight=FontWeight.Bold); Text(word,Modifier.weight(1f),color=Colors.Text,fontSize=14.sp,fontWeight=if(index<3)FontWeight.SemiBold else FontWeight.Normal);Image(painterResource(R.drawable.chevron_right),null,Modifier.size(16.dp),colorFilter=ColorFilter.tint(Colors.Muted)) } }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("전체", "진행 중", "예정", "종료").forEach { value ->
                            DiscoveryFilterChip(status == value, { if (status != value) { status = value; submit() } }, value)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscoveryFilterButton(activeCount = listOf(category, price).count { it != "전체" }) {
                            draftCategory = category
                            draftPrice = price
                            showFilters = true
                        }
                        Box(Modifier.weight(1f)) {
                            AuctionSortDropdown(sort, onSelect = { value ->
                                if (sort != value) { sort = value; submit() }
                            })
                        }
                        DibViewModeToggle(contentView, { contentView = it })
                    }
                }
                if (category != "전체" || price != "전체") item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (category != "전체") DiscoveryAppliedChip(categoryDisplayName(category)) { category = "전체"; submit() }
                        if (price != "전체") DiscoveryAppliedChip(price) { price = "전체"; submit() }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(if (browseOnOpen && query.isBlank()) {
                            if (status == "진행 중" && sort == "ENDING_SOON") "마감 임박 경매" else "전체 경매"
                        } else "검색 결과")
                        if (!isLoading && errorMessage == null) Text("현재 ${results.size}개 표시", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
                if (isLoading) item { LoadingContent("경매를 불러오고 있어요") }
                else if (errorMessage != null) item { NetworkErrorContent(onRetry) }
                else {
                    if(results.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(top=80.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text(if (browseOnOpen && query.isBlank()) "조건에 맞는 경매가 없어요" else "검색 결과가 없어요",fontSize=18.sp,fontWeight=FontWeight.Bold); Text("검색어나 필터를 바꿔보세요",Modifier.padding(top=8.dp),color=Colors.Muted,fontSize=12.sp) } }
                    if (contentView == DibContentView.Grid) {
                        items(results.chunked(2).size) { rowIndex -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){ results.chunked(2)[rowIndex].forEach { auction -> HomeAuctionCard(auction, favorite = false, onFavorite = null, onClick = { onProductClick(auction.id) }, modifier = Modifier.weight(1f), showStatus = true) }; if(results.chunked(2)[rowIndex].size==1) Spacer(Modifier.weight(1f)) } }
                    } else {
                        items(results.size) { index -> HomeAuctionListCard(results[index], favorite = false, onFavorite = null, onClick = { onProductClick(results[index].id) }, showStatus = true) }
                    }
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
        }
    }
    if(showFilters) ModalBottomSheet(onDismissRequest={showFilters=false},containerColor=Colors.Background){
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("검색 조건",fontSize=20.sp,fontWeight=FontWeight.Bold);IconButton(onClick={showFilters=false}){Image(painterResource(R.drawable.close),"닫기",Modifier.size(20.dp),colorFilter=ColorFilter.tint(Colors.Text))}}
            FilterGroup(
                "카테고리",
                listOf("전체") + categories.map { categoryDisplayName(it.name) },
                if (draftCategory == "전체") draftCategory else categoryDisplayName(draftCategory)
            ) { label ->
                draftCategory = categories.firstOrNull { categoryDisplayName(it.name) == label }?.name ?: "전체"
            }
            FilterGroup("가격 범위",PriceRanges.map(PriceRange::label),draftPrice){draftPrice=it}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("초기화",Modifier.width(88.dp).clickable{draftCategory="전체";draftPrice="전체"}.padding(vertical=14.dp),color=Colors.Muted,fontWeight=FontWeight.Bold);Button({category=draftCategory;price=draftPrice;showFilters=false;submit(draftCategory,draftPrice)},Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Colors.Navy)){Text("결과 보기",fontWeight=FontWeight.Bold)}}
        }
    }
}

@Composable private fun FilterGroup(title:String,values:List<String>,selected:String,onSelect:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=Colors.Text,fontSize=14.sp,fontWeight=FontWeight.Bold);FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){values.forEach{DiscoveryFilterChip(selected=selected==it,onClick={onSelect(it)},label=it)}}}}

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
    Scaffold(modifier.fillMaxSize().safeDrawingPadding(),containerColor=Colors.Canvas,contentWindowInsets=WindowInsets(0,0,0,0),topBar={DiscoveryAppBar("알림",onBack,"설정",onSettingsClick)},bottomBar={DibBottomNavigation(DibMainTab.Home,onTabSelected)}){padding->
        DibPullToRefreshBox(isRefreshing=isLoading,onRefresh=onRetry,modifier=Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=18.dp,end=18.dp,top=14.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("전체","라이브","찜","거래").forEach{DiscoveryFilterChip(filter==it,{filter=it},it)}}}
            if (notifications.any { !it.isRead }) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Text("읽지 않은 알림 ${notifications.count { !it.isRead }}개",color=Colors.Muted,fontSize=12.sp); TextButton(onClick = onMarkAllRead, enabled = actionNotificationId == null) { Text("모두 읽음", color = Colors.Navy,fontWeight=FontWeight.SemiBold) } } }
            }
            actionError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            if (connectionState == RealtimeConnectionState.Connecting || connectionState == RealtimeConnectionState.Reconnecting) {
                item { Row(Modifier.fillMaxWidth().background(Colors.NavySoft,RoundedCornerShape(12.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){CircularProgressIndicator(Modifier.size(16.dp),color=Colors.Navy,strokeWidth=2.dp);Text(if(connectionState == RealtimeConnectionState.Connecting) "실시간 알림에 연결하고 있어요" else "실시간 알림을 다시 연결하고 있어요", color=Colors.Muted, fontSize=12.sp) } }
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (item.isRead) Colors.Background else Colors.MintSoft)
                        .border(1.dp, if(item.isRead) Colors.Border else Colors.Mint.copy(alpha=.45f), RoundedCornerShape(16.dp))
                        .clickable { onMarkRead(item); if(actionable) onNotificationClick(item) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        val isLive = item.category.label == "라이브"
                        Box(Modifier.size(42.dp).background(if(isLive)Color(0xFFFFE8EA)else Colors.NavySoft,CircleShape),contentAlignment=Alignment.Center){Text(if(isLive)"LIVE" else item.category.label.take(1),color=if(isLive)Colors.Live else Colors.Navy,fontSize=if(isLive)9.sp else 15.sp,fontWeight=FontWeight.Bold)}
                        Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(item.category.label,color=if(isLive)Colors.Live else Colors.MintInk,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            Text(item.title,color=Colors.Text,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold)
                            Text(item.body,color=Colors.Muted,fontSize=12.sp, lineHeight = 18.sp)
                        }
                        Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(notificationTimeLabel(item.occurredAt),color=Colors.Muted,fontSize=10.sp)
                            if (!item.isRead) Box(Modifier.size(7.dp).background(Colors.MintInk,CircleShape))
                            else if(actionable) Image(painterResource(R.drawable.chevron_right),null,Modifier.size(16.dp),colorFilter=ColorFilter.tint(Colors.Muted))
                        }
                    }
                    if (item.isRunnerUpOffer) {
                        Button(
                            onClick = { offerToConfirm = item },
                            enabled = actionNotificationId == null,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
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
    }
    offerToConfirm?.let { offer ->
        DibDialog(
            onDismissRequest = { offerToConfirm = null },
            title = "차순위 구매 제안을 수락할까요?",
            text = { Text("수락하면 주문이 생성되고 등록 카드로 즉시 자동결제를 요청합니다. 제안은 알림 생성 후 24시간 동안 유효해요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = { DibDialogConfirmButton("수락하고 결제", { offerToConfirm = null; onMarkRead(offer); onAcceptOffer(offer) }) },
            dismissButton = { DibDialogDismissButton({ offerToConfirm = null }, label = "나중에") }
        )
    }
}

@Composable private fun LoadingContent(label: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { CircularProgressIndicator(color = Colors.Navy); Text(label, color = Colors.Muted, fontSize = 13.sp) } }
@Composable private fun EmptyContent(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(54.dp).background(Colors.NavySoft,CircleShape),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.notification_vector),null,Modifier.size(24.dp),colorFilter=ColorFilter.tint(Colors.Navy))};Text(title, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 12.sp) } }
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

@Composable private fun DiscoveryAppBar(title:String,onBack:()->Unit,action:String="",onActionClick:(()->Unit)?=null){
    DibSubAppBar(title, onBack, actions = {
        if (action.isNotBlank()) Text(action, Modifier.clickable(enabled = onActionClick != null) { onActionClick?.invoke() }.padding(horizontal = 12.dp, vertical = 14.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    })
}

@Composable private fun SectionTitle(text:String){Text(text,color=Colors.Text,fontSize=18.sp,fontWeight=FontWeight.Bold)}

@Composable private fun DiscoveryFilterChip(selected:Boolean,onClick:()->Unit,label:String){FilterChip(selected=selected,onClick=onClick,label={Text(label,fontSize=12.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)},shape=RoundedCornerShape(12.dp),border=FilterChipDefaults.filterChipBorder(enabled=true,selected=selected,borderColor=Colors.Border,selectedBorderColor=Colors.Navy),colors=FilterChipDefaults.filterChipColors(containerColor=Colors.Background,labelColor=Colors.Muted,selectedContainerColor=Colors.Navy,selectedLabelColor=Color.White))}

@Composable private fun DiscoveryAppliedChip(label: String, onRemove: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        trailingIcon = { Image(painterResource(R.drawable.close), "$label 조건 해제", Modifier.size(12.dp), colorFilter = ColorFilter.tint(Color.White)) },
        shape = RoundedCornerShape(12.dp),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = true, borderColor = Colors.Border, selectedBorderColor = Colors.Navy),
        colors = FilterChipDefaults.filterChipColors(containerColor = Colors.Background, labelColor = Colors.Muted, selectedContainerColor = Colors.Navy, selectedLabelColor = Color.White)
    )
}

@Composable private fun AuctionSortDropdown(sort: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = true },
        color = Colors.Surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(auctionSortOptions.firstOrNull { it.first == sort }?.second ?: "최신순", color = Colors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("⌄", color = Colors.Muted, fontSize = 16.sp)
        }
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = Colors.Background,
        tonalElevation = 0.dp
    ) {
        auctionSortOptions.forEach { (value, label) ->
            DropdownMenuItem(
                text = { Text(label, color = Colors.Text, fontSize = 13.sp) },
                onClick = { expanded = false; onSelect(value) },
                trailingIcon = { if (sort == value) Text("✓", color = Colors.Navy) },
                modifier = Modifier.background(if (sort == value) Colors.NavySoft else Colors.Background)
            )
        }
    }
}

@Composable private fun DiscoveryFilterButton(activeCount: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (activeCount > 0) Colors.NavySoft else Colors.Surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(
                painterResource(R.drawable.filter_list),
                contentDescription = "필터 설정",
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(if (activeCount > 0) Colors.Navy else Colors.Muted)
            )
            Text(if (activeCount > 0) "필터 $activeCount" else "필터", color = if (activeCount > 0) Colors.Navy else Colors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// 서버 알림 문구에는 "주문 #12 결제가 완료되었습니다"처럼 내부 번호가 들어 있다. 사용자에게 번호는 의미가 없다
private val ORDER_NUMBER_IN_TEXT = Regex("""주문 #(\d+)""")
private val OTHER_NUMBER_IN_TEXT = Regex("""(문의|신고|상품|경매) #\d+""")

/** 알림 문구 속 "주문 #번호"가 가리키는 주문 번호들. 상품명을 미리 찾아 둘 때 쓴다 */
internal fun orderIdsInNotificationText(text: String): List<String> =
    ORDER_NUMBER_IN_TEXT.findAll(text).map { it.groupValues[1] }.toList()

/**
 * 알림 문구의 내부 번호를 사람이 읽는 말로 바꾼다.
 * "주문 #12" 는 상품명을 알면 "‘상품명’", 모르면 "주문" 으로, "문의 #3" 같은 나머지는 번호만 뗀다.
 * 뒤에 "(주문 #12)" 처럼 괄호로 붙은 참조는 상품명이 이미 앞에 있으므로 통째로 지운다
 */
internal fun humanizeNotificationText(text: String, orderTitles: Map<String, String>): String =
    text.replace(Regex("""\s*\(주문 #\d+\)"""), "")
        .replace(ORDER_NUMBER_IN_TEXT) { match ->
            orderTitles[match.groupValues[1]]?.takeIf(String::isNotBlank)?.let { "‘$it’" } ?: "주문"
        }
        .replace(OTHER_NUMBER_IN_TEXT) { match -> match.groupValues[1] }
        // 서버 문구의 금액은 쉼표 없이 온다(87000원). 앱의 다른 금액 표기와 맞춘다
        .replace(Regex("""(\d{4,})원""")) { match -> match.groupValues[1].toLongOrNull()?.let { "%,d원".format(it) } ?: match.value }
