package com.ssafy.dib.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/** Figma 01_Wireframe / 03L1_Product_Report and selected/success states. */
@Composable
fun ProductReportScreen(onBack: () -> Unit, onSubmitted: () -> Unit, modifier: Modifier = Modifier) {
    ReportFormScreen(
        title = "상품 신고",
        heading = "상품을 신고하는 이유를 선택해주세요",
        subtitle = "상품과 관련된 신고 사유를 선택해주세요.",
        reasons = listOf("허위·과장된 상품 정보", "판매 금지·제한 상품", "부적절한 이미지·내용", "사기 또는 거래 유도 의심", "기타"),
        onBack = onBack,
        onSubmitted = onSubmitted,
        modifier = modifier
    )
}

@Composable
internal fun ReportFormScreen(
    title: String,
    heading: String,
    subtitle: String,
    reasons: List<String>,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var detail by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuctionSubAppBar(title, onBack) }
    ) { padding ->
        if (submitted) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(56.dp).background(Colors.Mint, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = Colors.MintInk, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Text("신고가 접수됐어요", Modifier.padding(top = 20.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("검토 후 필요한 조치를 진행할게요.", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 13.sp)
                Button(onClick = onSubmitted, modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 20.dp)
            ) {
                item { Text(heading, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                item { Text(subtitle, color = Colors.Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)) }
                items(reasons.size) { index ->
                    ReportReasonRow(reasons[index], selected == index) { selected = index }
                }
                item { Text("상세 내용 (선택)", Modifier.padding(top = 16.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                item {
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it.take(300) },
                        modifier = Modifier.fillMaxWidth().height(104.dp),
                        placeholder = { Text("상황을 조금 더 자세히 알려주세요.", color = Colors.Muted, fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item { Text("허위 신고 또는 반복적인 악의적 신고는 서비스 이용에 제한이 있을 수 있어요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                item {
                    Button(
                        onClick = { submitted = true },
                        enabled = selected >= 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                    ) { Text("신고하기", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ReportReasonRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .border(1.dp, if (selected) Colors.Navy else Colors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Colors.Background, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(20.dp)
                .border(1.dp, if (selected) Colors.Navy else Colors.Muted, CircleShape)
                .background(if (selected) Colors.Navy else Colors.Background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(8.dp).background(Colors.Background, CircleShape))
        }
        Text(label, fontSize = 13.sp)
    }
}
