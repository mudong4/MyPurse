package com.wyd.mypurse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * 预算实体。极简方案：表中永远只有一条记录（id=1），
 * 用户修改时覆盖。不保留历史，不支持多粒度。
 *
 * 设计决策（2026-06-16）：从原按月存储（year+month 复合主键）简化为单一值。
 * 个人记账场景下，历史预算值几乎不会被回顾，按月存储的复杂度收益不成比例。
 * 如需扩展（如分类预算、历史记录），可通过 Room Migration 平滑升级。
 */
@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey
    val id: Int = 1,

    /** 月度预算金额 */
    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    /** 最后修改时间（毫秒时间戳） */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
