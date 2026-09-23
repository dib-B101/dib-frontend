package com.ssafy.dib.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WireframeColors.Mint,
    secondary = WireframeColors.Live,
    tertiary = WireframeColors.Urgent,
    background = WireframeColors.Text,
    surface = WireframeColors.Navy,
    onPrimary = WireframeColors.Navy,
    onBackground = WireframeColors.Background,
    onSurface = WireframeColors.Background
)

private val LightColorScheme = lightColorScheme(
    primary = WireframeColors.Navy,
    secondary = WireframeColors.MintInk,
    tertiary = WireframeColors.Urgent,
    background = WireframeColors.Canvas,
    surface = WireframeColors.Background,
    surfaceVariant = WireframeColors.Surface,
    onPrimary = WireframeColors.Background,
    onBackground = WireframeColors.Text,
    onSurface = WireframeColors.Text,
    outline = WireframeColors.Border,
    error = WireframeColors.Live

    /* Other default colors to override
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    */
)

@Composable
fun DibTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 앱 전체 글자를 한 단계 키운다. 화면마다 10~12sp 로 박힌 글자가 많아 전체적으로 작다는 QA 가 있었다.
    // 화면을 하나씩 고치는 대신 시스템 글자 크기 설정에 배율을 곱한다(사용자가 키운 설정도 그대로 반영된다).
    // 대화상자·바텀시트도 같은 CompositionLocal 을 물려받는다
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * DIB_FONT_SCALE)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private const val DIB_FONT_SCALE = 1.1f
