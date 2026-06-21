package com.wyd.mypurse.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 设计令牌 — 统一管理圆角、阴影、间距等视觉常量。
 *
 * 所有页面应优先使用这些值，而非各处零散硬编码。
 */

// ========== 圆角 ==========

/** 小圆角（标签、Chip、小卡片） */
val AppCornerSmall = 6.dp
/** 标准圆角（卡片、按钮、输入框） */
val AppCornerMedium = 12.dp
/** 大圆角（Modal、Sheet） */
val AppCornerLarge = 16.dp

// ========== 阴影 / 海拔 ==========

/** 卡片默认阴影 */
val AppElevationCard = 1.dp
/** 浮起元素（FAB、下拉菜单） */
val AppElevationRaised = 4.dp
/** 对话框 / Sheet */
val AppElevationOverlay = 8.dp

// ========== 间距 ==========

/** 紧凑间距（图标与文字之间） */
val AppSpacingXs = 4.dp
/** 小间距（并列元素之间） */
val AppSpacingSm = 8.dp
/** 标准间距（区块内元素之间） */
val AppSpacingMd = 12.dp
/** 段间距（区块之间） */
val AppSpacingLg = 16.dp
/** 页间距（页面内大区块之间） */
val AppSpacingXl = 24.dp
