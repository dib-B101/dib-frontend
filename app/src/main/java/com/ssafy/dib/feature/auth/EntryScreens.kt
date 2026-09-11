package com.ssafy.dib.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        delay(1_300)
        onFinished()
    }
    Column(
        modifier.fillMaxSize().safeDrawingPadding().background(Color(0xFFFCFBF7)).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(58.dp))
        Image(
            painterResource(R.drawable.splash_mascot),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(18.dp))
        Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(104.dp, 66.dp), contentScale = ContentScale.Fit)
        Text("경매의 순간을 잡다", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { .64f },
            modifier = Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF6EDCC1),
            trackColor = Color(0xFFE0E3E0)
        )
        Spacer(Modifier.height(68.dp))
    }
}

@Composable
fun WelcomeScreen(
    onKakaoStart: () -> Unit,
    onEmailSignup: () -> Unit,
    onLogin: () -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().safeDrawingPadding().background(Color(0xFFFCFBF7))) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 61.dp), contentScale = ContentScale.Fit)
            Text("실시간 경매를\n더 쉽고 안전하게", color = Colors.Navy, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
            Text("라이브로 둘러보고, 원하는 물건에 빠르게 입찰해보세요", Modifier.padding(top = 12.dp), color = Colors.Muted, fontSize = 13.sp)
            Image(
                painterResource(R.drawable.welcome_mascot),
                contentDescription = null,
                modifier = Modifier.padding(top = 24.dp).size(270.dp, 312.dp).clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.weight(1f))
        }
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Colors.Navy)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = Color.White.copy(alpha = .06f), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth().height(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("♢  AI 기반 안전거래 지원", color = Color.White, fontSize = 12.sp) }
            }
            Button(onClick = onKakaoStart, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE500), contentColor = Color(0xFF16181B))) {
                Text("▢  카카오로 시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onEmailSignup, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)) {
                Text("이메일로 회원가입", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("이미 계정이 있나요?  로그인", Modifier.clickable(onClick = onLogin).padding(6.dp), color = Color(0xFF6EDCC1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("로그인 없이 둘러보기", Modifier.clickable(onClick = onBrowse).padding(6.dp), color = Color.White, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val valid = email.contains('@') && password.length >= 4

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Color(0xFFFCFBF7),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().height(48.dp).background(Color.White), verticalAlignment = Alignment.CenterVertically) {
                Text("←", Modifier.size(48.dp).clickable(onClick = onBack).wrapContentSize(), fontSize = 24.sp)
                Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 61.dp), contentScale = ContentScale.Fit)
            Text("dib 계정으로 경매를 계속해보세요", color = Colors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(26.dp))
            LoginField("이메일", email, { email = it }, "이메일을 입력해주세요", KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            LoginField("비밀번호", password, { password = it }, "비밀번호를 입력해주세요", KeyboardType.Password, passwordVisible) {
                passwordVisible = !passwordVisible
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("비밀번호 찾기", color = Colors.Muted, fontSize = 12.sp)
                Text(
                    "회원가입",
                    Modifier.clickable(onClick = onSignUp).padding(4.dp),
                    color = Colors.Navy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (attempted && !valid) Text("이메일과 4자 이상의 비밀번호를 확인해주세요", Modifier.fillMaxWidth().padding(top = 8.dp), color = Colors.Urgent, fontSize = 11.sp)
            errorMessage?.let {
                Text(it, Modifier.fillMaxWidth().padding(top = 8.dp), color = Colors.Urgent, fontSize = 11.sp)
            }
            Button(
                onClick = { attempted = true; if (valid) onLogin(email.trim(), password) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
                enabled = valid && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("로그인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD1D6DE))
                Text("또는", Modifier.padding(horizontal = 20.dp), color = Colors.Muted, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD1D6DE))
            }
            Button(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color(0xFF17140F), disabledContainerColor = Color(0xFFF3E787), disabledContentColor = Color(0xFF6F681F))) {
                Text("▢  카카오로 로그인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("카카오 로그인은 서버 연동 후 사용할 수 있어요", Modifier.padding(top = 18.dp), color = Colors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LoginField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType, visible: Boolean = true, onVisibility: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            placeholder = { Text(placeholder, color = Color(0xFF8C919C), fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (keyboardType == KeyboardType.Password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = onVisibility?.let { action -> ({ Text(if (visible) "◉" else "◎", Modifier.clickable(onClick = action), color = Colors.Muted) }) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Colors.Navy, unfocusedBorderColor = Color(0xFFD1D6DE))
        )
    }
}
