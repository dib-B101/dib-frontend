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
        modifier.fillMaxSize().safeDrawingPadding().background(Colors.Canvas).padding(horizontal = 24.dp),
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
            color = Colors.Mint,
            trackColor = Colors.Border
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
    Box(modifier.fillMaxSize().safeDrawingPadding().background(Colors.Canvas)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 61.dp), contentScale = ContentScale.Fit)
            Text("실시간 경매를\n더 쉽고 안전하게", color = Colors.Text, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Text("라이브로 둘러보고 원하는 물건에 바로 참여해보세요", Modifier.padding(top = 10.dp), color = Colors.Muted, fontSize = 14.sp)
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
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = Color.White.copy(alpha = .08f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("AI 기반 상품 검수와 안전한 거래", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
            Button(onClick = onKakaoStart, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE500), contentColor = Color(0xFF16181B))) {
                Text("카카오로 시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onEmailSignup, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)) {
                Text("이메일로 회원가입", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("이미 계정이 있나요?  로그인", Modifier.clickable(onClick = onLogin).padding(6.dp), color = Colors.Mint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("로그인 없이 둘러보기", Modifier.clickable(onClick = onBrowse).padding(6.dp), color = Color.White, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onFindEmail: () -> Unit,
    onPasswordReset: () -> Unit,
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
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("로그인", onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 61.dp), contentScale = ContentScale.Fit)
            Text("dib 계정으로 경매를 계속해보세요", color = Colors.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            LoginField(
                "이메일",
                email,
                { email = it },
                "이메일을 입력해주세요",
                KeyboardType.Email,
                errorMessage = "올바른 이메일을 입력해주세요.".takeIf { attempted && !email.contains('@') }
            )
            Spacer(Modifier.height(14.dp))
            LoginField("비밀번호", password, { password = it }, "비밀번호를 입력해주세요", KeyboardType.Password, passwordVisible, errorMessage = "비밀번호를 4자 이상 입력해주세요.".takeIf { attempted && password.length < 4 }) {
                passwordVisible = !passwordVisible
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("이메일 찾기", Modifier.clickable(onClick = onFindEmail).padding(4.dp), color = Colors.Muted, fontSize = 12.sp)
                Text("·", color = Colors.Muted, fontSize = 12.sp)
                Text("비밀번호 찾기", Modifier.clickable(onClick = onPasswordReset).padding(4.dp), color = Colors.Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "회원가입",
                    Modifier.clickable(onClick = onSignUp).padding(4.dp),
                    color = Colors.Navy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            errorMessage?.let {
                Text(it, Modifier.fillMaxWidth().padding(top = 8.dp), color = Colors.Urgent, fontSize = 11.sp)
            }
            AuthPrimaryButton(
                text = "로그인",
                enabled = valid,
                loading = isLoading,
                onClick = { attempted = true; if (valid) onLogin(email.trim(), password) },
                modifier = Modifier.padding(top = 24.dp)
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD1D6DE))
                Text("또는", Modifier.padding(horizontal = 20.dp), color = Colors.Muted, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFFD1D6DE))
            }
            Button(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color(0xFF17140F), disabledContainerColor = Color(0xFFF3E787), disabledContentColor = Color(0xFF6F681F))) {
                Text("카카오로 로그인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("카카오 로그인은 서버 연동 후 사용할 수 있어요", Modifier.padding(top = 18.dp), color = Colors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    visible: Boolean = true,
    errorMessage: String? = null,
    onVisibility: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            placeholder = { Text(placeholder, color = Color(0xFF8C919C), fontSize = 14.sp) },
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (keyboardType == KeyboardType.Password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = onVisibility?.let { action -> ({ Text(if (visible) "◉" else "◎", Modifier.clickable(onClick = action), color = Colors.Muted) }) },
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Colors.Navy,
                unfocusedBorderColor = Colors.Border,
                focusedContainerColor = Colors.Background,
                unfocusedContainerColor = Colors.Background,
                errorBorderColor = Colors.Urgent
            )
        )
        errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
    }
}
