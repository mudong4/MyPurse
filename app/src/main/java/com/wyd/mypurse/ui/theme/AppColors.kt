package com.wyd.mypurse.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 应用级语义色值（第二层：业务语义色）。
 *
 * 这些颜色有固定语义，不随明暗主题反转，但可通过 [ChartColorScheme] 切换图表配色方案。
 *
 * 层级关系：
 * - 第一层：MaterialTheme.colorScheme（页面背景/卡片/文字框架色，自动适配明暗）
 * - 第二层：AppColors（支出红/收入绿/预算色/图表色，全局统一）
 * - 第三层：极少数页面级硬编码色
 *
 * V1.0.1：新增 [categoryColor] 工具函数，将 Category.color（Long 型 ARGB 色值）转为 Compose Color。
 *          color 为 0 时返回透明色，由调用方 fallback 到默认语义色。
 */

/**
 * 将 Category.color（Long 型 ARGB 色值）转为 Compose [Color]。
 * 返回 null 表示 color 未设置（值为 0），调用方应 fallback 到默认色。
 */
fun categoryColor(argb: Long): Color? = if (argb == 0L) null else Color(argb)

/**
 * 计算颜色的相对亮度（WCAG 标准），返回 0~1，>0.5 视为亮色。
 * 用于决定前景文字颜色（白/黑）以保证可读性。
 */
fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** 根据背景色的亮度返回对比度足够的前景色（白或黑） */
fun Color.contrastingForeground(): Color = if (luminance() > 0.55f) Color.Black else Color.White

// ========== 功能语义色 ==========

/** 支出金额专用色 */
val AppExpenseRed = Color(0xFFE53935)

/** 支出浅背景色（标签/图标底） */
val AppExpenseLightBg = Color(0xFFFFEBEE)

/** 收入金额专用色 */
val AppIncomeGreen = Color(0xFF43A047)

/** 收入浅背景色（标签/图标底） */
val AppIncomeLightBg = Color(0xFFE8F5E9)

// ========== 预算相关色 ==========

/** 预算正常色（进度条/文字链接） */
val AppBudgetBlue = Color(0xFF1E88E5)

/** 预算使用率 ≥80% 警告色 */
val AppBudgetOrange = Color(0xFFFB8C00)

/** 预算超支色 */
val AppBudgetOverRed = Color(0xFFD32F2F)

/** 进度条背景轨道色（浅色模式下使用） */
val AppProgressTrack = Color(0xFFE0E0E0)

// ========== 分割线 / 浅色背景 ==========

/** 通用分割线色（浅色模式） */
val AppDivider = Color(0xFFE0E0E0)

/** 浅灰背景（Sheet/页面底） */
val AppSheetBg = Color(0xFFF5F5F5)

/** 极浅页面背景 */
val AppSurfaceBg = Color(0xFFF8F9FA)

/** 条形图背景色 */
val AppBarChartBg = Color(0xFFEEEEEE)

// ========== 编辑/警告色 ==========

/** 编辑模式警告卡片背景 */
val AppEditWarningBg = Color(0xFFFFF3E0)

/** 编辑模式警告文字 */
val AppEditWarningText = Color(0xFFE65100)

// ========== 图表配色方案 ==========

/**
 * 图表配色方案，可扩展用于未来自定义风格。
 * 当前提供两套方案，后续可增加更多。
 */
data class ChartColorScheme(
    /** 柱状图/折线图主色 */
    val chartPrimary: Color = Color(0xFF1E88E5),
    /** 选中/高亮色 */
    val chartAccent: Color = Color(0xFFFF7043),
    /** 环形图/条形图 TOP5 彩色列表 */
    val chartPalette: List<Color> = listOf(
        Color(0xFF1E88E5),  // 蓝
        Color(0xFF43A047),  // 绿
        Color(0xFFFB8C00),  // 橙
        Color(0xFFE53935),  // 红
        Color(0xFF8E24AA),  // 紫
        Color(0xFF9E9E9E),  // 灰（"其他"）
    ),
    /** 环形图/条形图扩展彩色列表（用于更多分类） */
    val chartPaletteExtended: List<Color> = listOf(
        Color(0xFF1E88E5),  // 蓝
        Color(0xFF43A047),  // 绿
        Color(0xFFFB8C00),  // 橙
        Color(0xFFE53935),  // 红
        Color(0xFF8E24AA),  // 紫
        Color(0xFF00ACC1),  // 青
        Color(0xFFEC407A),  // 粉
        Color(0xFF3949AB),  // 靛蓝
        Color(0xFFFF7043),  // 橘
        Color(0xFF9E9E9E),  // 灰（兜底）
    )
)

/** 默认图表配色方案 */
val DefaultChartColors = ChartColorScheme()

/** 柔和图表配色方案（未来可选） */
val SoftChartColors = ChartColorScheme(
    chartPrimary = Color(0xFF5C6BC0),
    chartAccent = Color(0xFFFF8A65),
    chartPalette = listOf(
        Color(0xFF7986CB), Color(0xFF81C784), Color(0xFFFFB74D),
        Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFFBDBDBD),
    ),
    chartPaletteExtended = listOf(
        Color(0xFF7986CB), Color(0xFF81C784), Color(0xFFFFB74D),
        Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DD0E1),
        Color(0xFFF06292), Color(0xFF7986CB), Color(0xFFFF8A65),
        Color(0xFFBDBDBD),
    )
)


