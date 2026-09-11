package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private enum class TradeTab(val label: String) { Bid("입찰"), Purchase("구매"), Sale("판매") }
private enum class TradeTone { Urgent, Positive, Neutral }
private data class TradeItem(val status: String, val title: String, val meta: String, val action: String, val tone: TradeTone)

@Composable
fun MyTradesScreen(
    onTabSelected: (DibMainTab) -> Unit,
    onProductClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf(TradeTab.Bid) }
    val items = when (selected) {
        TradeTab.Bid -> listOf(
            TradeItem("다른 입찰 발생", "빈티지 필름 카메라", "현재가 35,000원 · 마감 00:42", "500원 높여 입찰하기 →", TradeTone.Urgent),
            TradeItem("최고 입찰자", "빈티지 스니커즈", "내 입찰가 58,000원 · 마감 12분", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("경매 종료", "레더 숄더백", "최종가 72,000원 · 미낙찰", "결과 확인하기 →", TradeTone.Neutral)
        )
        TradeTab.Purchase -> listOf(
            TradeItem("결제 필요", "빈티지 필름 카메라", "낙찰가 35,000원 · 23:42:18 남음", "거래 진행하기 →", TradeTone.Urgent),
            TradeItem("배송 중", "노이즈 캔슬링 헤드폰", "판매자가 상품을 발송했어요", "배송 조회하기 →", TradeTone.Positive),
            TradeItem("구매 완료", "레더 카드지갑", "거래가 안전하게 완료됐어요", "거래 내역 보기 →", TradeTone.Neutral)
        )
        TradeTab.Sale -> listOf(
            TradeItem("경매 진행 중", "빈티지 스니커즈", "현재가 58,000원 · 입찰 12명", "경매 상태 보기 →", TradeTone.Positive),
            TradeItem("발송 필요", "빈티지 필름 카메라", "구매자 결제 완료 · 1일 남음", "배송 정보 입력하기 →", TradeTone.Urgent),
            TradeItem("판매 완료", "원목 라운지 체어", "구매 확정 · 정산 예정", "거래 내역 보기 →", TradeTone.Neutral)
        )
    }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F8FA), contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            Column(Modifier.background(Color.White)) {
                Text("내 거래", Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp, vertical = 13.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(45.dp).padding(horizontal = 16.dp)) {
                    TradeTab.entries.forEach { tab ->
                        Column(Modifier.weight(1f).fillMaxHeight().clickable { selected = tab }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(tab.label, color = if (tab == selected) Colors.Navy else Colors.Muted, fontSize = 14.sp, fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.height(9.dp))
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (tab == selected) Colors.Navy else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFE7E9ED))
            }
        },
        bottomBar = { DibBottomNavigation(DibMainTab.Trades, onTabSelected) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${selected.label} 현황", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("${items.size}건", color = Colors.Muted, fontSize = 11.sp) } }
            items(items.size) { index ->
                TradeCard(items[index]) {
                    if (selected == TradeTab.Bid) {
                        onProductClick(if (items[index].status == "경매 종료") "lost" else if (items[index].title.contains("카메라")) "camera" else "sneakers")
                    } else {
                        onTransactionClick(if (selected == TradeTab.Sale) "seller" else "buyer")
                    }
                }
            }
        }
    }
}

