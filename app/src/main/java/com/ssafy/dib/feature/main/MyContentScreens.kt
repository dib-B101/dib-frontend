package com.ssafy.dib.feature.main

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibBottomNavigation
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibWishlistButton
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun ProfileEditScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var nickname by rememberSaveable { mutableStateOf("dib러버") }
    SimpleHeaderScaffold("프로필 수정", onBack, modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp).background(Color(0xFFD6F5ED), CircleShape).clickable { }, contentAlignment = Alignment.Center) { Text("d", color = Colors.Navy, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
            Text("사진 변경", Modifier.padding(top = 8.dp), color = Color(0xFFF5636E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("닉네임", Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(nickname, { nickname = it.take(12) }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Text("이메일", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFF0F2F7), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).padding(14.dp)) { Text("dib_user@email.com", color = Colors.Muted, fontSize = 14.sp) }
            Button(onBack, Modifier.fillMaxWidth().padding(top = 28.dp).height(48.dp), enabled = nickname.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("변경사항 저장", fontWeight = FontWeight.Bold) }
        }
    }
}

private data class RegisteredProduct(val title: String, val detail: String, val status: String)

@Composable
fun RegisteredProductsScreen(onBack: () -> Unit, onRegister: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val products = listOf(
        RegisteredProduct("빈티지 필름 카메라", "AI 검수 중", "검수 중"),
        RegisteredProduct("달빛 유약 머그컵", "경매 등록 가능", "승인"),
        RegisteredProduct("핸드메이드 가죽 지갑", "이미지 정책 확인 필요", "거부")
    )
    var filter by rememberSaveable { mutableStateOf("전체") }
    MyListScaffold("등록 상품 관리", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("전체", "검수 중", "승인", "거부").forEach { label -> FilterChip(label, filter == label) { filter = label } } } }
            val filtered = products.filter { filter == "전체" || it.status == filter }
            items(filtered.size) { index ->
                val product = filtered[index]
                Row(Modifier.fillMaxWidth().height(76.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Color(0xFFD1D6DE), RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(product.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(product.detail, color = Colors.Muted, fontSize = 11.sp) }
                    Text(product.status, color = when (product.status) { "승인" -> Color(0xFF61D1B2); "거부" -> Color(0xFFF5636E); else -> Color(0xFFF26B47) }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Button(onRegister, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("새 상품 등록", fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun FavoriteAuctionsScreen(onBack: () -> Unit, onProductClick: (String) -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val favorites = remember { mutableStateListOf("무선 헤드폰", "빈티지 필름 카메라", "달빛 유약 머그컵", "레더 숄더백") }
    MyListScaffold("찜한 상품", onBack, onTabSelected, modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("찜한 경매 ${favorites.size}개", Modifier.padding(vertical = 16.dp), color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (favorites.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(R.drawable.favorite_outline), null, Modifier.size(36.dp), colorFilter = ColorFilter.tint(Color(0xFFB8C0CC)))
                        Text("찜한 경매가 없어요", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("관심 있는 상품의 하트를 눌러 저장해보세요", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favorites, key = { it }) { title ->
                        Column(Modifier.clickable { onProductClick(if (title.contains("카메라")) "camera" else "favorite") }) {
                            Box(Modifier.fillMaxWidth().height(122.dp).background(Color(0xFFD1D4D9), RoundedCornerShape(10.dp))) {
                                Text("LIVE", Modifier.padding(8.dp).background(Color(0x6B000000), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontSize = 9.sp)
                                DibWishlistButton(true, { if (!it) favorites.remove(title) }, title, Modifier.align(Alignment.TopEnd))
                            }
                            Text(title, Modifier.padding(top = 6.dp), color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("현재가 52,000원", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("입찰 7명", color = Colors.Muted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class Inquiry(val status: String, val title: String, val date: String)

@Composable
fun InquiryHistoryScreen(onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier = Modifier) {
    val inquiries = remember { mutableStateListOf(Inquiry("답변 완료", "배송 상태가 갱신되지 않아요", "2026.09.08"), Inquiry("답변 대기", "자동 결제 실패 문의", "2026.09.09")) }
    var formOpen by rememberSaveable { mutableStateOf(false) }
    MyListScaffold("문의 내역", onBack, onTabSelected, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("문의 답변은 등록한 이메일로도 알려드려요", color = Colors.Muted, fontSize = 12.sp) }
            items(inquiries.size) { index -> val item = inquiries[index]; Column(Modifier.fillMaxWidth().height(104.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDBE0E8), RoundedCornerShape(12.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(item.status, color = if (item.status == "답변 완료") Color(0xFF61D1B2) else Color(0xFFF26B47), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(item.title, color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(item.date, color = Colors.Muted, fontSize = 11.sp) } }
            item { Button({ formOpen = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("문의하기", fontWeight = FontWeight.Bold) } }
        }
    }
    if (formOpen) {
        var title by rememberSaveable { mutableStateOf("") }; var body by rememberSaveable { mutableStateOf("") }
        AlertDialog(onDismissRequest = { formOpen = false }, title = { Text("문의하기") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("제목") }); OutlinedTextField(body, { body = it }, label = { Text("문의 내용") }) } }, confirmButton = { TextButton({ inquiries.add(0, Inquiry("답변 대기", title, "2026.09.11")); formOpen = false }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("등록") } }, dismissButton = { TextButton({ formOpen = false }) { Text("취소") } })
    }
}

@Composable
fun ReportHistoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SimpleHeaderScaffold("신고 내역", onBack, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { HistoryCard("상품 신고", "빈티지 필름 카메라", "허위 정보가 포함되어 있어요", "검토 완료") }
            item { HistoryCard("판매자 신고", "seller01", "부적절한 메시지를 받았어요", "접수됨") }
        }
    }
}

@Composable private fun HistoryCard(type: String, target: String, reason: String, status: String) { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(Modifier.fillMaxWidth()) { Text(type, Modifier.weight(1f), color = Colors.Muted, fontSize = 11.sp); Text(status, color = Color(0xFF41AA8E), fontSize = 11.sp, fontWeight = FontWeight.Bold) }; Text(target, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(reason, color = Colors.Muted, fontSize = 12.sp) } }

@Composable private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) { Text(label, Modifier.height(32.dp).background(if (selected) Colors.Navy else Color.White, RoundedCornerShape(16.dp)).border(1.dp, if (selected) Colors.Navy else Color(0xFFDBE0E8), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp), color = if (selected) Color.White else Colors.Muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }

@Composable private fun MyListScaffold(title: String, onBack: () -> Unit, onTabSelected: (DibMainTab) -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB), contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, bottomBar = { DibBottomNavigation(DibMainTab.My, onTabSelected) }, content = content) }
@Composable private fun SimpleHeaderScaffold(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable (PaddingValues) -> Unit) { Scaffold(modifier.fillMaxSize().safeDrawingPadding(), containerColor = Color(0xFFF7F9FB), contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0,0,0,0), topBar = { Header(title, onBack) }, content = content) }
@Composable private fun Header(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.height(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text(title, color = Colors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
