package com.ssafy.dib.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
    Box(
        modifier.fillMaxSize().background(Colors.Canvas).safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val splashPainter = painterResource(R.drawable.splash_mascot)
            val splashRatio = splashPainter.intrinsicSize.let { size ->
                if (size.isSpecified && size.height > 0f) size.width / size.height else 1f
            }
            Surface(
                color = Colors.Background,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(.88f).aspectRatio(splashRatio)
            ) {
                Image(
                    splashPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 60.dp), contentScale = ContentScale.Fit)
            Text("경매의 순간을 잡다", style = MaterialTheme.typography.titleMedium, color = Colors.Navy)
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(88.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Colors.Mint,
                trackColor = Colors.Border
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onEmailSignup: () -> Unit,
    onLogin: () -> Unit,
    onBrowse: () -> Unit,
    showDeveloperPreview: Boolean,
    onDeveloperPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().background(Colors.Canvas).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            painterResource(R.drawable.dib_primary_logo),
            "dib",
            Modifier.padding(top = 10.dp).size(76.dp, 48.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "지금 가장 설레는 경매를\n놓치지 마세요",
            style = MaterialTheme.typography.headlineSmall,
            color = Colors.Text
        )
        Text(
            "라이브로 보고, 안전하게 참여하고,\n내 거래까지 한곳에서 관리해요.",
            Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Colors.Muted
        )
        val mascotPainter = painterResource(R.drawable.welcome_mascot)
        val mascotRatio = mascotPainter.intrinsicSize.let { size ->
            if (size.isSpecified && size.height > 0f) size.width / size.height else 1f
        }
        Surface(
            color = Colors.Navy,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).aspectRatio(mascotRatio)
        ) {
            Box {
                Image(
                    mascotPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    color = Colors.Background.copy(alpha = .94f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.size(7.dp).background(Colors.Urgent, RoundedCornerShape(50)))
                        Text("LIVE 경매 진행 중", style = MaterialTheme.typography.labelLarge, color = Colors.Text)
                    }
                }
            }
        }
        Surface(
            color = Colors.Background,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Colors.Border),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEmailSignup,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                ) {
                    Text("이메일로 시작하기", style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("이미 계정이 있나요?", style = MaterialTheme.typography.bodyMedium, color = Colors.Muted)
                    Text(
                        " 로그인",
                        Modifier.clickable(onClick = onLogin).padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Colors.Navy
                    )
                }
                TextButton(onClick = onBrowse, modifier = Modifier.fillMaxWidth()) {
                    Text("로그인 없이 먼저 둘러보기", style = MaterialTheme.typography.labelLarge, color = Colors.Muted)
                }
                if (showDeveloperPreview) {
                    HorizontalDivider(color = Colors.Border)
                    Surface(
                        color = Colors.NavySoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onDeveloperPreview)
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Text("개발 화면 둘러보기", style = MaterialTheme.typography.labelLarge, color = Colors.Navy)
                            Text("샘플 데이터로 전체 화면과 등록 흐름을 확인해요", style = MaterialTheme.typography.labelSmall, color = Colors.Muted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
    onKakaoLogin: () -> Unit,
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
            Button(onClick = onKakaoLogin, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color(0xFF17140F), disabledContainerColor = Color(0xFFF3E787), disabledContentColor = Color(0xFF6F681F))) {
                Text("카카오로 로그인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
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
            trailingIcon = onVisibility?.let { action -> ({ IconButton(onClick = action) { Image(painterResource(if (visible) R.drawable.visibility else R.drawable.visibility_off), if (visible) "비밀번호 숨기기" else "비밀번호 보기", Modifier.size(22.dp), colorFilter = ColorFilter.tint(Colors.Muted)) } }) },
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
