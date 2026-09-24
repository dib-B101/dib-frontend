package com.ssafy.dib.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class SignupUiState(
    val phoneRequestLoading: Boolean = false,
    val phoneConfirmationLoading: Boolean = false,
    val verificationRequestKey: String? = null,
    val requestedPhone: String? = null,
    val retryAfterSeconds: Long = 0,
    val phoneVerified: Boolean = false,
    val phoneError: String? = null,
    val emailCheckLoading: Boolean = false,
    val checkedEmail: String? = null,
    val emailAvailable: Boolean? = null,
    val emailError: String? = null,
    val signupLoading: Boolean = false,
    val signupError: String? = null
)

data class SignupForm(
    val phoneNumber: String,
    val email: String,
    val password: String,
    val name: String,
    val nickname: String,
    val gender: String,
    val birthDate: String
)

enum class SignupMode { EMAIL, KAKAO }

object SignupValidator {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val phonePattern = Regex("010\\d{8}")
    private val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\p{Punct}).{10,64}$")
    private val punctuationPattern = Regex("\\p{Punct}")

    fun isPhoneValid(value: String) = phonePattern.matches(value)
    fun isCodeValid(value: String) = value.length == 6 && value.all(Char::isDigit)
    fun isEmailValid(value: String) = value.trim().length <= 255 && emailPattern.matches(value.trim())
    fun passwordRequirements(value: String): List<PasswordRequirement> = listOf(
        PasswordRequirement("10~64자", value.length in 10..64),
        PasswordRequirement("영문 대문자", value.any { it in 'A'..'Z' }),
        PasswordRequirement("영문 소문자", value.any { it in 'a'..'z' }),
        PasswordRequirement("숫자", value.any { it in '0'..'9' }),
        PasswordRequirement("특수문자", value.any { it.toString().matches(punctuationPattern) })
    )

    fun isPasswordValid(value: String) = passwordPattern.matches(value)
    fun isNameValid(value: String) = value.trim().length in 2..10
    fun isNicknameValid(value: String) = value.trim().length in 2..20
    fun isBirthDateValid(value: String): Boolean = try {
        LocalDate.parse(value)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    fun isFormValid(form: SignupForm) =
        isPhoneValid(form.phoneNumber) &&
            isEmailValid(form.email) &&
            isPasswordValid(form.password) &&
            isNameValid(form.name) &&
            isNicknameValid(form.nickname) &&
            isBirthDateValid(form.birthDate) &&
            form.gender in setOf("MALE", "FEMALE")

    fun isKakaoFormValid(form: SignupForm) =
        isPhoneValid(form.phoneNumber) &&
            isEmailValid(form.email) &&
            isNameValid(form.name) &&
            isNicknameValid(form.nickname) &&
            isBirthDateValid(form.birthDate) &&
            form.gender in setOf("MALE", "FEMALE")
}

