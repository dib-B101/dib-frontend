package com.ssafy.dib.feature.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.domain.settlement.SettlementAccount
import com.ssafy.dib.domain.member.MemberAddress
import com.ssafy.dib.domain.member.NewAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressManagementScreen(
    addresses: List<MemberAddress>?,
    isLoading: Boolean,
    errorMessage: String?,
    actionLoading: Boolean,
    actionError: String?,
    actionMessage: String?,
    onRetry: () -> Unit,
    onUpdate: (MemberAddress) -> Unit,
    onCreate: (NewAddress) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAddress by remember { mutableStateOf<MemberAddress?>(null) }
    var deletingAddress by remember { mutableStateOf<MemberAddress?>(null) }
    var addingAddress by remember { mutableStateOf(false) }
    SettingsScaffold("배송지 관리", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            actionMessage?.let { message -> item { Text("완료 · $message", Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(14.dp)).padding(14.dp), color = Colors.MintInk, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            actionError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(14.dp)).padding(14.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            when {
                isLoading && addresses == null -> item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null && addresses == null -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onRetry, Modifier.padding(top = 10.dp)) { Text("다시 불러오기") } } }
                addresses.isNullOrEmpty() -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("등록한 배송지가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("아래 버튼으로 새 배송지를 등록해보세요.", Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 12.sp) } }
            }
            items(addresses?.size ?: 0) { index ->
                val address = addresses.orEmpty()[index]
                Column(
                    Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp))
                        .clickable(enabled = !actionLoading) { editingAddress = address }.padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("배송지", Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("수정", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(address.name, color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (address.postalCode.isNotBlank()) Text("우편번호 ${address.postalCode}", color = Colors.Muted, fontSize = 11.sp)
                    Text(address.address, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            item { OutlinedButton({ addingAddress = true }, Modifier.fillMaxWidth().height(48.dp), enabled = !actionLoading, shape = RoundedCornerShape(12.dp)) { Text("새 배송지 추가", fontWeight = FontWeight.Bold) } }
            }
        }
    }
    if (addingAddress) {
        AddressCreator(
            actionLoading = actionLoading,
            onDismiss = { if (!actionLoading) addingAddress = false },
            onSave = { value -> addingAddress = false; onCreate(value) }
        )
    }
    editingAddress?.let { selected ->
        AddressEditor(
            initial = selected,
            actionLoading = actionLoading,
            onDismiss = { if (!actionLoading) editingAddress = null },
            onSave = { value -> onUpdate(value); editingAddress = null },
            onDelete = { editingAddress = null; deletingAddress = selected }
        )
    }
    deletingAddress?.let { selected ->
        AlertDialog(onDismissRequest = { if (!actionLoading) deletingAddress = null }, title = { Text("배송지를 삭제할까요?") }, text = { Text("‘${selected.name}’ 배송지를 삭제하면 주문 시 선택할 수 없습니다.") }, confirmButton = { TextButton({ deletingAddress = null; onDelete(selected.addressId) }, enabled = !actionLoading) { Text("삭제", color = Color(0xFFEF596B)) } }, dismissButton = { TextButton({ deletingAddress = null }, enabled = !actionLoading) { Text("취소") } })
    }
}

// 카카오(다음) 우편번호 서비스에서 고른 주소로 회원 배송지를 새로 만든다.
// 서버가 apiAddressId 를 필수로 받으므로 직접 입력이 아니라 반드시 검색을 거쳐야 한다
@Composable private fun AddressCreator(actionLoading: Boolean, onDismiss: () -> Unit, onSave: (NewAddress) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var apiAddressId by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    if (searching) {
        PostcodeSearchDialog(
            onSelected = { selectedZip, selectedAddress, buildingCode ->
                postalCode = selectedZip
                address = selectedAddress
                apiAddressId = buildingCode.ifBlank { selectedAddress }
                searching = false
            },
            onDismiss = { searching = false }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 배송지") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ searching = true }, Modifier.fillMaxWidth().height(48.dp), enabled = !actionLoading, shape = RoundedCornerShape(12.dp)) {
                Text(if (postalCode.isBlank()) "우편번호 검색" else "우편번호 다시 찾기", fontWeight = FontWeight.Bold)
            }
            if (postalCode.isNotBlank()) Text("($postalCode) $address", color = Colors.Text, fontSize = 13.sp, lineHeight = 19.sp)
            OutlinedTextField(detail, { detail = it }, modifier = Modifier.fillMaxWidth(), label = { Text("상세주소 (동/호수)") }, singleLine = true, colors = dialogFieldColors())
            OutlinedTextField(label, { label = it }, modifier = Modifier.fillMaxWidth(), label = { Text("배송지 이름 (집, 회사 …)") }, singleLine = true, colors = dialogFieldColors())
        } },
        confirmButton = {
            TextButton({
                onSave(
                    NewAddress(
                        postalCode = postalCode,
                        address = listOf(address, detail.trim()).filter(String::isNotBlank).joinToString(" "),
                        name = label.trim(),
                        apiAddressId = apiAddressId
                    )
                )
            }, enabled = label.isNotBlank() && address.isNotBlank() && apiAddressId.isNotBlank() && !actionLoading) { Text("등록") }
        },
        dismissButton = { TextButton(onDismiss, enabled = !actionLoading) { Text("취소") } }
    )
}

