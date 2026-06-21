package com.wyd.mypurse.domain.model

/**
 * 分类业务模型。与 CategoryDefEntity 对应但去除 Room 注解，供 UI 层使用。
 *
 * @param color ARGB 色值（Long），用于分类可视化展示。支出侧默认红系、收入侧默认绿系。
 *              V1.1 预留：后续可支持用户自定义颜色（DataStore 覆盖层）。
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val flowSign: Int = -1,
    val color: Long = 0  // 0 表示未设置，UI 层 fallback 到默认语义色
)
