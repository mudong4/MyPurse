package com.wyd.mypurse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ========== 深色配色方案 ==========
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // 深色模式专用色：卡片/页面背景使用柔和深灰而非纯黑
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

// ========== 浅色配色方案 ==========
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = AppSurfaceBg,
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F2),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outlineVariant = AppDivider,
)

/**
 * 应用主题。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置。可由设置页传入覆盖。
 * @param dynamicColor 是否使用 Android 12+ 动态取色，默认关闭（避免覆盖自定义配色）。
 * @param chartColors 图表配色方案，默认使用 [DefaultChartColors]。
 */
@Composable
fun MyPurseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    chartColors: ChartColorScheme = DefaultChartColors,
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

    // 设置状态栏颜色与背景一致
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 应用级暗黑模式偏好，可由设置页改写。
 * 默认值为 null，表示跟随系统设置。
 */
val LocalDarkThemeOverride = staticCompositionLocalOf<Boolean?> { null }

/**
 * 图表配色方案偏好，可由设置页改写。
 * 默认使用 DefaultChartColors。
 */
val LocalChartColorScheme = staticCompositionLocalOf { DefaultChartColors }
