package com.ssafy.dib.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import kotlinx.coroutines.delay
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

object SignupValidator {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun isPhoneValid(value: String) = value.length in 10..11 && value.all(Char::isDigit)
    fun isCodeValid(value: String) = value.length == 6 && value.all(Char::isDigit)
    fun isEmailValid(value: String) = emailPattern.matches(value.trim())
    fun isPasswordValid(value: String) =
        value.length in 10..64 &&
            value.any(Char::isUpperCase) &&
            value.any(Char::isLowerCase) &&
            value.any(Char::isDigit) &&
            value.any { !it.isLetterOrDigit() }
    fun isNameValid(value: String) = value.trim().length in 2..30
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    state: SignupUiState,
    onBack: () -> Unit,
    onRequestPhoneVerification: (String) -> Unit,
    onConfirmPhoneVerification: (String) -> Unit,
    onCheckEmail: (String) -> Unit,
    onSignUp: (SignupForm) -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var retryRemaining by rememberSaveable { mutableIntStateOf(0) }

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
        birthDate = birthDate
    )
    val phoneConfirmed = state.phoneVerified && state.requestedPhone == phone
    val emailConfirmed = state.emailAvailable == true && state.checkedEmail == email.trim()
    val passwordMatches = password == passwordConfirm
    val canSubmit = phoneConfirmed && emailConfirmed && passwordMatches && SignupValidator.isFormValid(form)

    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("이메일 회원가입", onBack) }
    ) { contentPadding ->
        Column(
            Modifier.fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("휴대폰 인증", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("본인 명의의 휴대폰 번호를 인증해주세요.", color = Colors.Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                SignupField(
                    label = "휴대폰 번호",
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                    placeholder = "숫자만 입력",
                    keyboardType = KeyboardType.Phone,
                    enabled = !phoneConfirmed,
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

            Spacer(Modifier.height(8.dp))
            Text("계정 정보", color = Colors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                SignupField(
                    label = "이메일",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "name@example.com",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { onCheckEmail(email.trim()) },
                    enabled = SignupValidator.isEmailValid(email) && !state.emailCheckLoading,
                    modifier = Modifier.height(56.dp)
                ) {
                    if (state.emailCheckLoading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                    else Text("중복 확인")
                }
            }
            when {
                emailConfirmed -> FeedbackText("사용할 수 있는 이메일이에요.", success = true)
                state.checkedEmail == email.trim() && state.emailAvailable == false -> FeedbackText("이미 사용 중인 이메일이에요.")
                state.emailError != null -> FeedbackText(state.emailError)
            }
            SignupField(
                "비밀번호",
                password,
                { password = it.take(64) },
                "대·소문자, 숫자, 특수문자 포함 10자 이상",
                KeyboardType.Password,
                password = true,
                errorMessage = "비밀번호 조건을 확인해주세요.".takeIf { attempted && !SignupValidator.isPasswordValid(password) }
            )
            SignupField(
                "비밀번호 확인",
                passwordConfirm,
                { passwordConfirm = it },
                "비밀번호 다시 입력",
                KeyboardType.Password,
                password = true,
                errorMessage = "비밀번호가 일치하지 않아요.".takeIf { (attempted || passwordConfirm.isNotEmpty()) && !passwordMatches }
            )
            SignupField("이름", name, { name = it.take(30) }, "실명을 입력해주세요", errorMessage = "이름은 2~30자로 입력해주세요.".takeIf { attempted && !SignupValidator.isNameValid(name) })
            SignupField("닉네임", nickname, { nickname = it.take(20) }, "2~20자", errorMessage = "닉네임은 2~20자로 입력해주세요.".takeIf { attempted && !SignupValidator.isNicknameValid(nickname) })

            Text("성별", color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderButton("남성", "MALE", gender, { gender = it }, Modifier.weight(1f))
                GenderButton("여성", "FEMALE", gender, { gender = it }, Modifier.weight(1f))
            }
            if (attempted && gender !in setOf("MALE", "FEMALE")) FeedbackText("성별을 선택해주세요.")
            SignupField(
                "생년월일",
                birthDate,
                { birthDate = formatBirthDate(it) },
                "YYYY-MM-DD",
                KeyboardType.Number,
                errorMessage = "생년월일을 확인해주세요.".takeIf { attempted && !SignupValidator.isBirthDateValid(birthDate) }
            )

            if (attempted && !canSubmit) {
                when {
                    !phoneConfirmed -> FeedbackText("휴대폰 인증을 완료해주세요.")
                    !emailConfirmed -> FeedbackText("이메일 중복 확인을 완료해주세요.")
                    else -> FeedbackText("필수 입력값을 모두 확인해주세요.")
                }
            }
            state.signupError?.let { FeedbackText(it) }
            AuthPrimaryButton(
                text = "가입하고 시작하기",
                enabled = canSubmit,
                loading = state.signupLoading,
                onClick = { attempted = true; if (canSubmit) onSignUp(form) }
            )
            Spacer(Modifier.height(18.dp))
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
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Colors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            placeholder = { Text(placeholder, color = Color(0xFF8C919C), fontSize = 13.sp) },
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Colors.Navy,
                unfocusedBorderColor = Colors.Border,
                focusedContainerColor = Colors.Background,
                unfocusedContainerColor = Colors.Background,
                errorBorderColor = Colors.Urgent
            )
        )
        errorMessage?.let { FeedbackText(it) }
    }
}

@Composable
private fun GenderButton(
    label: String,
    value: String,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = { onSelected(value) },
        modifier = modifier.height(48.dp),
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
