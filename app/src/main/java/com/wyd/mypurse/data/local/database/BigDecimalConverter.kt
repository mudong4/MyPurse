package com.wyd.mypurse.data.local.database

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * BigDecimal 与 String 之间的 Room TypeConverter。
 * 所有金额字段在数据库中以纯文本形式存储，读写时转为 BigDecimal 确保精度。
 */
class BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
