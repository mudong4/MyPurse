package com.wyd.mypurse.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * V1.1 预设主题色值定义。
 *
 * 每一套预设包含：
 * - 名称（用于 UI 展示和 DataStore 持久化）
 * - 浅色方案（Material3 ColorScheme）
 * - 深色方案（Material3 ColorScheme）
 * - 图表配色方案（ChartColorScheme）
 *
 * 注意：AppColors 功能语义色（支出红、收入绿、预算色等）不随预设变化。
 */

// =====================================================================
// 1. 默认紫（Default Purple）— 新用户默认
// =====================================================================
private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6650A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3EDF7),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE4EC),
    onTertiaryContainer = Color(0xFF4A152C),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF49454F),
    outlineVariant = Color(0xFFE0E0E0),
)

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFF3EDF7),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFCE4EC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outlineVariant = Color(0xFF3A3A3A),
)

private val PurpleCharts = ChartColorScheme(
    chartPrimary = Color(0xFF7C4DFF),
    chartAccent = Color(0xFFFF7043),
    chartPalette = listOf(
        Color(0xFF7C4DFF), Color(0xFF43A047), Color(0xFFFF7043),
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF9E9E9E),
    ),
    chartPaletteExtended = listOf(
        Color(0xFF7C4DFF), Color(0xFF43A047), Color(0xFFFF7043),
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF00ACC1),
        Color(0xFFEC407A), Color(0xFF3949AB), Color(0xFFFF8A65),
        Color(0xFF9E9E9E),
    ),
)

// =====================================================================
// 2. 清新绿（Fresh Green）
// =====================================================================
private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF2E7D32),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF00695C),
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8EDE8),
    onSurfaceVariant = Color(0xFF44483D),
    outlineVariant = Color(0xFFC0C4C0),
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF004D40),
    tertiaryContainer = Color(0xFF00695C),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1B241B),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C332C),
    onSurfaceVariant = Color(0xFFA0A8A0),
    outlineVariant = Color(0xFF3A423A),
)

private val GreenCharts = ChartColorScheme(
    chartPrimary = Color(0xFF43A047),
    chartAccent = Color(0xFFFF7043),
    chartPalette = listOf(
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFF7043),
        Color(0xFF8E24AA), Color(0xFFE53935), Color(0xFF9E9E9E),
    ),
    chartPaletteExtended = listOf(
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFF7043),
        Color(0xFF8E24AA), Color(0xFFE53935), Color(0xFF00ACC1),
        Color(0xFFEC407A), Color(0xFF66BB6A), Color(0xFFFF8A65),
        Color(0xFF9E9E9E),
    ),
)

// =====================================================================
// 3. 深邃蓝（Deep Blue）
// =====================================================================
private val BlueLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF1565C0),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF00695C),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8EDF2),
    onSurfaceVariant = Color(0xFF44474E),
    outlineVariant = Color(0xFFC0C4CC),
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1976D2),
    onSecondaryContainer = Color(0xFFE3F2FD),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF004D40),
    tertiaryContainer = Color(0xFF00695C),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1A1F2B),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A303B),
    onSurfaceVariant = Color(0xFFA0A8B4),
    outlineVariant = Color(0xFF39404C),
)

private val BlueCharts = ChartColorScheme(
    chartPrimary = Color(0xFF1E88E5),
    chartAccent = Color(0xFFFF7043),
    chartPalette = listOf(
        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFF7043),
        Color(0xFFE53935), Color(0xFF3949AB), Color(0xFF9E9E9E),
    ),
    chartPaletteExtended = listOf(
        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFF7043),
        Color(0xFFE53935), Color(0xFF3949AB), Color(0xFF00ACC1),
        Color(0xFFEC407A), Color(0xFF1565C0), Color(0xFFFF8A65),
        Color(0xFF9E9E9E),
    ),
)

// =====================================================================
// 4. 暖橙（Warm Orange）
// =====================================================================
private val OrangeLight = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFFE65100),
    tertiary = Color(0xFFD84315),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFCCBC),
    onTertiaryContainer = Color(0xFFBF360C),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5ECE4),
    onSurfaceVariant = Color(0xFF4F453D),
    outlineVariant = Color(0xFFD4C4BC),
)

