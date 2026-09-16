package com.ssafy.dib.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.R
import com.ssafy.dib.ui.theme.WireframeColors as Colors

@Composable
internal fun AuthTopBar(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Colors.Background)) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                Image(
                    painterResource(R.drawable.back),
                    contentDescription = "뒤로",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Colors.Text)
                )
            }
            Text(title, color = Colors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = Colors.Border)
    }
}

@Composable
internal fun AuthPageTitle(title: String, description: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Colors.Text, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Text(description, Modifier.padding(top = 8.dp), color = Colors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = !loading,
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Colors.Navy else Colors.Border,
            contentColor = if (enabled) Color.White else Colors.Muted,
            disabledContainerColor = Colors.Border,
            disabledContentColor = Colors.Muted
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
