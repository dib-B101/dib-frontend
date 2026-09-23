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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

private val WelcomeCanvas = Color(0xFFFCF9F4)

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
    onKakaoLogin: () -> Unit,
    onBrowse: () -> Unit,
    kakaoLoginLoading: Boolean,
    kakaoLoginError: String?,
    showDeveloperPreview: Boolean,
    onDeveloperPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mascotPainter = painterResource(R.drawable.welcome_mascot)
    val mascotRatio = mascotPainter.intrinsicSize.let { size ->
        if (size.isSpecified && size.height > 0f) size.width / size.height else 1f
    }
    Column(
        modifier.fillMaxSize().background(WelcomeCanvas).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painterResource(R.drawable.dib_primary_logo),
                    "dib",
                    Modifier.size(76.dp, 48.dp),
                    contentScale = ContentScale.Fit
                )
                if (showDeveloperPreview) {
                    TextButton(onClick = onDeveloperPreview) {
                        Text("샘플 화면 · 개발용", style = MaterialTheme.typography.labelSmall, color = Colors.Muted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "마음에 드는 물건,\n경매에서 만나보세요",
                style = MaterialTheme.typography.headlineSmall,
                color = Colors.Text
            )
            Text(
                "일반 경매부터 라이브 경매까지,\n둘러보고 원하는 물건에 입찰해 보세요.",
                Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.Muted
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                color = WelcomeCanvas,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(mascotRatio)
            ) {
                Image(
                    mascotPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KakaoLoginButton(onClick = onKakaoLogin, enabled = !kakaoLoginLoading)
            kakaoLoginError?.let {
                Text(it, Modifier.fillMaxWidth(), color = Colors.Urgent, style = MaterialTheme.typography.bodyMedium)
            }
            EmailLoginButton(text = "이메일로 로그인", onClick = onLogin)
            Row(
                Modifier.widthIn(max = 336.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEmailSignup, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("회원가입", style = MaterialTheme.typography.labelLarge, color = Colors.Navy)
                }
                TextButton(
                    onClick = onBrowse,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "로그인 없이 둘러보기" }
                ) {
                    Text("둘러보기", style = MaterialTheme.typography.labelLarge, color = Colors.Navy)
                }
            }
        }
    }
}

@Composable
private fun KakaoLoginButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 54.dp)
            .semantics { contentDescription = "카카오 로그인" }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.kakao_login_kr_large),
            contentDescription = null,
            modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth().aspectRatio(336f / 46f),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun EmailLoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.widthIn(max = 336.dp).fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Colors.Navy,
            contentColor = Color.White,
            disabledContainerColor = Colors.Navy,
            disabledContentColor = Color.White
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.dib_primary_logo), "dib", Modifier.size(96.dp, 61.dp), contentScale = ContentScale.Fit)
            Text("로그인하고 경매를 이어가세요.", color = Colors.Muted, fontSize = 14.sp)
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
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFindEmail) {
                    Text("이메일 찾기", color = Colors.Muted, fontSize = 13.sp)
                }
                Text("·", color = Colors.Muted, fontSize = 13.sp)
                TextButton(onClick = onPasswordReset) {
                    Text("비밀번호 찾기", color = Colors.Muted, fontSize = 13.sp)
                }
            }
            errorMessage?.let {
                Text(it, Modifier.fillMaxWidth().padding(top = 8.dp), color = Colors.Urgent, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            EmailLoginButton(
                text = "로그인",
                loading = isLoading,
                onClick = { attempted = true; if (valid) onLogin(email.trim(), password) }
            )
            Spacer(Modifier.height(8.dp))
            KakaoLoginButton(onClick = onKakaoLogin, enabled = !isLoading)
            Row(
                Modifier.padding(top = 12.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("아직 계정이 없으신가요?", style = MaterialTheme.typography.bodyMedium, color = Colors.Muted)
                TextButton(onClick = onSignUp) {
                    Text("회원가입", style = MaterialTheme.typography.labelLarge, color = Colors.Navy)
                }
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
            // 높이를 56dp 로 묶으면 글자 크기 배율에 따라 안쪽 글자가 잘린다. 최소 높이만 둔다
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