private val OrangeDark = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFFBF360C),
    primaryContainer = Color(0xFFE65100),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFFBF360C),
    secondaryContainer = Color(0xFFEF6C00),
    onSecondaryContainer = Color(0xFFFFF3E0),
    tertiary = Color(0xFFFF8A65),
    onTertiary = Color(0xFFBF360C),
    tertiaryContainer = Color(0xFFD84315),
    onTertiaryContainer = Color(0xFFFFCCBC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF261D16),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF3A2E24),
    onSurfaceVariant = Color(0xFFB8A898),
    outlineVariant = Color(0xFF4A3A2C),
)

private val OrangeCharts = ChartColorScheme(
    chartPrimary = Color(0xFFFF7043),
    chartAccent = Color(0xFF7C4DFF),
    chartPalette = listOf(
        Color(0xFFFF7043), Color(0xFF43A047), Color(0xFF1E88E5),
        Color(0xFF8E24AA), Color(0xFFE53935), Color(0xFF9E9E9E),
    ),
    chartPaletteExtended = listOf(
        Color(0xFFFF7043), Color(0xFF43A047), Color(0xFF1E88E5),
        Color(0xFF8E24AA), Color(0xFFE53935), Color(0xFF00ACC1),
        Color(0xFFEC407A), Color(0xFFFF8A65), Color(0xFF7C4DFF),
        Color(0xFF9E9E9E),
    ),
)

// =====================================================================
// 5. 极简灰（Minimal Gray）
// =====================================================================
private val GrayLight = lightColorScheme(
    primary = Color(0xFF455A64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFD8DC),
    onPrimaryContainer = Color(0xFF263238),
    secondary = Color(0xFF607D8B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = Color(0xFF37474F),
    tertiary = Color(0xFF78909C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFD8DC),
    onTertiaryContainer = Color(0xFF37474F),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF49454F),
    outlineVariant = Color(0xFFCCCCCC),
)

private val GrayDark = darkColorScheme(
    primary = Color(0xFFB0BEC5),
    onPrimary = Color(0xFF263238),
    primaryContainer = Color(0xFF455A64),
    onPrimaryContainer = Color(0xFFCFD8DC),
    secondary = Color(0xFFCFD8DC),
    onSecondary = Color(0xFF37474F),
    secondaryContainer = Color(0xFF546E7A),
    onSecondaryContainer = Color(0xFFECEFF1),
    tertiary = Color(0xFF90A4AE),
    onTertiary = Color(0xFF37474F),
    tertiaryContainer = Color(0xFF546E7A),
    onTertiaryContainer = Color(0xFFCFD8DC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outlineVariant = Color(0xFF3A3A3A),
)

private val GrayCharts = ChartColorScheme(
    chartPrimary = Color(0xFF607D8B),
    chartAccent = Color(0xFFFF7043),
    chartPalette = listOf(
        Color(0xFF607D8B), Color(0xFF546E7A), Color(0xFF78909C),
        Color(0xFF455A64), Color(0xFF90A4AE), Color(0xFFBDBDBD),
    ),
    chartPaletteExtended = listOf(
        Color(0xFF607D8B), Color(0xFF546E7A), Color(0xFF78909C),
        Color(0xFF455A64), Color(0xFF90A4AE), Color(0xFFB0BEC5),
        Color(0xFF37474F), Color(0xFF8D6E63), Color(0xFF9E9E9E),
        Color(0xFFBDBDBD),
    ),
)

// =====================================================================
// 公开的 5 套 ThemePreset
// =====================================================================

/** 预设清单（供 UI 选择器消费） */
val AllThemePresets = listOf(
    ThemePreset(
        name = "默认紫",
        lightScheme = PurpleLight,
        darkScheme = PurpleDark,
        chartColors = PurpleCharts,
    ),
    ThemePreset(
        name = "清新绿",
        lightScheme = GreenLight,
        darkScheme = GreenDark,
        chartColors = GreenCharts,
    ),
    ThemePreset(
        name = "深邃蓝",
        lightScheme = BlueLight,
        darkScheme = BlueDark,
        chartColors = BlueCharts,
    ),
    ThemePreset(
        name = "暖橙",
        lightScheme = OrangeLight,
        darkScheme = OrangeDark,
        chartColors = OrangeCharts,
    ),
    ThemePreset(
        name = "极简灰",
        lightScheme = GrayLight,
        darkScheme = GrayDark,
        chartColors = GrayCharts,
    ),
)

/** 按名称查找预设，不存在时返回默认紫 */
fun findPresetByName(name: String): ThemePreset =
    AllThemePresets.find { it.name == name } ?: AllThemePresets[0]
