package com.ssafy.dib.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    onChangePhone: () -> Unit,
    onRequestResetLink: (email: String, code: String) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var phoneAttempted by remember { mutableStateOf(false) }
    var linkAttempted by remember { mutableStateOf(false) }
    val emailValid = email.contains('@') && email.substringAfter('@').contains('.')
    val phoneValid = phoneNumber.matches(Regex("010[0-9]{8}"))
    val codeValid = verificationCode.length == 6

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("비밀번호 찾기", onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.Center) {
            AuthPageTitle(
                if (linkSent) "재설정 링크를\n보냈어요" else "비밀번호를\n다시 설정해요",
                if (linkSent) "메일함에서 30분 안에 링크를 열어주세요." else "가입 이메일과 인증한 휴대전화 번호를 확인할게요."
            )
            Spacer(Modifier.height(28.dp))

            if (!linkSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("가입 이메일") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    isError = linkAttempted && !emailValid,
                    supportingText = if (linkAttempted && !emailValid) ({ Text("올바른 이메일을 입력해주세요.") }) else null
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter(Char::isDigit).take(11) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("휴대전화 번호") },
                    placeholder = { Text("01012345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    readOnly = verificationRequested,
                    shape = RoundedCornerShape(15.dp),
                    isError = phoneAttempted && !phoneValid,
                    supportingText = if (phoneAttempted && !phoneValid) ({ Text("010으로 시작하는 11자리 번호를 입력해주세요.") }) else null
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (verificationRequested) TextButton(onClick = {
                        verificationCode = ""
                        onChangePhone()
                    }, enabled = !isLoading) { Text("번호 변경") }
                    TextButton(onClick = {
                        phoneAttempted = true
                        if (phoneValid) onRequestVerification(phoneNumber)
                    }, enabled = !isLoading) { Text(if (verificationRequested) "인증번호 재전송" else "인증번호 받기") }
                }
                if (verificationRequested) {
                    Text("인증한 번호로 받은 6자리 코드를 입력해주세요.", color = Colors.Muted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        label = { Text("인증번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        isError = linkAttempted && !codeValid,
                        supportingText = if (linkAttempted && !codeValid) ({ Text("인증번호 6자리를 입력해주세요.") }) else null
                    )
                    AuthPrimaryButton(
                        text = "재설정 링크 받기",
                        enabled = emailValid && codeValid,
                        loading = isLoading,
                        onClick = {
                            linkAttempted = true
                            if (emailValid && codeValid) onRequestResetLink(email.trim(), verificationCode)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth().background(Colors.MintSoft, RoundedCornerShape(18.dp)).padding(20.dp)) {
                    Text("메일이 보이지 않나요?", color = Colors.Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("스팸 메일함을 확인하고 입력한 이메일 주소가 맞는지 확인해주세요.", Modifier.padding(top = 6.dp), color = Colors.Muted, fontSize = 12.sp)
                }
                AuthPrimaryButton("로그인으로 돌아가기", true, false, onLogin, Modifier.padding(top = 20.dp))
            }
            errorMessage?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Colors.Urgent, fontSize = 12.sp) }
        }
    }
}
