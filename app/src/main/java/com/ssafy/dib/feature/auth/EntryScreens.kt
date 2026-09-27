package com.ssafy.dib.feature.auth

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.ssafy.dib.R
import com.ssafy.dib.core.ui.DibSnackbarHost
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay

private val WelcomeCanvas = Color.White

@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        delay(1_300)
        onFinished()
    }
    Box(
        modifier.fillMaxSize().background(Colors.Background)
    ) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(.88f).aspectRatio(520f / 579f)
            )
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.dib_official_logo), "dib", Modifier.size(96.dp, 60.dp), contentScale = ContentScale.Fit)
            Text("경매의 순간을 잡다", style = MaterialTheme.typography.titleMedium, color = Colors.Navy)
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(88.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Colors.Mint,
                trackColor = Colors.Border
            )
        }
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    setImageResource(R.mipmap.ic_launcher)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = "dib"
                }
            },
            modifier = Modifier.align(Alignment.Center).size(160.dp)
        )
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
    modifier: Modifier = Modifier
) {
    val toastHost = remember { SnackbarHostState() }
    LaunchedEffect(kakaoLoginError) {
        kakaoLoginError?.takeIf(String::isNotBlank)?.let { toastHost.showSnackbar(it) }
    }
    // 원본 로고와 제목 PNG의 투명 여백만 화면에서 잘라 사용한다.
    val logoBitmap = ImageBitmap.imageResource(R.drawable.dib_official_logo)
    val headlineBitmap = ImageBitmap.imageResource(R.drawable.welcome_headline)
    val logoPainter = remember(logoBitmap) { BitmapPainter(logoBitmap, IntOffset(31, 32), IntSize(339, 200)) }
    val headlinePainter = remember(headlineBitmap) { BitmapPainter(headlineBitmap, IntOffset(83, 247), IntSize(1538, 471)) }
    val headlineRatio = 1538f / 471f
    BoxWithConstraints(modifier.fillMaxSize().background(WelcomeCanvas).safeDrawingPadding()) {
        val compact = maxHeight < 790.dp || LocalDensity.current.fontScale > 1.1f
        val contentWidth = maxWidth.coerceAtMost(430.dp)
        Column(
            Modifier.align(Alignment.TopCenter).width(contentWidth).fillMaxHeight()
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                logoPainter,
                contentDescription = "dib",
                modifier = Modifier.align(Alignment.Start).padding(top = if (compact) 8.dp else 16.dp)
                    .size(76.dp, 45.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(if (compact) 18.dp else 26.dp))
            if (!compact) Spacer(Modifier.weight(0.5f))
            Image(
                headlinePainter,
                contentDescription = "마음에 드는 물건을 경매에서 만나보세요",
                modifier = Modifier.fillMaxWidth().aspectRatio(headlineRatio),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(if (compact) 14.dp else 20.dp))
            if (!compact) Spacer(Modifier.weight(0.5f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WelcomeBenefit("실시간 경매", R.drawable.welcome_bolt, Color(0xFFE7F9F1), Modifier.weight(1f))
                WelcomeBenefit("간편 참여", R.drawable.welcome_touch, Color(0xFFE9F2FF), Modifier.weight(1f))
                WelcomeBenefit("관심 상품 알림", R.drawable.welcome_bell, Color(0xFFFFF1E7), Modifier.weight(1f))
            }
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            if (!compact) Spacer(Modifier.weight(0.25f))
            Image(
                painterResource(R.drawable.welcome_auction_3d_scene),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1200f / 799f),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(4.dp))
            if (!compact) Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x1A8C572F))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFF5F0EC), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeActionButton("이메일로 시작하기", R.drawable.mail_filled, Colors.Navy, Color.White, onLogin)
                Spacer(Modifier.height(10.dp))
                WelcomeActionButton("카카오로 시작하기", R.drawable.welcome_chat, Color(0xFFFFE500), Color(0xFF252525), onKakaoLogin, !kakaoLoginLoading)
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = Colors.Border)
                    Text("빠르게 시작해 보세요", color = Colors.Muted, fontSize = 12.sp)
                    HorizontalDivider(Modifier.weight(1f), color = Colors.Border)
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    WelcomeTextAction("회원가입", onEmailSignup, Modifier.weight(1f))
                    WelcomeTextAction("둘러보기", onBrowse, Modifier.weight(1f), "로그인 없이 둘러보기")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        DibSnackbarHost(toastHost, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun WelcomeBenefit(label: String, icon: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.height(38.dp).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(tint, Color.White)))
            .border(1.dp, Color(0xFFF0F1F3), RoundedCornerShape(50))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Colors.Navy, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun WelcomeActionButton(
    label: String,
    icon: Int,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(11.dp))
            .background(background).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = if (icon == R.drawable.mail_filled) foreground else Color.Unspecified, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WelcomeTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
    ) {
        Text(label, color = Colors.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("  ›", color = Colors.Navy, fontSize = 22.sp)
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
    loading: Boolean = false,
    showIcon: Boolean = false
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
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showIcon) Icon(painterResource(R.drawable.mail_filled), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, fontSize = if (showIcon) 13.sp else 15.sp, fontWeight = FontWeight.Medium)
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
    fun submitLogin() {
        if (isLoading) return
        attempted = true
        if (valid) onLogin(email.trim(), password)
    }
    val toastHost = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.takeIf(String::isNotBlank)?.let { toastHost.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("로그인", onBack) },
        snackbarHost = { DibSnackbarHost(toastHost, Modifier.imePadding()) }
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
            LoginField("비밀번호", password, { password = it }, "비밀번호를 입력해주세요", KeyboardType.Password, passwordVisible, errorMessage = "비밀번호를 4자 이상 입력해주세요.".takeIf { attempted && password.length < 4 }, onDone = ::submitLogin) {
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
            Spacer(Modifier.height(16.dp))
            EmailLoginButton(
                text = "로그인",
                loading = isLoading,
                onClick = ::submitLogin
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
    onDone: (() -> Unit)? = null,
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
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
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
