package com.ssafy.dib.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/**
 * 앱 공통 모달. 모서리 24dp·흰 배경·굵은 제목으로 통일한다.
 * 문의 작성·문의 상세·배송지·정산 계좌처럼 화면마다 다르게 생겼던 모달이 같은 틀을 쓰게 한 곳에 둔다.
 * 버튼은 DibDialogConfirmButton / DibDialogDismissButton 을 쓴다.
 */
@Composable
fun DibDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    text: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = { Text(title, color = Colors.Text, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold) },
        text = text,
        shape = RoundedCornerShape(24.dp),
        containerColor = Colors.Background,
        titleContentColor = Colors.Text,
        textContentColor = Colors.Text,
        properties = properties
    )
}

/** 모달의 확인 버튼. 기본은 남색, 삭제·탈퇴처럼 되돌리기 어려운 동작은 destructive 로 코랄색. */
@Composable
fun DibDialogConfirmButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) Colors.Urgent else Colors.Navy,
            contentColor = Color.White,
            disabledContainerColor = Colors.Surface,
            disabledContentColor = Colors.Muted
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DibDialogDismissButton(onClick: () -> Unit, label: String = "취소", enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled) { Text(label, color = Colors.Muted, fontWeight = FontWeight.SemiBold) }
}
