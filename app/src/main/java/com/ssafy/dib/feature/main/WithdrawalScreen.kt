package com.ssafy.dib.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.member.MemberWithdrawal
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class WithdrawalState { Check, Blocked, Requested }

@Composable
fun WithdrawalScreen(
    isSubmitting: Boolean,
    errorMessage: String?,
    blockingMessage: String?,
    withdrawal: MemberWithdrawal?,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onOpenTrades: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    var state by rememberSaveable { mutableStateOf(WithdrawalState.Check) }
    LaunchedEffect(withdrawal) { if (withdrawal != null) state = WithdrawalState.Requested }
    LaunchedEffect(blockingMessage) { if (blockingMessage != null) state = WithdrawalState.Blocked }
    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(), containerColor = Colors.Canvas,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { Column(Modifier.background(Colors.Background)){Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={if(state==WithdrawalState.Requested)onComplete() else onBack()}){Image(painterResource(if(state==WithdrawalState.Requested)R.drawable.close else R.drawable.back),if(state==WithdrawalState.Requested)"닫기" else "뒤로",Modifier.size(22.dp),colorFilter=ColorFilter.tint(Colors.Text))};Text(if(state==WithdrawalState.Requested)"회원 탈퇴 신청 완료" else "회원 탈퇴",color=Colors.Text,fontSize=17.sp,fontWeight=FontWeight.Bold)};HorizontalDivider(color=Colors.Border)} }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp,vertical=20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (state) {
                WithdrawalState.Check -> {
                    WarningHero("탈퇴 전에 확인해주세요", "진행 중인 입찰·판매·주문이 있으면\n탈퇴할 수 없습니다.")
                    SectionCard("탈퇴 시 삭제되는 정보", listOf("프로필 및 계정 정보", "찜·최근 본 상품 기록", "문의 및 알림 정보"))
                    Row(Modifier.fillMaxWidth().height(56.dp).clickable { agreed = !agreed }, verticalAlignment = Alignment.CenterVertically) { Checkbox(agreed, { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = Colors.Navy)); Text("안내 내용을 확인했습니다", fontSize = 13.sp) }
                    errorMessage?.let { Text(it, Modifier.fillMaxWidth(), color = Colors.Urgent, fontSize = 12.sp) }
                    Spacer(Modifier.weight(1f))
                    Button(onSubmit, Modifier.fillMaxWidth().height(52.dp), enabled = agreed && !isSubmitting, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF596B))) {
                        if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("탈퇴 신청", fontWeight = FontWeight.Bold)
                    }
                }
                WithdrawalState.Blocked -> {
                    WarningHero("현재는 탈퇴할 수 없어요", blockingMessage ?: "진행 중인 거래를 모두 완료한 뒤\n다시 시도해주세요.")
                    SectionCard("완료가 필요한 항목", listOf("진행 중인 주문 또는 경매를 먼저 완료해주세요"))
                    Text("내 거래에서 진행 상태를 확인할 수 있어요", Modifier.fillMaxWidth().background(Color(0xFFF1F5FA), RoundedCornerShape(12.dp)).padding(14.dp), color = Colors.Muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Button(onOpenTrades, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("내 거래 확인하기", fontWeight = FontWeight.Bold) }
                    TextButton({ agreed = true; state = WithdrawalState.Check }) { Text("거래 완료 후 다시 확인", color = Colors.Navy) }
                }
                WithdrawalState.Requested -> {
                    Spacer(Modifier.height(80.dp))
                    Box(Modifier.size(88.dp).background(Color(0xFFE8FAF5), CircleShape), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.check_circle), null, Modifier.size(48.dp), colorFilter = ColorFilter.tint(Color(0xFF27806E))) }
                    Text("탈퇴 신청이 접수됐어요", Modifier.padding(top = 28.dp), color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("예정 시각까지 계정 삭제가 유예됩니다.", Modifier.padding(top = 12.dp), color = Colors.Muted, fontSize = 13.sp)
                    withdrawal?.let { result ->
                        SectionCard(
                            "탈퇴 처리 일정",
                            listOf(
                                "신청 시각  ${withdrawalTimeLabel(result.requestedAt)}",
                                "삭제 예정  ${withdrawalTimeLabel(result.scheduledAt)}"
                            ),
                            Modifier.padding(top = 36.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onComplete, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("로그아웃하고 시작 화면으로", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

internal fun withdrawalTimeLabel(value: String, zoneId: ZoneId = ZoneId.systemDefault()): String =
    runCatching {
        Instant.parse(value).atZone(zoneId).format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"))
    }.getOrDefault(value)

@Composable private fun WarningHero(title: String, body: String) { Column(Modifier.fillMaxWidth().background(Colors.UrgentBackground, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.size(48.dp).background(Colors.Background.copy(alpha=.82f),CircleShape),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.warning_outline), null, Modifier.size(27.dp), colorFilter = ColorFilter.tint(Colors.Urgent))}; Text(title, color = Colors.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(body, color = Colors.Muted, fontSize = 13.sp, lineHeight = 20.sp) } }
@Composable private fun SectionCard(title: String, items: List<String>, modifier: Modifier = Modifier) { Column(modifier.fillMaxWidth().padding(top = 20.dp).background(Colors.Background, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, color = Colors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold); items.forEach { Text("•  $it", color = Colors.Muted, fontSize = 12.sp) } } }
