package com.wyd.mypurse.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 *
 * 版本历史：
 * - V1：初始版本（TransactionEntity + CategoryDefEntity）
 * - V2：CategoryDefEntity 新增 flow_sign 列
 * - V3：新增 BudgetEntity 表
 * - V4：新增 RecurringTemplateEntity 表
 * - V5：CategoryDefEntity 新增 color 列（ARGB 色值，默认 0）
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryDefEntity::class,
        BudgetEntity::class,
        RecurringTemplateEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDefDao(): CategoryDefDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao

    companion object {
        /** V1 → V2：category_def 表新增 flow_sign 列，默认 -1（支出侧） */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category_def ADD COLUMN flow_sign INTEGER NOT NULL DEFAULT -1")
            }
        }

        /** V2 → V3：新增 budget 表 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget (
                        id INTEGER NOT NULL PRIMARY KEY,
                        amount TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /** V3 → V4：新增 recurring_template 表 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_template (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        flow_type TEXT NOT NULL,
                        category_l1_id INTEGER,
                        category_l2_id INTEGER,
                        amount TEXT NOT NULL,
                        note TEXT,
                        cycle_type TEXT NOT NULL,
                        cycle_value INTEGER NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        last_executed_date INTEGER,
                        next_execution_date INTEGER NOT NULL,
                        create_time INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /** V4 → V5：category_def 表新增 color 列，默认 0（未设置） */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category_def ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 所有 Migration 列表 */
        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
