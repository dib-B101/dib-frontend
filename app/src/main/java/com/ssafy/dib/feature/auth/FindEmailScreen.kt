package com.ssafy.dib.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun FindEmailScreen(
    verificationRequested: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    maskedEmail: String?,
    onRequestVerification: (String) -> Unit,
    onConfirmVerification: (String) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFCFBF7),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) { Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp); Text("이메일 찾기", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
            Text(if (maskedEmail == null) "가입한 이메일을 찾아볼게요" else "가입 이메일을 찾았어요", color = Colors.Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(if (maskedEmail == null) "가입할 때 인증한 휴대전화 번호를 입력해주세요." else "개인정보 보호를 위해 일부 문자를 가렸어요.", Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))

            if (maskedEmail == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter(Char::isDigit).take(11) },
                        modifier = Modifier.weight(1f),
                        label = { Text("휴대전화 번호") },
                        placeholder = { Text("01012345678") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    TextButton(
                        onClick = { onRequestVerification(phoneNumber) },
                        enabled = phoneNumber.length >= 10 && !isLoading
                    ) { Text(if (verificationRequested) "재전송" else "인증요청") }
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
                        onClick = { onConfirmVerification(verificationCode) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                        enabled = verificationCode.length >= 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                    ) { if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("이메일 확인", fontWeight = FontWeight.Bold) }
                }
            } else {
                Text(maskedEmail, Modifier.fillMaxWidth().background(Color(0xFFEAF8F4)).padding(horizontal = 18.dp, vertical = 24.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Button(onLogin, Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)) { Text("로그인으로 돌아가기", fontWeight = FontWeight.Bold) }
            }
            errorMessage?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
        }
    }
}
