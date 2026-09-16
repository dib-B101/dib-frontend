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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun PasswordResetScreen(
    isLoading: Boolean,
    isComplete: Boolean,
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val validPassword = SignupValidator.isPasswordValid(password)
    val matches = password == confirmation

    Scaffold(
        modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = Colors.Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AuthTopBar("비밀번호 재설정", onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AuthPageTitle(
                if (isComplete) "비밀번호를 변경했어요" else "새 비밀번호를\n입력해주세요",
                if (isComplete) "새 비밀번호로 다시 로그인해주세요." else "대·소문자, 숫자, 특수문자를 포함해 10~64자로 설정해주세요."
            )
            Spacer(Modifier.height(28.dp))

            if (isComplete) {
                AuthPrimaryButton("로그인으로 돌아가기", true, false, onLogin)
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 비밀번호") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(15.dp),
                    isError = attempted && !validPassword,
                    supportingText = if (attempted && !validPassword) ({ Text("비밀번호 조건을 확인해주세요.") }) else null
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.take(64) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("새 비밀번호 확인") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(15.dp),
                    isError = (attempted || confirmation.isNotEmpty()) && !matches,
                    supportingText = if ((attempted || confirmation.isNotEmpty()) && !matches) ({ Text("비밀번호가 일치하지 않아요.") }) else null
                )
                errorMessage?.let {
                    Text(it, Modifier.fillMaxWidth().padding(top = 10.dp), color = Colors.Urgent, fontSize = 12.sp)
                }
                AuthPrimaryButton(
                    text = "비밀번호 변경",
                    enabled = validPassword && matches,
                    loading = isLoading,
                    onClick = {
                        attempted = true
                        if (validPassword && matches) onSubmit(password)
                    },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}
