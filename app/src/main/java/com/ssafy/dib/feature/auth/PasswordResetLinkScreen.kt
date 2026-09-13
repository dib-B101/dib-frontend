package com.ssafy.dib.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
fun PasswordResetLinkScreen(
    verificationRequested: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    linkSent: Boolean,
    onRequestVerification: (String) -> Unit,
    onRequestResetLink: (email: String, code: String) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    val emailValid = email.contains('@') && email.substringAfter('@').contains('.')

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFCFBF7),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text("비밀번호 찾기", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
            Text(if (linkSent) "재설정 링크를 보냈어요" else "비밀번호를 다시 설정해요", color = Colors.Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(
                if (linkSent) "메일함에서 30분 안에 링크를 열어주세요." else "가입 이메일과 인증한 휴대전화 번호를 확인할게요.",
                Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(28.dp))

            if (!linkSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("가입 이메일") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter(Char::isDigit).take(11) },
                        modifier = Modifier.weight(1f),
                        label = { Text("휴대전화 번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    TextButton({ onRequestVerification(phoneNumber) }, enabled = phoneNumber.length >= 10 && !isLoading) { Text(if (verificationRequested) "재전송" else "인증요청") }
                }
                if (verificationRequested) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("인증번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Button(
                        onClick = { onRequestResetLink(email.trim(), verificationCode) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                        enabled = emailValid && verificationCode.length >= 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                        shape = RoundedCornerShape(12.dp)
                    ) { if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("재설정 링크 받기", fontWeight = FontWeight.Bold) }
                }
            } else {
                Column(Modifier.fillMaxWidth().background(Color(0xFFEAF8F4), RoundedCornerShape(14.dp)).padding(18.dp)) {
                    Text("메일이 보이지 않나요?", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("스팸 메일함을 확인하고 입력한 이메일 주소가 맞는지 확인해주세요.", Modifier.padding(top = 6.dp), color = Colors.Muted, fontSize = 12.sp)
                }
                Button(onLogin, Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy), shape = RoundedCornerShape(12.dp)) { Text("로그인으로 돌아가기", fontWeight = FontWeight.Bold) }
            }
            errorMessage?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
        }
    }
}