@Composable private fun AddressEditor(initial: MemberAddress, actionLoading: Boolean, onDismiss: () -> Unit, onSave: (MemberAddress) -> Unit, onDelete: () -> Unit) {
    var label by rememberSaveable(initial.addressId) { mutableStateOf(initial.name) }
    var postalCode by rememberSaveable(initial.addressId) { mutableStateOf(initial.postalCode) }
    var address by rememberSaveable(initial.addressId) { mutableStateOf(initial.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("배송지 수정") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(label, { label = it }, modifier = Modifier.fillMaxWidth(), label = { Text("배송지 이름") }, singleLine = true, colors = dialogFieldColors())
            OutlinedTextField(postalCode, { postalCode = it.filter(Char::isDigit).take(10) }, modifier = Modifier.fillMaxWidth(), label = { Text("우편번호") }, singleLine = true, colors = dialogFieldColors())
            OutlinedTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("주소") }, colors = dialogFieldColors())
            Text("배송지 삭제", Modifier.clickable(enabled = !actionLoading, onClick = onDelete).padding(vertical = 8.dp), color = Color(0xFFEF596B), fontWeight = FontWeight.Bold)
        } },
        confirmButton = { TextButton({ onSave(initial.copy(name = label.trim(), postalCode = postalCode, address = address.trim())) }, enabled = label.isNotBlank() && address.isNotBlank() && !actionLoading) { Text("저장") } },
        dismissButton = { TextButton(onDismiss, enabled = !actionLoading) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementAccountsScreen(
    account: SettlementAccount?,
    isLoading: Boolean,
    errorMessage: String?,
    actionLoading: Boolean,
    actionError: String?,
    actionRevision: Int,
    onRetry: () -> Unit,
    onSave: (bankName: String, accountNumber: String, accountHolder: String) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(actionRevision) { if (actionRevision > 0) editorOpen = false }
    SettingsScaffold("정산 계좌 관리", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                isLoading -> item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null && account == null -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onRetry, Modifier.padding(top = 10.dp)) { Text("다시 불러오기") } } }
                account != null -> item {
                    Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){Box(Modifier.background(Colors.NavySoft,RoundedCornerShape(9.dp)).padding(horizontal=8.dp,vertical=5.dp)){Text("정산",color=Colors.Navy,fontSize=10.sp,fontWeight=FontWeight.Bold)};Text(account.bankName, color = Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)}
                        Text(account.maskedAccountNumber, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("예금주 ${account.accountHolder} · 확인 완료", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
                else -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("등록한 정산 계좌가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("판매 대금을 받을 본인 계좌를 등록해주세요", Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 12.sp) } }
            }
            item { Text("안전 확인 · 계좌 추가·변경 시 예금주 일치 여부를 확인해요", Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(14.dp)).padding(16.dp), color = Colors.MintInk, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            item { Button({ editorOpen = true }, Modifier.fillMaxWidth().height(48.dp), enabled = !isLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if (account == null) "정산 계좌 등록" else "정산 계좌 변경", fontWeight = FontWeight.Bold) } }
            }
        }
    }
    if (editorOpen) {
        var bank by rememberSaveable { mutableStateOf(account?.bankName.orEmpty()) }
        var number by remember { mutableStateOf("") }
        var holder by rememberSaveable { mutableStateOf(account?.accountHolder.orEmpty()) }
        AlertDialog(
            onDismissRequest = { if (!actionLoading) editorOpen = false }, title = { Text(if (account == null) "정산 계좌 등록" else "정산 계좌 변경") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bank, { bank = it.take(30) }, modifier = Modifier.fillMaxWidth(), label = { Text("은행") }, singleLine = true, colors = dialogFieldColors())
                OutlinedTextField(number, { number = it.filter(Char::isDigit).take(24) }, modifier = Modifier.fillMaxWidth(), label = { Text("계좌번호") }, singleLine = true, colors = dialogFieldColors())
                OutlinedTextField(holder, { holder = it.take(30) }, modifier = Modifier.fillMaxWidth(), label = { Text("예금주") }, singleLine = true, colors = dialogFieldColors())
                // 휴대전화 재인증은 받지 않는다. 가입 때 본인인증을 마친 계정이다
                actionError?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
            } },
            confirmButton = { TextButton({ onSave(bank.trim(), number, holder.trim()) }, enabled = bank.isNotBlank() && number.length >= 8 && holder.isNotBlank() && !actionLoading) { if (actionLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("저장") } },
            dismissButton = { TextButton({ editorOpen = false }, enabled = !actionLoading) { Text("취소") } }
        )
    }
}

