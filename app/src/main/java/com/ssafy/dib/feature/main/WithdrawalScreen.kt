package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

private enum class WithdrawalState { Check, Blocked, Success }

@Composable
fun WithdrawalScreen(onBack: () -> Unit, onOpenTrades: () -> Unit, onComplete: () -> Unit, modifier: Modifier = Modifier) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    var hasBlockingTrades by rememberSaveable { mutableStateOf(true) }
    var state by rememberSaveable { mutableStateOf(WithdrawalState.Check) }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text(if (state == WithdrawalState.Success) "×" else "←", Modifier.size(48.dp).clickable { if (state == WithdrawalState.Success) onComplete() else onBack() }.padding(start = 14.dp, top = 8.dp), fontSize = 22.sp); Text(if (state == WithdrawalState.Success) "회원 탈퇴 완료" else "회원 탈퇴", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                WithdrawalState.Check -> {
                    WarningHero("탈퇴 전에 확인해주세요", "진행 중인 입찰·판매·주문이 있으면\n탈퇴할 수 없습니다.")
                    SectionCard("탈퇴 시 삭제되는 정보", listOf("프로필 및 계정 정보", "찜·최근 본 상품 기록", "문의 및 알림 정보"))
                    Row(Modifier.fillMaxWidth().height(56.dp).clickable { agreed = !agreed }, verticalAlignment = Alignment.CenterVertically) { Checkbox(agreed, { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = Colors.Navy)); Text("안내 내용을 확인했습니다", fontSize = 13.sp) }
                    Spacer(Modifier.weight(1f))
                    Button({ state = if (hasBlockingTrades) WithdrawalState.Blocked else WithdrawalState.Success }, Modifier.fillMaxWidth().height(52.dp), enabled = agreed, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF596B))) { Text("탈퇴 신청", fontWeight = FontWeight.Bold) }
                }
                WithdrawalState.Blocked -> {
                    WarningHero("현재는 탈퇴할 수 없어요", "진행 중인 입찰·판매·주문을 모두 완료한 뒤\n다시 시도해주세요.")
                    SectionCard("완료가 필요한 항목", listOf("진행 중인 입찰 1건", "판매 중인 상품 1건", "미완료 주문·정산 없음"))
                    Text("내 거래에서 진행 상태를 확인할 수 있어요", Modifier.fillMaxWidth().background(Color(0xFFF1F5FA), RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Button(onOpenTrades, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("내 거래 확인하기", fontWeight = FontWeight.Bold) }
                    TextButton({ hasBlockingTrades = false; agreed = true; state = WithdrawalState.Check }) { Text("거래 완료 후 다시 확인", color = Colors.Navy) }
                }
                WithdrawalState.Success -> {
                    Spacer(Modifier.height(80.dp))
                    Box(Modifier.size(88.dp).background(Color(0xFFE8FAF5), CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = Color(0xFF27806E), fontSize = 42.sp, fontWeight = FontWeight.Bold) }
                    Text("회원 탈퇴가 완료됐어요", Modifier.padding(top = 28.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("그동안 dib을 이용해주셔서 감사합니다.", Modifier.padding(top = 12.dp), color = Colors.Muted, fontSize = 13.sp)
                    SectionCard("계정 삭제 완료", listOf("개인정보는 정책에 따라 안전하게 처리돼요"), Modifier.padding(top = 36.dp))
                    Spacer(Modifier.weight(1f))
                    Button(onComplete, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("시작 화면으로", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable private fun WarningHero(title: String, body: String) { Column(Modifier.fillMaxWidth().background(Color(0xFFFFF1EA), RoundedCornerShape(18.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("!", color = Color(0xFFF26B47), fontSize = 34.sp, fontWeight = FontWeight.Bold); Text(title, color = Colors.Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 13.sp, lineHeight = 20.sp) } }
@Composable private fun SectionCard(title: String, items: List<String>, modifier: Modifier = Modifier) { Column(modifier.fillMaxWidth().padding(top = 20.dp).background(Color.White, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold); items.forEach { Text("✓  $it", color = Colors.Muted, fontSize = 12.sp) } } }
