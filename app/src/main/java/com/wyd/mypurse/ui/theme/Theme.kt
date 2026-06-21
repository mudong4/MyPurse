package com.wyd.mypurse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ========== Material3 形状定义 ==========

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppCornerSmall),
    small = RoundedCornerShape(AppCornerSmall),
    medium = RoundedCornerShape(AppCornerMedium),
    large = RoundedCornerShape(AppCornerLarge),
    extraLarge = RoundedCornerShape(AppCornerLarge),
)

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

// ========== 主题预设体系（V1.0.1 — 为 V1.1 多主题扩展预留） ==========

/**
 * 主题预设：将配色方案 + 图表颜色打包为一个可切换单元。
 *
 * V1.0.1 仅提供 [DefaultPreset]，后续可扩展：
 * - WarmPreset（暖色调）
 * - CoolPreset（冷色调）
 * - MinimalPreset（极简灰）
 */
data class ThemePreset(
    val name: String,
    val lightScheme: androidx.compose.material3.ColorScheme = LightColorScheme,
    val darkScheme: androidx.compose.material3.ColorScheme = DarkColorScheme,
    val chartColors: ChartColorScheme = DefaultChartColors,
)

/** 默认主题预设 */
val DefaultPreset = ThemePreset(
    name = "默认",
    lightScheme = LightColorScheme,
    darkScheme = DarkColorScheme,
    chartColors = DefaultChartColors,
)

/** 柔和主题预设（备选） */
val SoftPreset = ThemePreset(
    name = "柔和",
    lightScheme = LightColorScheme,  // V1.1 可替换为独立柔和配色
    darkScheme = DarkColorScheme,
    chartColors = SoftChartColors,
)

/** 所有可用预设 */
val AllPresets = listOf(DefaultPreset, SoftPreset)

/**
 * 应用主题。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置。可由设置页传入覆盖。
 * @param dynamicColor 是否使用 Android 12+ 动态取色，默认关闭（避免覆盖自定义配色）。
 * @param preset 主题预设，默认 [DefaultPreset]。V1.1 可由设置页切换。
 */
@Composable
fun MyPurseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    preset: ThemePreset = DefaultPreset,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> preset.darkScheme
        else -> preset.lightScheme
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

    // 通过 CompositionLocal 向下传递当前预设的图表配色
    androidx.compose.runtime.CompositionLocalProvider(
        LocalChartColorScheme provides preset.chartColors,
        LocalThemePreset provides preset,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content
        )
    }
}

/**
 * 图表配色方案偏好，可由设置页改写。
 * 默认使用 DefaultChartColors。
 */
val LocalChartColorScheme = staticCompositionLocalOf { DefaultChartColors }

/**
 * 主题预设偏好，可由设置页改写。
 * V1.1 在设置页增加主题风格切换后使用。
 */
val LocalThemePreset = staticCompositionLocalOf { DefaultPreset }
