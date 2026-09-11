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
import com.ssafy.dib.ui.theme.WireframeColors as Colors

data class AuctionSearchFilters(
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
    onSearch: (AuctionSearchFilters) -> Unit,
    onRetry: () -> Unit,
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
    val results = if (!submitted) emptyList() else sourceAuctions.filter {
        val keywordMatch = query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || query.contains("카메라") && it.id == "camera"
        keywordMatch
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

@Composable private fun SearchAuctionCard(auction:HomeAuction,onClick:()->Unit,modifier:Modifier=Modifier){val statusLabel=when(auction.status){"SCHEDULED"->"예정";"ENDED"->"종료";else->"진행중"};Column(modifier.clickable(onClick=onClick),verticalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.fillMaxWidth().height(122.dp)){ProductPhoto(auction.photo, auction.imageUrls.firstOrNull(), Modifier.fillMaxSize());Surface(Modifier.padding(8.dp),color=Colors.Navy,shape=RoundedCornerShape(12.dp)){Text(statusLabel,Modifier.padding(horizontal=8.dp,vertical=5.dp),color=androidx.compose.ui.graphics.Color.White,fontSize=9.sp)}};Text(auction.name,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${auction.pricePrefix} ${auction.priceLabel}",color=Colors.Navy,fontSize=13.sp,fontWeight=FontWeight.Bold);Text("입찰 ${auction.bidCount}명",color=Colors.Muted,fontSize=9.sp)}}
@Composable private fun FilterGroup(title:String,values:List<String>,selected:String,onSelect:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontSize=13.sp,fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){values.forEach{FilterChip(selected=selected==it,onClick={onSelect(it)},label={Text(it,fontSize=12.sp)})}}}}

@Composable
fun NotificationCenterScreen(onBack:()->Unit,onTabSelected:(DibMainTab)->Unit,modifier:Modifier=Modifier){
    var filter by rememberSaveable{mutableStateOf("전체")}
    var loading by rememberSaveable { mutableStateOf(true) }
    var loadRevision by rememberSaveable { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val online = remember(loadRevision) { isNetworkAvailable(context) }
    LaunchedEffect(loadRevision) { loading = true; kotlinx.coroutines.delay(450); loading = false }
    val all=listOf(Triple("팔로잉","하루공방 라이브가 곧 시작해요","10분 전"),Triple("LIVE","빈티지룸이 지금 경매 중이에요","방금"),Triple("찜","찜한 상품이 곧 마감돼요","2분 전"))
    val shown=if(filter=="전체") all else all.filter{it.first==filter}
    Scaffold(modifier.fillMaxSize().safeDrawingPadding(),containerColor=androidx.compose.ui.graphics.Color.White,contentWindowInsets=WindowInsets(0,0,0,0),topBar={SimpleAppBar("알림",onBack,"설정")},bottomBar={DibBottomNavigation(DibMainTab.Home,onTabSelected)}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if (loading) {
                item { LoadingContent("알림을 불러오고 있어요") }
            } else if (!online) {
                item { NetworkErrorContent { loadRevision++ } }
            } else {
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("전체","팔로잉","찜","거래").forEach{FilterChip(filter==it,{filter=it},{Text(it)})}}}
                if (shown.isEmpty()) item { EmptyContent("새로운 알림이 없어요", "경매 상태가 바뀌면 여기서 알려드릴게요") }
                items(shown.size){index->val item=shown[index];Row(Modifier.fillMaxWidth().height(92.dp).border(1.dp,Colors.Border,RoundedCornerShape(14.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(if(item.first=="LIVE")androidx.compose.ui.graphics.Color(0xFFFFE4E9)else androidx.compose.ui.graphics.Color(0xFFE8FAF5),CircleShape),contentAlignment=Alignment.Center){Text(if(item.first=="LIVE")"●" else "d",color=if(item.first=="LIVE")androidx.compose.ui.graphics.Color(0xFFEF596B)else Colors.Navy,fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f).padding(start=12.dp)){Text(item.first,color=androidx.compose.ui.graphics.Color(0xFFEF596B),fontSize=9.sp,fontWeight=FontWeight.Bold);Text(item.second,color=Colors.Navy,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(if(index==0)"오늘 오후 3:00 · 선반 5개" else "빈티지 필름 카메라",color=Colors.Muted,fontSize=10.sp)};Text(item.third,color=Colors.Muted,fontSize=9.sp)}}
            }
        }
    }
}

@Composable private fun LoadingContent(label: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { CircularProgressIndicator(color = Colors.Navy); Text(label, color = Colors.Muted, fontSize = 13.sp) } }
@Composable private fun EmptyContent(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 12.sp) } }
@Composable private fun NetworkErrorContent(onRetry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("연결이 원활하지 않아요", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("네트워크를 확인하고 다시 시도해주세요", color = Colors.Muted, fontSize = 12.sp); Button(onRetry, colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) { Text("다시 시도") } } }
private fun isNetworkAvailable(context: android.content.Context): Boolean { val manager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager; val network = manager.activeNetwork ?: return false; return manager.getNetworkCapabilities(network)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true }

@Composable private fun SimpleAppBar(title:String,onBack:()->Unit,action:String=""){Row(Modifier.fillMaxWidth().height(48.dp).background(androidx.compose.ui.graphics.Color.White),verticalAlignment=Alignment.CenterVertically){Text("←",Modifier.size(48.dp).clickable(onClick=onBack).wrapContentSize(),fontSize=24.sp);Text(title,Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.Bold);if(action.isNotBlank())Text(action,Modifier.padding(end=16.dp),color=Colors.Muted,fontSize=12.sp)}}
