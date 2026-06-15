package com.wyd.mypurse.data.local.entity

import java.math.BigDecimal

/**
 * 分类金额汇总查询结果，对应 Room @Query 聚合查询。
 */
data class CategoryAmountEntity(
    val categoryL1Id: Long?,
    val categoryL1: String,
    val total: BigDecimal
)

/**
 * 月度金额汇总查询结果，对应 Room @Query 聚合查询。
 */
data class MonthlyAmountEntity(
    val year: Int,
    val month: Int,
    val total: BigDecimal
)
