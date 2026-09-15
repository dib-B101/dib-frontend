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
        containerColor = Color(0xFFFCFBF7),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(48.dp).background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("←", Modifier.size(48.dp).clickable(onClick = onBack).padding(14.dp, 8.dp), fontSize = 22.sp)
                Text("비밀번호 재설정", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (isComplete) "비밀번호를 변경했어요" else "새 비밀번호를 입력해주세요",
                color = Colors.Navy,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isComplete) "새 비밀번호로 다시 로그인해주세요." else "대·소문자, 숫자, 특수문자를 포함해 10~64자로 설정해주세요.",
                Modifier.padding(top = 8.dp),
                color = Colors.Muted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(28.dp))

            if (isComplete) {
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("로그인으로 돌아가기", fontWeight = FontWeight.Bold) }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("새 비밀번호") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
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
                    isError = (attempted || confirmation.isNotEmpty()) && !matches,
                    supportingText = if ((attempted || confirmation.isNotEmpty()) && !matches) ({ Text("비밀번호가 일치하지 않아요.") }) else null
                )
                errorMessage?.let {
                    Text(it, Modifier.fillMaxWidth().padding(top = 10.dp), color = Colors.Urgent, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        attempted = true
                        if (validPassword && matches) onSubmit(password)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (validPassword && matches) Colors.Navy else Color(0xFFD6DBE3),
                        contentColor = if (validPassword && matches) Color.White else Color(0xFF8C94A1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("비밀번호 변경", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
