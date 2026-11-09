package com.example.sample.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC5D0F5),
    onPrimary = Color(0xFF1B2559),
    secondary = Color(0xFF9AA3B5),
    background = Color(0xFF10141C),
    onBackground = Color(0xFFE8ECF4),
    surface = Color(0xFF1C2333),
    onSurface = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF9AA3B5),
    surfaceVariant = Color(0xFF2A3348),
    error = Color(0xFFFFB4AB),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B2559),
    onPrimary = Color.White,
    secondary = Color(0xFF5C6578),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF1B2559),
    surface = Color.White,
    onSurface = Color(0xFF1B2559),
    onSurfaceVariant = Color(0xFF8B93A7),
    surfaceVariant = Color(0xFFE8EEF9),
    error = Color(0xFFE11D48),
)

@Composable
fun SampleTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}