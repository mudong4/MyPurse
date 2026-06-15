package com.wyd.mypurse.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.dao.RecurringTemplateDao
import com.wyd.mypurse.data.local.dao.TransactionDao
import com.wyd.mypurse.data.local.entity.BudgetEntity
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import com.wyd.mypurse.data.local.entity.RecurringTemplateEntity
import com.wyd.mypurse.data.local.entity.TransactionEntity

/**
 * 应用数据库定义。注册全部 4 个 Entity 和 4 个 DAO。
 * V1.0 开发期使用 destructive migration，阶段 5 替换为正式 Migration。
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryDefEntity::class,
        BudgetEntity::class,
        RecurringTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDefDao(): CategoryDefDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
}
