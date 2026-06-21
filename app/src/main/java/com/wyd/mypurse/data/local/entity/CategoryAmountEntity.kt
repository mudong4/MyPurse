package com.wyd.mypurse.data.local.entity

import java.math.BigDecimal

/**
 * 分类金额汇总查询结果，对应 Room @Query 聚合查询。
 */
data class CategoryAmountEntity(
    val categoryL1Id: Long?,
    val categoryL1: String,
    val total: BigDecimal,
    /** 分类颜色（ARGB 色值），从 category_def.color 取。0 表示未设置 */
    val color: Long = 0
)

/**
 * 月度金额汇总查询结果，对应 Room @Query 聚合查询。
 */
data class MonthlyAmountEntity(
    val year: Int,
    val month: Int,
    val total: BigDecimal
)
