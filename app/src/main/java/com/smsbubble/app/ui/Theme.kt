package com.smsbubble.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.smsbubble.app.data.BubbleStyle

// Bảng màu kiểu iPhone
val IosBlueLight = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)
val IosGray = Color(0xFF8E8E93)
private val IosIncomingLight = Color(0xFFE9E9EB)
private val IosIncomingDark = Color(0xFF26252A)
private val IosBarLight = Color(0xFFF6F6F6)
private val IosBarDark = Color(0xFF1C1C1E)

@Composable
fun AppTheme(style: BubbleStyle, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        style == BubbleStyle.ANDROID && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        style == BubbleStyle.IOS ->
            if (dark) darkColorScheme(
                primary = IosBlueDark,
                onPrimary = Color.White,
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerHigh = IosBarDark,
            ) else lightColorScheme(
                primary = IosBlueLight,
                onPrimary = Color.White,
                background = Color.White,
                surface = Color.White,
                surfaceContainerHigh = IosBarLight,
            )

        else -> if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Màu dùng cho màn hình chat theo từng kiểu giao diện. */
@Immutable
data class BubblePalette(
    val outBg: Color,
    val outText: Color,
    val inBg: Color,
    val inText: Color,
    val chatBg: Color,
    val barBg: Color,
    val meta: Color,
    val accent: Color,
)

@Composable
fun bubblePalette(style: BubbleStyle): BubblePalette {
    val dark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme
    return when (style) {
        BubbleStyle.IOS -> if (dark) BubblePalette(
            outBg = IosBlueDark, outText = Color.White,
            inBg = IosIncomingDark, inText = Color.White,
            chatBg = Color.Black, barBg = IosBarDark,
            meta = IosGray, accent = IosBlueDark,
        ) else BubblePalette(
            outBg = IosBlueLight, outText = Color.White,
            inBg = IosIncomingLight, inText = Color.Black,
            chatBg = Color.White, barBg = IosBarLight,
            meta = IosGray, accent = IosBlueLight,
        )

        BubbleStyle.ANDROID -> BubblePalette(
            outBg = cs.primary, outText = cs.onPrimary,
            inBg = cs.surfaceContainerHigh, inText = cs.onSurface,
            chatBg = cs.surface, barBg = cs.surfaceContainer,
            meta = cs.onSurfaceVariant, accent = cs.primary,
        )
    }
}
