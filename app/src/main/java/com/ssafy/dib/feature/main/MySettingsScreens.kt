package com.ssafy.dib.feature.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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

private data class Address(val label: String, val recipient: String, val address: String, val isDefault: Boolean = false)
private data class BankAccount(val bank: String, val number: String, val isDefault: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressManagementScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val addresses = remember { mutableStateListOf(
        Address("집", "김띱", "부산광역시 동래구 중앙대로 000\n101동 1001호", true),
        Address("회사", "김띱", "부산광역시 부산진구 중앙대로 000\n8층")
    ) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf(-1) }
    var actionIndex by rememberSaveable { mutableIntStateOf(-1) }
    var deleteIndex by rememberSaveable { mutableIntStateOf(-1) }
    var message by rememberSaveable { mutableStateOf("") }
    if (message.isNotBlank()) LaunchedEffect(message) { kotlinx.coroutines.delay(1_800); message = "" }
    SettingsScaffold("배송지 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (message.isNotBlank()) item { Text("✓ $message", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(14.dp), color = Color(0xFF27806E), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            items(addresses.size) { index ->
                val address = addresses[index]
                Column(
                    Modifier.fillMaxWidth().height(130.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp))
                        .clickable { actionIndex = index }.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(if (address.isDefault) "기본 배송지" else "배송지", Modifier.weight(1f), color = if (address.isDefault) Color(0xFF61D1B2) else Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("수정", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${address.label} · ${address.recipient}", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(address.address, color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            item { Button({ editingIndex = -1; showEditor = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("새 배송지 추가", fontWeight = FontWeight.Bold) } }
        }
    }
    if (showEditor) {
        AddressEditor(
            initial = addresses.getOrNull(editingIndex),
            onDismiss = { showEditor = false },
            onSave = { value -> if (editingIndex >= 0) addresses[editingIndex] = value else addresses.add(value); showEditor = false },
            onDelete = if (editingIndex >= 0 && !addresses[editingIndex].isDefault) ({ addresses.removeAt(editingIndex); showEditor = false }) else null
        )
    }
    if (actionIndex >= 0) {
        val selected = addresses[actionIndex]
        ModalBottomSheet(onDismissRequest = { actionIndex = -1 }, containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("배송지 관리", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${selected.label} 배송지에 적용할 작업을 선택해주세요.", color = Colors.Muted, fontSize = 12.sp)
                SettingsAction("수정") { editingIndex = actionIndex; actionIndex = -1; showEditor = true }
                if (!selected.isDefault) SettingsAction("기본 배송지로 설정") {
                    addresses.indices.forEach { i -> addresses[i] = addresses[i].copy(isDefault = i == actionIndex) }
                    message = "기본 배송지가 ‘${selected.label}’로 변경됐어요"
                    actionIndex = -1
                }
                if (!selected.isDefault) SettingsAction("삭제", Color(0xFFEF596B)) { deleteIndex = actionIndex; actionIndex = -1 }
            }
        }
    }
    if (deleteIndex >= 0) {
        AlertDialog(onDismissRequest = { deleteIndex = -1 }, title = { Text("배송지를 삭제할까요?") }, text = { Text("‘${addresses[deleteIndex].label}’ 배송지를 삭제하면 주문 시 선택할 수 없습니다.") }, confirmButton = { TextButton({ addresses.removeAt(deleteIndex); deleteIndex = -1 }) { Text("삭제", color = Color(0xFFEF596B)) } }, dismissButton = { TextButton({ deleteIndex = -1 }) { Text("취소") } })
    }
}

@Composable private fun AddressEditor(initial: Address?, onDismiss: () -> Unit, onSave: (Address) -> Unit, onDelete: (() -> Unit)?) {
    var label by rememberSaveable { mutableStateOf(initial?.label.orEmpty()) }
    var recipient by rememberSaveable { mutableStateOf(initial?.recipient.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(initial?.address.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "새 배송지" else "배송지 수정") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(label, { label = it }, label = { Text("배송지 이름") }, singleLine = true)
            OutlinedTextField(recipient, { recipient = it }, label = { Text("받는 사람") }, singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("주소") })
            if (onDelete != null) Text("배송지 삭제", Modifier.clickable(onClick = onDelete).padding(vertical = 8.dp), color = Color(0xFFEF596B), fontWeight = FontWeight.Bold)
        } },
        confirmButton = { TextButton({ onSave(Address(label, recipient, address, initial?.isDefault == true)) }, enabled = label.isNotBlank() && recipient.isNotBlank() && address.isNotBlank()) { Text("저장") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementAccountsScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val accounts = remember { mutableStateListOf(BankAccount("우리은행", "1002-***-123456", true), BankAccount("카카오뱅크", "3333-**-7890123")) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var actionIndex by rememberSaveable { mutableIntStateOf(-1) }
    var deleteIndex by rememberSaveable { mutableIntStateOf(-1) }
    var message by rememberSaveable { mutableStateOf("") }
    if (message.isNotBlank()) LaunchedEffect(message) { kotlinx.coroutines.delay(1_800); message = "" }
    SettingsScaffold("정산 계좌 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (message.isNotBlank()) item { Text("✓ $message", Modifier.fillMaxWidth().background(Color(0xFFE8FAF5), RoundedCornerShape(12.dp)).padding(14.dp), color = Color(0xFF27806E), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            items(accounts.size) { index ->
                val account = accounts[index]
                Column(Modifier.fillMaxWidth().height(120.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp)).clickable { actionIndex = index }.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("▣  ${account.bank}${if (account.isDefault) "  · 기본" else ""}", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(account.number, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("예금주 김띱 · 확인 완료", color = Colors.Muted, fontSize = 12.sp)
                }
            }
            item { Text("✓  계좌 추가·변경 시 예금주 일치 여부를 확인해요", Modifier.fillMaxWidth().background(Color(0xFFE0F7F0), RoundedCornerShape(12.dp)).padding(16.dp), color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            item { Button({ editingIndex = -1; addOpen = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("계좌 추가", fontWeight = FontWeight.Bold) } }
        }
    }
    if (addOpen) {
        val editingAccount = accounts.getOrNull(editingIndex)
        var bank by rememberSaveable(editingIndex) { mutableStateOf(editingAccount?.bank.orEmpty()) }
        var number by rememberSaveable(editingIndex) { mutableStateOf(editingAccount?.number?.filter(Char::isDigit).orEmpty()) }
        var duplicate by rememberSaveable(editingIndex) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { addOpen = false }, title = { Text(if (editingAccount == null) "정산 계좌 추가" else "정산 계좌 수정") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(bank, { bank = it }, label = { Text("은행") }); OutlinedTextField(number, { number = it.filter(Char::isDigit); duplicate = false }, label = { Text("계좌번호") }); if (duplicate) Text("이미 등록된 계좌예요", color = Color(0xFFEF596B), fontSize = 12.sp, fontWeight = FontWeight.Bold) } },
            confirmButton = { TextButton({
                val duplicateAccount = accounts.withIndex().any { (index, account) -> index != editingIndex && account.number.filter(Char::isDigit).takeLast(6) == number.takeLast(6) }
                if (duplicateAccount) duplicate = true else {
                    val updated = BankAccount(bank, maskAccount(number), editingAccount?.isDefault ?: accounts.isEmpty())
                    if (editingIndex >= 0) accounts[editingIndex] = updated else accounts.add(updated)
                    addOpen = false
                }
            }, enabled = bank.isNotBlank() && number.length >= 8) { Text(if (editingAccount == null) "본인 확인 후 추가" else "본인 확인 후 저장") } },
            dismissButton = { TextButton({ addOpen = false }) { Text("취소") } }
        )
    }
    if (actionIndex >= 0) {
        val selected = accounts[actionIndex]
        ModalBottomSheet(onDismissRequest = { actionIndex = -1 }, containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("정산 계좌 관리", color = Colors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${selected.bank} 계좌에 적용할 작업을 선택해주세요.", color = Colors.Muted, fontSize = 12.sp)
                SettingsAction("수정 후 예금주 확인") { editingIndex = actionIndex; actionIndex = -1; addOpen = true }
                if (!selected.isDefault) SettingsAction("기본 정산 계좌로 설정") {
                    accounts.indices.forEach { i -> accounts[i] = accounts[i].copy(isDefault = i == actionIndex) }
                    message = "기본 정산 계좌가 변경됐어요"
                    actionIndex = -1
                }
                if (!selected.isDefault) SettingsAction("삭제", Color(0xFFEF596B)) { deleteIndex = actionIndex; actionIndex = -1 }
            }
        }
    }
    if (deleteIndex >= 0) {
        AlertDialog(onDismissRequest = { deleteIndex = -1 }, title = { Text("정산 계좌를 삭제할까요?") }, text = { Text("진행 중인 정산이 있으면 해당 계좌는 삭제할 수 없습니다.") }, confirmButton = { TextButton({ accounts.removeAt(deleteIndex); deleteIndex = -1 }) { Text("삭제", color = Color(0xFFEF596B)) } }, dismissButton = { TextButton({ deleteIndex = -1 }) { Text("취소") } })
    }
}

@Composable private fun SettingsAction(label: String, color: Color = Colors.Navy, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

private fun maskAccount(number: String): String = when {
    number.length < 7 -> number
    else -> number.take(4) + "-***-" + number.takeLast(6)
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var trade by rememberSaveable { mutableStateOf(true) }
    var live by rememberSaveable { mutableStateOf(true) }
    var wishlist by rememberSaveable { mutableStateOf(false) }
    SettingsScaffold("알림 설정", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Column(Modifier.fillMaxWidth().background(Color(0xFFF1F5FA), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("알림은 휴대전화 설정에서 관리해요", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("허용 여부와 소리·진동은 Android 시스템 설정에서 변경할 수 있어요.", color = Colors.Muted, fontSize = 12.sp) } }
            item { Text("dib에서 보내는 알림", color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            item { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE3E8EF), RoundedCornerShape(16.dp))) {
                NotificationToggle("입찰·거래 상태", "상회 입찰, 낙찰, 결제와 배송 상태", trade) { trade = it }
                NotificationToggle("팔로잉 판매자 라이브", "예약 라이브 시작 10분 전과 시작 시점", live) { live = it }
                NotificationToggle("찜한 경매", "찜한 경매의 시작·마감 임박 알림", wishlist) { wishlist = it }
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
