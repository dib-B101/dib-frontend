package com.ssafy.dib.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var phoneAttempted by remember { mutableStateOf(false) }
    var codeAttempted by remember { mutableStateOf(false) }
    val phoneValid = phoneNumber.length in 10..11
    val codeValid = verificationCode.length == 6

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("이메일 찾기", onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
            AuthPageTitle(
                if (maskedEmail == null) "가입한 이메일을\n찾아볼게요" else "가입 이메일을 찾았어요",
                if (maskedEmail == null) "가입할 때 인증한 휴대전화 번호를 입력해주세요." else "개인정보 보호를 위해 일부 문자를 가렸어요."
            )
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
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        isError = phoneAttempted && !phoneValid,
                        supportingText = if (phoneAttempted && !phoneValid) ({ Text("휴대전화 번호를 확인해주세요.") }) else null
                    )
                    TextButton(
                        onClick = {
                            phoneAttempted = true
                            if (phoneValid) onRequestVerification(phoneNumber)
                        },
                        enabled = !isLoading
                    ) { Text(if (verificationRequested) "재전송" else "인증요청") }
                }
                if (verificationRequested) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("인증번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        isError = codeAttempted && !codeValid,
                        supportingText = if (codeAttempted && !codeValid) ({ Text("인증번호 6자리를 입력해주세요.") }) else null
                    )
                    AuthPrimaryButton(
                        text = "이메일 확인",
                        enabled = codeValid,
                        loading = isLoading,
                        onClick = {
                            codeAttempted = true
                            if (codeValid) onConfirmVerification(verificationCode)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                Text(maskedEmail, Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(18.dp)).padding(horizontal = 20.dp, vertical = 26.dp), color = Colors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                AuthPrimaryButton("로그인으로 돌아가기", true, false, onLogin, Modifier.padding(top = 20.dp))
            }
            errorMessage?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
        }
    }
}
