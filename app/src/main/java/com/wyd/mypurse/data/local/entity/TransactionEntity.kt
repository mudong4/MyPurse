package com.wyd.mypurse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * 收支记录实体。金额使用 BigDecimal，数据库通过 TypeConverter 转为 String 存储。
 */
@Entity(tableName = "transaction")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "flow_type")
    val flowType: String,

    @ColumnInfo(name = "category_l1_id")
    val categoryL1Id: Long? = null,

    @ColumnInfo(name = "category_l2_id")
    val categoryL2Id: Long? = null,

    @ColumnInfo(name = "category_l1")
    val categoryL1: String,

    @ColumnInfo(name = "category_l2")
    val categoryL2: String? = null,

    val amount: BigDecimal,

    val note: String? = null,

    /** 交易日期（毫秒时间戳） */
    val date: Long,

    /** 记录创建时间（毫秒时间戳） */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),

    /** 账本 ID，V1.0 固定为 "default" */
    @ColumnInfo(name = "ledger_id")
    val ledgerId: String = "default",

    /** 来源模板 ID，null 表示手动记录 */
    @ColumnInfo(name = "recurring_template_id")
    val recurringTemplateId: Long? = null
)
