package com.wyd.mypurse.domain.model

import java.math.BigDecimal

/**
 * 交易记录业务模型。供 UI 层使用，去除 Room 注解。
 */
data class Transaction(
    val id: Long = 0,
    val flowType: String,
    val categoryL1Id: Long? = null,
    val categoryL2Id: Long? = null,
    val categoryL1: String,
    val categoryL2: String? = null,
    val amount: BigDecimal,
    val note: String? = null,
    val date: Long,
    val createTime: Long = System.currentTimeMillis(),
    val ledgerId: String = "default",
    val recurringTemplateId: Long? = null
)

/**
 * 分类金额汇总，用于统计图。
 */
data class CategoryAmount(
    val categoryL1Id: Long?,
    val categoryL1: String,
    val total: BigDecimal
)

/**
 * 月度金额汇总，用于趋势图。
 */
data class MonthlyAmount(
    val year: Int,
    val month: Int,
    val total: BigDecimal
)

/**
 * 日/周/月/年汇总快照。
 */
data class PeriodSummary(
    val expense: BigDecimal = BigDecimal.ZERO,
    val income: BigDecimal = BigDecimal.ZERO
)

/**
 * 趋势数据点（统计页趋势图使用）。
 * label 格式因粒度而异：日="06-16"、周="W24"、月="6月"、季="Q2"、年="2026"
 */
data class TrendPoint(
    val label: String,
    val total: BigDecimal
)
