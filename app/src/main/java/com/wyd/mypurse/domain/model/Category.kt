package com.wyd.mypurse.domain.model

/**
 * 分类业务模型。与 CategoryDefEntity 对应但去除 Room 注解，供 UI 层使用。
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val flowSign: Int = -1
)
