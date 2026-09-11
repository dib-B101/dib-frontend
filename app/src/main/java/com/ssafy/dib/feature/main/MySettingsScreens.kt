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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
private data class BankAccount(val bank: String, val number: String)

@Composable
fun AddressManagementScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val addresses = remember { mutableStateListOf(
        Address("집", "김띱", "부산광역시 동래구 중앙대로 000\n101동 1001호", true),
        Address("회사", "김띱", "부산광역시 부산진구 중앙대로 000\n8층")
    ) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf(-1) }
    SettingsScaffold("배송지 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(addresses.size) { index ->
                val address = addresses[index]
                Column(
                    Modifier.fillMaxWidth().height(130.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp))
                        .clickable { editingIndex = index; showEditor = true }.padding(15.dp),
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

@Composable
fun SettlementAccountsScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val accounts = remember { mutableStateListOf(BankAccount("우리은행", "1002-***-123456"), BankAccount("카카오뱅크", "3333-**-7890123")) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    SettingsScaffold("정산 계좌 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(accounts.size) { index ->
                val account = accounts[index]
                Column(Modifier.fillMaxWidth().height(120.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(14.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("▣  ${account.bank}", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(account.number, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth()) { Text("예금주 김띱 · 확인 완료", Modifier.weight(1f), color = Colors.Muted, fontSize = 12.sp); Text("삭제", Modifier.clickable { accounts.removeAt(index) }, color = Color(0xFFEF596B), fontSize = 11.sp) }
                }
            }
            item { Text("✓  계좌 추가·변경 시 예금주 일치 여부를 확인해요", Modifier.fillMaxWidth().background(Color(0xFFE0F7F0), RoundedCornerShape(12.dp)).padding(16.dp), color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            item { Button({ addOpen = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("계좌 추가", fontWeight = FontWeight.Bold) } }
        }
    }
    if (addOpen) {
        var bank by rememberSaveable { mutableStateOf("") }
        var number by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addOpen = false }, title = { Text("정산 계좌 추가") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(bank, { bank = it }, label = { Text("은행") }); OutlinedTextField(number, { number = it.filter(Char::isDigit) }, label = { Text("계좌번호") }) } },
            confirmButton = { TextButton({ accounts.add(BankAccount(bank, maskAccount(number))); addOpen = false }, enabled = bank.isNotBlank() && number.length >= 8) { Text("본인 확인 후 추가") } },
            dismissButton = { TextButton({ addOpen = false }) { Text("취소") } }
        )
    }
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
