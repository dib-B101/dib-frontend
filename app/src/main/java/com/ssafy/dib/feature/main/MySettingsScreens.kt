package com.ssafy.dib.feature.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import com.ssafy.dib.domain.settlement.SettlementAccount
import com.ssafy.dib.domain.member.MemberAddress

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
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAddress by remember { mutableStateOf<MemberAddress?>(null) }
    var deletingAddress by remember { mutableStateOf<MemberAddress?>(null) }
    SettingsScaffold("배송지 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actionMessage?.let { message -> item { Text("✓ $message", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(14.dp), color = Color(0xFF27806E), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            actionError?.let { message -> item { Text(message, Modifier.fillMaxWidth().background(Color(0xFFFFEEF0), RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Urgent, fontSize = 12.sp) } }
            when {
                isLoading && addresses == null -> item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null && addresses == null -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onRetry, Modifier.padding(top = 10.dp)) { Text("다시 불러오기") } } }
                addresses.isNullOrEmpty() -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("등록한 배송지가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("주소 검색 연동 후 새 배송지를 등록할 수 있어요.", Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 12.sp) } }
            }
            items(addresses?.size ?: 0) { index ->
                val address = addresses.orEmpty()[index]
                Column(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp))
                        .clickable(enabled = !actionLoading) { editingAddress = address }.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("배송지", Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("수정", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(address.name, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (address.postalCode.isNotBlank()) Text("우편번호 ${address.postalCode}", color = Colors.Muted, fontSize = 11.sp)
                    Text(address.address, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            item { OutlinedButton({}, Modifier.fillMaxWidth().height(48.dp), enabled = false, shape = RoundedCornerShape(12.dp)) { Text("주소 검색 API 연결 후 새 배송지 추가", fontWeight = FontWeight.Bold) } }
        }
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

@Composable private fun AddressEditor(initial: MemberAddress, actionLoading: Boolean, onDismiss: () -> Unit, onSave: (MemberAddress) -> Unit, onDelete: () -> Unit) {
    var label by rememberSaveable(initial.addressId) { mutableStateOf(initial.name) }
    var postalCode by rememberSaveable(initial.addressId) { mutableStateOf(initial.postalCode) }
    var address by rememberSaveable(initial.addressId) { mutableStateOf(initial.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("배송지 수정") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(label, { label = it }, label = { Text("배송지 이름") }, singleLine = true)
            OutlinedTextField(postalCode, { postalCode = it.filter(Char::isDigit).take(10) }, label = { Text("우편번호") }, singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("주소") })
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
    verificationRequested: Boolean,
    verificationConfirmed: Boolean,
    actionLoading: Boolean,
    actionError: String?,
    actionRevision: Int,
    onRetry: () -> Unit,
    onRequestVerification: (String) -> Unit,
    onConfirmVerification: (String) -> Unit,
    onSave: (bankName: String, accountNumber: String, accountHolder: String) -> Unit,
    onResetVerification: () -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(actionRevision) { if (actionRevision > 0) editorOpen = false }
    SettingsScaffold("정산 계좌 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                isLoading -> item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Colors.Navy) } }
                errorMessage != null && account == null -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(errorMessage, color = Colors.Muted, fontSize = 12.sp); OutlinedButton(onRetry, Modifier.padding(top = 10.dp)) { Text("다시 불러오기") } } }
                account != null -> item {
                    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("▣  ${account.bankName}", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(account.maskedAccountNumber, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("예금주 ${account.accountHolder} · 확인 완료", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
                else -> item { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("등록한 정산 계좌가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("판매 대금을 받을 본인 계좌를 등록해주세요", Modifier.padding(top = 7.dp), color = Colors.Muted, fontSize = 12.sp) } }
            }
            item { Text("✓  계좌 추가·변경 시 예금주 일치 여부를 확인해요", Modifier.fillMaxWidth().background(Color(0xFFE0F7F0), RoundedCornerShape(12.dp)).padding(16.dp), color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            item { Button({ onResetVerification(); editorOpen = true }, Modifier.fillMaxWidth().height(48.dp), enabled = !isLoading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text(if (account == null) "정산 계좌 등록" else "정산 계좌 변경", fontWeight = FontWeight.Bold) } }
        }
    }
    if (editorOpen) {
        var bank by rememberSaveable { mutableStateOf(account?.bankName.orEmpty()) }
        var number by remember { mutableStateOf("") }
        var holder by rememberSaveable { mutableStateOf(account?.accountHolder.orEmpty()) }
        var phone by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!actionLoading) editorOpen = false }, title = { Text(if (account == null) "정산 계좌 등록" else "정산 계좌 변경") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bank, { bank = it.take(30) }, label = { Text("은행") }, singleLine = true)
                OutlinedTextField(number, { number = it.filter(Char::isDigit).take(24) }, label = { Text("계좌번호") }, singleLine = true)
                OutlinedTextField(holder, { holder = it.take(30) }, label = { Text("예금주") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, Modifier.weight(1f), label = { Text("휴대전화") }, singleLine = true)
                    TextButton({ onRequestVerification(phone) }, enabled = phone.length >= 10 && !actionLoading) { Text(if (verificationRequested) "재전송" else "인증요청") }
                }
                if (verificationRequested && !verificationConfirmed) Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.weight(1f), label = { Text("인증번호") }, singleLine = true)
                    TextButton({ onConfirmVerification(code) }, enabled = code.length >= 4 && !actionLoading) { Text("확인") }
                }
                if (verificationConfirmed) Text("✓ 휴대전화 본인 인증 완료", color = Color(0xFF27806E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                actionError?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
            } },
            confirmButton = { TextButton({ onSave(bank.trim(), number, holder.trim()) }, enabled = verificationConfirmed && bank.isNotBlank() && number.length >= 8 && holder.isNotBlank() && !actionLoading) { if (actionLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("저장") } },
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
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Column(Modifier.fillMaxWidth().background(Color(0xFFF1F5FA), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("알림은 휴대전화 설정에서 관리해요", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("허용 여부와 소리·진동은 Android 시스템 설정에서 변경할 수 있어요.", color = Colors.Muted, fontSize = 12.sp) } }
            item { Text("dib에서 보내는 알림", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            item { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE3E8EF), RoundedCornerShape(16.dp))) {
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 11.sp) }
        Switch(checked, onChecked, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF61D1B2)))
    }
}

@Composable private fun SettingsScaffold(title: String, onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.height(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) } },
        bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) },
        content = content
    )
}
