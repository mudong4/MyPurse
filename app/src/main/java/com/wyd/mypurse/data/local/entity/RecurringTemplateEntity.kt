package com.wyd.mypurse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * 固定收支周期模板实体。
 */
@Entity(tableName = "recurring_template")
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 流水类型（支出/收入/自定义名称） */
    @ColumnInfo(name = "flow_type")
    val flowType: String,

    /** 一级分类 ID */
    @ColumnInfo(name = "category_l1_id")
    val categoryL1Id: Long? = null,

    /** 二级分类 ID */
    @ColumnInfo(name = "category_l2_id")
    val categoryL2Id: Long? = null,

    /** 金额 */
    val amount: BigDecimal,

    /** 备注 */
    val note: String? = null,

    /** 周期类型：DAILY / WEEKLY / MONTHLY / YEARLY */
    @ColumnInfo(name = "cycle_type")
    val cycleType: String,

    /** 周期值：DAILY=0；WEEKLY=1-7；MONTHLY=1-31；YEARLY=MMDD */
    @ColumnInfo(name = "cycle_value")
    val cycleValue: Long,

    /** 是否启用 */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /** 上次执行日期（毫秒时间戳） */
    @ColumnInfo(name = "last_executed_date")
    val lastExecutedDate: Long? = null,

    /** 下次执行日期（毫秒时间戳），预计算，每次执行后更新 */
    @ColumnInfo(name = "next_execution_date")
    val nextExecutionDate: Long,

    /** 创建时间 */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)