@Composable
fun NotificationSettingsScreen(
    tradeEnabled: Boolean,
    liveEnabled: Boolean,
    wishlistEnabled: Boolean,
    onTradeEnabledChange: (Boolean) -> Unit,
    onLiveEnabledChange: (Boolean) -> Unit,
    onWishlistEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SettingsScaffold("알림 설정", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Column(Modifier.fillMaxWidth().background(Colors.NavySoft, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("알림은 휴대전화 설정에서 관리해요", color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("허용 여부와 소리·진동은 Android 시스템 설정에서 변경할 수 있어요.", color = Colors.Muted, fontSize = 12.sp) } }
            item { Text("dib에서 보내는 알림", color = Colors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            item { Column(Modifier.fillMaxWidth().background(Colors.Background, RoundedCornerShape(16.dp))) {
                NotificationToggle("입찰·거래 상태", "상회 입찰, 낙찰, 결제와 배송 상태", tradeEnabled, onTradeEnabledChange)
                NotificationToggle("Live 방송", "예약 Live 시작과 방송 상태 알림", liveEnabled, onLiveEnabledChange)
                NotificationToggle("찜한 경매", "찜한 경매의 시작·마감 임박 알림", wishlistEnabled, onWishlistEnabledChange)
            } }
            item { Text("필수 거래 알림은 안전한 경매 진행을 위해 전송될 수 있어요.", color = Colors.Muted, fontSize = 12.sp) }
            item { Button({ context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("시스템 알림 설정 열기", fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable private fun NotificationToggle(title: String, body: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 11.sp) }
        Switch(checked, onChecked, colors = SwitchDefaults.colors(checkedTrackColor = Colors.MintInk))
    }
}

@Composable internal fun SettingsScaffold(title: String, onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Column(Modifier.background(Colors.Background)){Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Image(painterResource(R.drawable.back),"뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))};Text(title,color=Colors.Text,fontSize=17.sp,fontWeight=FontWeight.Bold)};HorizontalDivider(color=Colors.Border)} },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) },
        content = content
    )
}
