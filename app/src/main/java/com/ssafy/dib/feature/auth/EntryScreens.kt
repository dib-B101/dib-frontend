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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.ssafy.dib.R
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
    // 원본 PNG의 넓은 투명 가장자리를 화면에서만 잘라 세 이미지의 실제 그림 간격을 맞춘다.
    val logoBitmap = ImageBitmap.imageResource(R.drawable.dib_official_logo)
    val headlineBitmap = ImageBitmap.imageResource(R.drawable.welcome_headline)
    val mascotBitmap = ImageBitmap.imageResource(R.drawable.welcome_auction_mint_scene)
    val logoPainter = remember(logoBitmap) { BitmapPainter(logoBitmap, IntOffset(31, 32), IntSize(339, 200)) }
    val headlinePainter = remember(headlineBitmap) { BitmapPainter(headlineBitmap, IntOffset(83, 247), IntSize(1538, 471)) }
    val mascotPainter = remember(mascotBitmap) { BitmapPainter(mascotBitmap, IntOffset(31, 160), IntSize(1296, 954)) }
    val headlineRatio = 1538f / 471f
    val mascotRatio = 1296f / 954f
    var actionRise by remember { mutableStateOf(0.dp) }
    Column(
        modifier.fillMaxSize().background(WelcomeCanvas).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val imageWidth = ((maxHeight - 48.dp).coerceAtLeast(0.dp) /
                (1f / headlineRatio + 1f / mascotRatio)).coerceAtMost(maxWidth)
            val heroHeight = imageWidth * 0.9f / headlineRatio + 16.dp + imageWidth / mascotRatio
            val heroBottomGap = (maxHeight - heroHeight) / 2f + 16.dp
            val desiredActionRise = (heroBottomGap + 8.dp - 40.dp).coerceIn(0.dp, 88.dp)
            LaunchedEffect(desiredActionRise) { actionRise = desiredActionRise }
            Image(
                logoPainter,
                contentDescription = "dib",
                modifier = Modifier.align(Alignment.TopStart).padding(top = 18.dp).size(76.dp, 45.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                Modifier.align(Alignment.Center).offset(y = (-16).dp).width(imageWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    headlinePainter,
                    contentDescription = "마음에 드는 물건을 경매에서 만나보세요",
                    modifier = Modifier.align(Alignment.Start).width(imageWidth * 0.9f).aspectRatio(headlineRatio),
                    contentScale = ContentScale.Fit
                )
                Image(
                    mascotPainter,
                    contentDescription = null,
                    modifier = Modifier.width(imageWidth).aspectRatio(mascotRatio),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().offset(y = -actionRise).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmailLoginButton(text = "이메일로 로그인", onClick = onLogin, showIcon = true)
            KakaoLoginButton(onClick = onKakaoLogin, enabled = !kakaoLoginLoading)
            kakaoLoginError?.let {
                Text(it, Modifier.fillMaxWidth(), color = Colors.Urgent, style = MaterialTheme.typography.bodyMedium)
            }
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
