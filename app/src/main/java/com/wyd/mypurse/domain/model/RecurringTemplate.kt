package com.wyd.mypurse.domain.model

import java.math.BigDecimal

/**
 * 固定收支模板业务模型。供 UI 层使用，去除 Room 注解。
 */
data class RecurringTemplate(
    val id: Long = 0,
    /** 流水类型（支出/收入/自定义名称） */
    val flowType: String,
    /** 一级分类 ID */
    val categoryL1Id: Long? = null,
    /** 二级分类 ID（可选） */
    val categoryL2Id: Long? = null,
    /** 金额 */
    val amount: BigDecimal,
    /** 备注 */
    val note: String? = null,
    /** 周期类型：DAILY / WEEKLY / MONTHLY / YEARLY */
    val cycleType: String,
    /** 周期值：DAILY=0；WEEKLY=1-7；MONTHLY=1-31；YEARLY=MMDD */
    val cycleValue: Long,
    /** 是否启用 */
    val isActive: Boolean = true,
    /** 上次执行日期（毫秒时间戳） */
    val lastExecutedDate: Long? = null,
    /** 下次执行日期（毫秒时间戳），预计算 */
    val nextExecutionDate: Long,
    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis()
)