data class PasswordRequirement(val label: String, val satisfied: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    state: SignupUiState,
    onBack: () -> Unit,
    onRequestPhoneVerification: (String) -> Unit,
    onConfirmPhoneVerification: (String) -> Unit,
    onCheckEmail: (String) -> Unit,
    onSignUp: (SignupForm) -> Unit,
    mode: SignupMode = SignupMode.EMAIL,
    initialNickname: String = "",
    modifier: Modifier = Modifier
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable(initialNickname) { mutableStateOf(initialNickname) }
    var gender by rememberSaveable { mutableStateOf("") }
    var birthDateDigits by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var accountAttempted by rememberSaveable { mutableStateOf(false) }
    var profileAttempted by rememberSaveable { mutableStateOf(false) }
    var retryRemaining by rememberSaveable { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val passwordConfirmFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    val nicknameFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val birthDateFocus = remember { FocusRequester() }
    val genderFocus = remember { FocusRequester() }
    val emailView = remember { BringIntoViewRequester() }
    val passwordView = remember { BringIntoViewRequester() }
    val passwordConfirmView = remember { BringIntoViewRequester() }
    val phoneView = remember { BringIntoViewRequester() }
    val codeView = remember { BringIntoViewRequester() }
    val nicknameView = remember { BringIntoViewRequester() }
    val nameView = remember { BringIntoViewRequester() }
    val birthDateView = remember { BringIntoViewRequester() }
    val genderView = remember { BringIntoViewRequester() }

    fun focusAndReveal(focusRequester: FocusRequester, viewRequester: BringIntoViewRequester) {
        focusRequester.requestFocus()
        coroutineScope.launch { viewRequester.bringIntoView() }
    }

    LaunchedEffect(step) { scrollState.scrollTo(0) }

    LaunchedEffect(state.verificationRequestKey, state.retryAfterSeconds) {
        retryRemaining = state.retryAfterSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        while (retryRemaining > 0) {
            delay(1_000)
            retryRemaining--
        }
    }

    val form = SignupForm(
        phoneNumber = phone,
        email = email.trim(),
        password = password,
        name = name.trim(),
        nickname = nickname.trim(),
        gender = gender,
        birthDate = formatBirthDate(birthDateDigits)
    )
    val phoneConfirmed = state.phoneVerified && state.requestedPhone == phone
    val emailConfirmed = mode == SignupMode.KAKAO ||
        (state.emailAvailable == true && state.checkedEmail == email.trim())
    val passwordMatches = password == passwordConfirm
    val formValid = if (mode == SignupMode.KAKAO) SignupValidator.isKakaoFormValid(form)
        else SignupValidator.isFormValid(form)
    val canSubmit = phoneConfirmed && emailConfirmed &&
        (mode == SignupMode.KAKAO || passwordMatches) && formValid
    val accountValid = SignupValidator.isEmailValid(email) && emailConfirmed &&
        (mode == SignupMode.KAKAO || (SignupValidator.isPasswordValid(password) && passwordMatches && passwordConfirm.isNotEmpty()))
    val profileValid = SignupValidator.isNameValid(name) && SignupValidator.isNicknameValid(nickname) &&
        SignupValidator.isBirthDateValid(form.birthDate) && gender in setOf("MALE", "FEMALE")
    val emailValid = SignupValidator.isEmailValid(email)
    val checkedEmailMatches = state.checkedEmail == email.trim()
    val emailFieldError = when {
        accountAttempted && !emailValid -> "이메일 형식을 확인해주세요."
        mode == SignupMode.EMAIL && checkedEmailMatches && state.emailError != null -> state.emailError
        mode == SignupMode.EMAIL && checkedEmailMatches && state.emailAvailable == false -> "이미 사용 중인 이메일이에요."
        else -> null
    }
    val stepTitle = when (step) {
        0 -> "계정 정보"
        1 -> "휴대폰 인증"
        else -> "기본 정보"
    }

    BackHandler(enabled = state.signupLoading) { }

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopBar(if (mode == SignupMode.KAKAO) "카카오 회원가입" else "이메일 회원가입") {
                if (state.signupLoading) return@AuthTopBar
                if (step > 0) step-- else onBack()
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Colors.Canvas).imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                when {
                    step == 0 -> AuthPrimaryButton(text = "다음", enabled = true, loading = false, onClick = {
                        accountAttempted = true
                        when {
                            !emailValid || !emailConfirmed -> focusAndReveal(emailFocus, emailView)
                            mode == SignupMode.EMAIL && !SignupValidator.isPasswordValid(password) ->
                                focusAndReveal(passwordFocus, passwordView)
                            mode == SignupMode.EMAIL && (passwordConfirm.isEmpty() || !passwordMatches) ->
                                focusAndReveal(passwordConfirmFocus, passwordConfirmView)
                            accountValid -> step = 1
                        }
                    })
                    step == 1 -> AuthPrimaryButton(text = "다음", enabled = true, loading = false, onClick = {
                        when {
                            phoneConfirmed -> step = 2
                            !SignupValidator.isPhoneValid(phone) || state.verificationRequestKey == null ||
                                state.requestedPhone != phone ->
                                focusAndReveal(phoneFocus, phoneView)
                            else -> focusAndReveal(codeFocus, codeView)
                        }
                    })
                    else -> AuthPrimaryButton(
                        text = "가입하고 시작하기",
                        enabled = true,
                        loading = state.signupLoading,
                        onClick = {
                            profileAttempted = true
                            when {
                                !SignupValidator.isNicknameValid(nickname) -> focusAndReveal(nicknameFocus, nicknameView)
                                !SignupValidator.isNameValid(name) -> focusAndReveal(nameFocus, nameView)
                                !SignupValidator.isBirthDateValid(form.birthDate) -> focusAndReveal(birthDateFocus, birthDateView)
                                gender !in setOf("MALE", "FEMALE") -> focusAndReveal(genderFocus, genderView)
                                !accountValid -> { accountAttempted = true; step = 0 }
                                !phoneConfirmed -> step = 1
                                profileValid && canSubmit -> onSignUp(form)
                            }
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            Modifier.fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("${step + 1} / 3", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(stepTitle, color = Colors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(when (step) {
                0 -> if (mode == SignupMode.KAKAO) "계정에 사용할 이메일을 입력해주세요." else "로그인에 사용할 이메일과 비밀번호를 입력해주세요."
                1 -> "휴대폰 번호를 인증해주세요."
                else -> "마지막으로 프로필 정보를 입력해주세요."
            }, color = Colors.Muted, fontSize = 13.sp)
            if (step == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    SignupField(
                        label = "휴대폰 번호",
                        value = phone,
                        onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                        placeholder = "숫자만 입력",
                        keyboardType = KeyboardType.Phone,
                        enabled = !phoneConfirmed,
                        focusRequester = phoneFocus,
                        bringIntoViewRequester = phoneView,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { onRequestPhoneVerification(phone) },
                        enabled = SignupValidator.isPhoneValid(phone) && !state.phoneRequestLoading && !phoneConfirmed && retryRemaining == 0,
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (state.phoneRequestLoading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        else Text(if (retryRemaining > 0) "${retryRemaining}초" else "인증 요청")
                    }
                }
                if (state.verificationRequestKey != null && !phoneConfirmed && state.requestedPhone == phone) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        SignupField(
                            label = "인증번호",
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            placeholder = "6자리 입력",
                            keyboardType = KeyboardType.Number,
                            focusRequester = codeFocus,
                            bringIntoViewRequester = codeView,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onConfirmPhoneVerification(code) },
                            enabled = SignupValidator.isCodeValid(code) && !state.phoneConfirmationLoading,
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                        ) {
                            if (state.phoneConfirmationLoading) CircularProgressIndicator(Modifier.height(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("확인")
                        }
                    }
                }
                if (phoneConfirmed) FeedbackText("휴대폰 인증이 완료됐어요.", success = true)
                state.phoneError?.let { FeedbackText(it) }
                if (!phoneConfirmed && state.phoneError == null) {
                    Text("인증이 완료되면 다음 단계로 넘어갈 수 있어요.", color = Colors.Muted, fontSize = 12.sp)
                }
            }
            if (step == 0) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SignupField(
                        label = "이메일",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "name@example.com",
                        keyboardType = KeyboardType.Email,
                        errorMessage = emailFieldError,
                        focusRequester = emailFocus,
                        bringIntoViewRequester = emailView,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mode == SignupMode.EMAIL) {
                        val buttonColor = when {
                            emailConfirmed -> Color(0xFF14866D)
                            emailValid -> Colors.Navy
                            else -> Colors.Muted
                        }
                        val buttonBackground = when {
                            emailConfirmed -> Color(0xFFEAF7F1)
                            emailValid -> Colors.Background
                            else -> Colors.Canvas
                        }
                        val buttonBorder = when {
                            emailConfirmed -> Color(0xFFA8DCC7)
                            emailValid -> Colors.Navy
                            else -> Colors.Border
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { onCheckEmail(email.trim()) },
                                enabled = emailValid && !state.emailCheckLoading && !emailConfirmed,
                                modifier = Modifier.widthIn(min = 120.dp).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, buttonBorder),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = buttonBackground,
                                    contentColor = buttonColor,
                                    disabledContainerColor = buttonBackground,
                                    disabledContentColor = buttonColor
                                )
                            ) {
                                when {
                                    state.emailCheckLoading -> Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(Modifier.size(16.dp), color = buttonColor, strokeWidth = 2.dp)
                                        Text("확인 중", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    emailConfirmed -> Text("✓ 확인 완료", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    else -> Text("중복 확인", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        Text("기존 계정 연결 시 가입한 이메일을 입력해주세요.", color = Colors.Muted, fontSize = 12.sp)
                    }
                }
                if (mode == SignupMode.EMAIL) {
                    SignupField(
                        "비밀번호",
                        password,
                        { password = it.take(64) },
                        "비밀번호 입력",
                        KeyboardType.Password,
                        password = true,
                        errorMessage = "비밀번호 조건을 확인해주세요.".takeIf { accountAttempted && !SignupValidator.isPasswordValid(password) },
                        passwordRequirements = SignupValidator.passwordRequirements(password),
                        focusRequester = passwordFocus,
                        bringIntoViewRequester = passwordView
                    )
                    SignupField(
                        "비밀번호 확인",
                        passwordConfirm,
                        { passwordConfirm = it },
                        "비밀번호 다시 입력",
                        KeyboardType.Password,
                        password = true,
                        errorMessage = "비밀번호가 일치하지 않아요.".takeIf { (accountAttempted || passwordConfirm.isNotEmpty()) && !passwordMatches },
                        focusRequester = passwordConfirmFocus,
                        bringIntoViewRequester = passwordConfirmView
                    )
                }
                if (accountAttempted && mode == SignupMode.EMAIL && emailValid && !emailConfirmed && emailFieldError == null) {
                    FeedbackText("이메일 중복 확인을 완료해주세요.")
                }
            }
            if (step == 2) {
                SignupField(
                    "닉네임", nickname, { nickname = it.take(20) }, "2~20자",
                    enabled = !state.signupLoading,
                    errorMessage = "닉네임은 2~20자로 입력해주세요.".takeIf {
                        profileAttempted && !SignupValidator.isNicknameValid(nickname)
                    },
                    focusRequester = nicknameFocus,
                    bringIntoViewRequester = nicknameView
                )
                SignupField(
                    "이름", name, { name = it.take(10) }, "실명을 입력해주세요",
                    enabled = !state.signupLoading,
                    errorMessage = "이름은 2~10자로 입력해주세요.".takeIf {
                        profileAttempted && !SignupValidator.isNameValid(name)
                    },
                    focusRequester = nameFocus,
                    bringIntoViewRequester = nameView
                )
                SignupField(
                    "생년월일",
                    birthDateDigits,
                    { birthDateDigits = it.filter(Char::isDigit).take(8) },
                    "YYYY-MM-DD",
                    KeyboardType.Number,
                    enabled = !state.signupLoading,
                    errorMessage = "생년월일을 확인해주세요.".takeIf { profileAttempted && !SignupValidator.isBirthDateValid(form.birthDate) },
                    visualTransformation = BirthDateVisualTransformation,
                    focusRequester = birthDateFocus,
                    bringIntoViewRequester = birthDateView
                )
                Text("성별", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.bringIntoViewRequester(genderView), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderButton("남성", "MALE", gender, { gender = it }, Modifier.weight(1f).focusRequester(genderFocus), enabled = !state.signupLoading)
                    GenderButton("여성", "FEMALE", gender, { gender = it }, Modifier.weight(1f), enabled = !state.signupLoading)
                }
                if (profileAttempted && gender !in setOf("MALE", "FEMALE")) FeedbackText("성별을 선택해주세요.")
                state.signupError?.let { FeedbackText(it) }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PasswordRequirementChecklist(
    requirements: List<PasswordRequirement>,
    hasInput: Boolean
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Colors.Background, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("비밀번호 조건", color = Colors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        requirements.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { requirement ->
                    val satisfied = hasInput && requirement.satisfied
                    Text(
                        "${if (satisfied) "✓" else "○"} ${requirement.label}",
                        modifier = Modifier.weight(1f),
                        color = if (satisfied) Color(0xFF14866D) else Colors.Muted,
                        fontSize = 11.sp,
                        fontWeight = if (satisfied) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignupField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    password: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    passwordRequirements: List<PasswordRequirement>? = null,
    focusRequester: FocusRequester? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val popupGap = with(density) { 8.dp.roundToPx() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                // 높이를 56dp 로 묶으면 글자 크기 배율에 따라 안쪽 글자가 잘린다. 최소 높이만 둔다
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .then(if (bringIntoViewRequester != null) Modifier.bringIntoViewRequester(bringIntoViewRequester) else Modifier)
                    .onSizeChanged { fieldWidth = it.width }
                    .onFocusChanged { focused = it.isFocused },
                placeholder = { Text(placeholder, color = Color(0xFF8C919C), fontSize = 13.sp) },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else visualTransformation,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Colors.Navy,
                    unfocusedBorderColor = Colors.Border,
                    focusedContainerColor = Colors.Background,
                    unfocusedContainerColor = Colors.Background,
                    errorBorderColor = Colors.Urgent
                )
            )
            if (focused && passwordRequirements != null && fieldWidth > 0) {
                Popup(
                    popupPositionProvider = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset(
                            x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                            y = (anchorBounds.top - popupContentSize.height - popupGap).coerceAtLeast(0)
                        )
                    },
                    onDismissRequest = { focused = false }
                ) {
                    Box(Modifier.width(with(density) { fieldWidth.toDp() }).shadow(8.dp, RoundedCornerShape(12.dp))) {
                        PasswordRequirementChecklist(passwordRequirements, value.isNotEmpty())
                    }
                }
            }
        }
        errorMessage?.let { FeedbackText(it) }
    }
}

@Composable
private fun GenderButton(
    label: String,
    value: String,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = { onSelected(value) },
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected == value) Colors.Navy else Color.Transparent,
            contentColor = if (selected == value) Color.White else Colors.Navy
        ),
        shape = RoundedCornerShape(14.dp)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun FeedbackText(message: String, success: Boolean = false) {
    Text(message, color = if (success) Color(0xFF14866D) else Colors.Urgent, fontSize = 12.sp)
}

private fun formatBirthDate(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 4 || index == 6) append('-')
            append(char)
        }
    }
}

internal object BirthDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatBirthDate(digits)
        return TransformedText(AnnotatedString(formatted), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + (if (digits.length > 4 && offset >= 4) 1 else 0) +
                    (if (digits.length > 6 && offset >= 6) 1 else 0)).coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count(Char::isDigit).coerceAtMost(digits.length)
        })
    }
}
