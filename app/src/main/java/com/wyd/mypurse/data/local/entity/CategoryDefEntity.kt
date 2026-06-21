package com.wyd.mypurse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类定义实体。支持两级分类，parentId 为 null 时表示一级分类。
 *
 * flowSign 表示分类所属的流水类型侧：
 * - -1：支出侧（餐饮、交通、购物等）
 * -  1：收入侧（工资、奖金、理财等）
 * -  0：预留（通用分类，V1.0 不使用）
 */
@Entity(tableName = "category_def")
data class CategoryDefEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 分类名称 */
    val name: String,

    /** 所属一级分类 ID，null 表示自身是一级分类 */
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,

    /** 是否为内置分类 */
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    /** 是否隐藏 */
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    /** 排序序号 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /**
     * 流水类型归属：
     * -1 = 支出侧，1 = 收入侧，0 = 通用（预留）。
     * 与 FlowType.sign 对齐。
     */
    @ColumnInfo(name = "flow_sign")
    val flowSign: Int = -1,

    /**
     * 分类专属颜色（ARGB 色值，Long 型存储）。
     * 0 表示未设置，UI 层 fallback 到 flowSign 对应的默认语义色（支出红/收入绿）。
     * V1.1 预留：后续支持用户自定义颜色。
     */
    @ColumnInfo(name = "color")
    val color: Long = 0
)
