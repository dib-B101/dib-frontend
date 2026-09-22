package com.ssafy.dib.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 화면 하단 토스트. 폭을 화면에 꽉 채우지 않고 글자 길이에 맞춰 둥글게 감싼다. */
@Composable
fun DibToast(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            Modifier.background(Color(0xE6191F28), RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** Scaffold 의 snackbarHost 자리에 그대로 넣는 토스트 호스트. 기본 Snackbar 는 화면 폭을 다 채워 글이 짧아도 띠처럼 보였다. */
@Composable
fun DibSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> DibToast(data.visuals.message, Modifier.padding(bottom = 12.dp)) }
}