@Composable private fun TradeCard(item: TradeItem, onClick: () -> Unit) {
    val chip = when(item.tone){ TradeTone.Urgent -> Color(0xFFFFF0EA); TradeTone.Positive -> Color(0xFFE8FAF5); TradeTone.Neutral -> Color(0xFFF1F3F5) }
    val ink = when(item.tone){ TradeTone.Urgent -> Color(0xFFE56F49); TradeTone.Positive -> Color(0xFF27806E); TradeTone.Neutral -> Color(0xFF6B7280) }
    Row(Modifier.fillMaxWidth().height(120.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE1E5EA), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(88.dp).background(Color(0xFFECECEC), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("상품 이미지", color = Color(0xFF858B94), fontSize = 10.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(color = chip, shape = RoundedCornerShape(12.dp)) { Text(item.status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(item.meta, color = Color(0xFF6B7280), fontSize = 11.sp)
            Text(item.action, color = if(item.tone == TradeTone.Urgent) ink else Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MyPageScreen(
    onTabSelected: (DibMainTab) -> Unit,
    onProfileEditClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onRegisteredProductsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onInquiriesClick: () -> Unit,
    onAddressesClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onWithdrawalClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F8FA), contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Text("마이", Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 16.dp, vertical = 13.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE1E5EA), RoundedCornerShape(14.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(Color(0xFFDDF8F0), CircleShape), contentAlignment = Alignment.Center) { Text("d", color = Colors.Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) { Text("dib러버", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("@dib_user", color = Colors.Muted, fontSize = 12.sp); Text("팔로워 128 · 팔로잉 24", color = Color(0xFF27806E), fontSize = 11.sp) }
                        OutlinedButton(onProfileEditClick, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("프로필 수정", fontSize = 11.sp) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Colors.Border)
                    Text("★ 4.8  ·  거래 32회  ·  응답 빠름", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Text("바로가기", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("♡" to "찜한 상품", "▣" to "등록 상품", "♧" to "알림", "◉" to "문의 내역").forEach { (icon,label) ->
                        Column(Modifier.width(76.dp).clickable { when(label) { "찜한 상품" -> onFavoritesClick(); "등록 상품" -> onRegisteredProductsClick(); "알림" -> onNotificationsClick(); else -> onInquiriesClick() } }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(40.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text(icon, color = Colors.Navy, fontSize = 22.sp) }; Text(label, Modifier.padding(top = 5.dp), color = Colors.Muted, fontSize = 11.sp) }
                    }
                }
            }
            item { Text("내 정보 · 설정", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFE1E5EA), RoundedCornerShape(14.dp))) {
                    MenuRow("배송지 관리", onClick = onAddressesClick)
                    MenuRow("정산 계좌 관리", onClick = onAccountsClick)
                    MenuRow("알림 설정", onClick = onNotificationSettingsClick)
                    MenuRow("신고 내역", onClick = onReportsClick)
                    MenuRow("회원 탈퇴", onClick = onWithdrawalClick)
                    MenuRow("로그아웃", Color(0xFFEF596B)) { confirmation = "로그아웃" }
                }
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("$action 할까요?") },
            text = { Text("현재 계정에서 로그아웃하고 시작 화면으로 이동해요.") },
            confirmButton = { TextButton({ confirmation = null; onLogout() }) { Text("로그아웃") } },
            dismissButton = { TextButton({ confirmation = null }) { Text("취소") } }
        )
    }
}

@Composable private fun MenuRow(label: String, color: Color = Colors.Text, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = color, fontSize = 14.sp); Text("›", color = Colors.Muted, fontSize = 20.sp) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductRegisterScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var photoAdded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var startPrice by rememberSaveable { mutableStateOf("") }
    val firstValid = photoAdded && name.isNotBlank() && category.isNotBlank() && condition.isNotBlank() && description.isNotBlank()
    val canNext = when(step){ 1 -> firstValid; 2 -> (startPrice.toIntOrNull() ?: 0) >= 1_000; else -> true }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Surface, contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(48.dp).clickable { if(step > 1) step-- else onBack() }.wrapContentSize(), fontSize = 24.sp); Text("상품 등록", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(when(step){1->"상품 정보";2->"경매 설정";else->"등록 확인"}, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("$step / 3", color = Colors.Muted, fontSize = 12.sp) }; LinearProgressIndicator({ step / 3f }, Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp), color = Colors.Navy, trackColor = Colors.Border) }
            when(step) {
                1 -> {
                    item { Text("상품 사진 *  1~10장 · 첫 사진이 대표", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    item { Box(Modifier.fillMaxWidth().height(112.dp).background(Colors.Surface, RoundedCornerShape(12.dp)).border(1.dp, Colors.Border, RoundedCornerShape(12.dp)).clickable { photoAdded = !photoAdded }, contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if(photoAdded) "✓" else "+", color = Colors.Navy, fontSize = 30.sp); Text(if(photoAdded) "대표 사진 1장 추가됨" else "사진 추가", fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if(photoAdded) "눌러서 제거" else "첫 사진이 대표 이미지 · 최대 10장", color = Colors.Muted, fontSize = 11.sp) } } }
                    item { RegisterTextField("상품명 *", name, { name = it }, "입력해주세요") }
                    item { RegisterSelect("카테고리 *", category.ifBlank { "선택해주세요" }) { category = if(category == "디지털") "라이프" else "디지털" } }
                    item { RegisterSelect("상품 상태 *", condition.ifBlank { "상 · 중 · 하" }) { condition = when(condition){"상"->"중";"중"->"하";else->"상"} } }
                    item { RegisterTextField("상품 설명 *", description, { description = it }, "상품의 특징과 하자를 자세히 적어주세요", 100.dp) }
                }
                2 -> {
                    item { Text("일반 경매 설정", color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    item { RegisterTextField("시작가 *", startPrice, { startPrice = it.filter(Char::isDigit) }, "1,000원 이상", keyboardType = KeyboardType.Number) }
                    item { RegisterSelect("경매 시간 *", "2시간") {} }
                    item { RegisterSelect("배송 방식 *", "안전배송 · 배송비 포함") {} }
                    item { Text("판매자 보증금은 등록 완료 전에 안내돼요.", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(16.dp), color = Color(0xFF27806E), fontSize = 12.sp) }
                }
                else -> {
                    item { Text("등록 내용을 확인해주세요", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    item { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("$category · 상태 $condition", color = Colors.Muted); Text("시작가 ${"%,d".format(startPrice.toIntOrNull() ?: 0)}원", color = Colors.Navy, fontWeight = FontWeight.Bold); Text(description, color = Colors.Muted, fontSize = 12.sp) } }
                    item { Text("AI 상품 검수 요청 후 승인되면 경매를 시작할 수 있어요.", Modifier.fillMaxWidth().background(Color(0xFFFFF0EA), RoundedCornerShape(12.dp)).padding(16.dp), color = Color(0xFFE56F49), fontSize = 12.sp) }
                }
            }
            item { Button(onClick = { if(step < 3) step++ else onBack() }, enabled = canNext, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if(step < 3) "다음" else "AI 검수 요청", fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable private fun RegisterTextField(label:String,value:String,onChange:(String)->Unit,placeholder:String,height: androidx.compose.ui.unit.Dp = 72.dp,keyboardType: KeyboardType = KeyboardType.Text){ Column(Modifier.fillMaxWidth().height(height), verticalArrangement=Arrangement.spacedBy(6.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold); OutlinedTextField(value,onChange,Modifier.fillMaxWidth().weight(1f),placeholder={Text(placeholder,color=Color(0xFF8A9099),fontSize=13.sp)},singleLine=height<90.dp,keyboardOptions=KeyboardOptions(keyboardType=keyboardType),shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Colors.Navy,unfocusedBorderColor=Color(0xFFDDE1E7)))} }
@Composable private fun RegisterSelect(label:String,value:String,onClick:()->Unit){Column(Modifier.fillMaxWidth().height(72.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth().weight(1f).background(Color.White,RoundedCornerShape(12.dp)).border(1.dp,Color(0xFFDDE1E7),RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text(value,Modifier.weight(1f),color=if(value.contains("선택")||value.contains("상 ·"))Color(0xFF8A9099)else Colors.Text,fontSize=14.sp);Text("›",color=Colors.Muted,fontSize=22.sp)}}}
