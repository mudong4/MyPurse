package com.wyd.mypurse.ui.settings

/**
 * 设置页 UI 状态。
 */
data class SettingsUiState(
    /** 导出进度 */
    val isExporting: Boolean = false,
    /** 导出文件 Uri（完成后供分享） */
    val exportedFileUri: String? = null,

    /** 导入进度 */
    val isImporting: Boolean = false,
    /** 导入结果 */
    val importResultMessage: String? = null,

    /** 清除数据确认对话框 */
    val showClearDataDialog: Boolean = false,
    /** 是否正在清除 */
    val isClearingData: Boolean = false,
    /** 清除完成 */
    val clearDataComplete: Boolean = false,

    /** 错误提示 */
    val errorMessage: String? = null,

    /** 当前主题预设名（V1.1 主题切换） */
    val currentThemePresetName: String = "默认紫",
    /** 是否显示预设选择器 BottomSheet（V1.1） */
    val showPresetSelector: Boolean = false
)
