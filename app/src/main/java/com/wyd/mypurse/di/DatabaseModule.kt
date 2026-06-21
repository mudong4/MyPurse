package com.wyd.mypurse.di

import android.content.Context
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.util.Log
import androidx.room.Room
import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.dao.RecurringTemplateDao
import com.wyd.mypurse.data.local.dao.TransactionDao
import com.wyd.mypurse.data.local.database.AppDatabase
import com.wyd.mypurse.data.local.database.DatabaseInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库 Hilt 模块。提供 Room Database 和各 DAO 的单例实例。
 *
 * 数据库异常兜底策略：
 * - 正常打开：使用 Migration 升级
 * - 数据库损坏（SQLiteDatabaseCorruptException）：尝试删除损坏文件并重建，首次启动种子数据会自动重新播种
 * - 版本跳跃（未匹配 Migration）：fallbackToDestructiveMigration 作为最后兜底，保护用户免受崩溃
 * - 其他 IO 异常：向上抛出，由调用方处理
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"
    private const val DB_NAME = "mypurse.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return try {
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: SQLiteDatabaseCorruptException) {
            Log.e(TAG, "数据库文件已损坏，尝试删除并重建: ${e.message}")
            context.deleteDatabase(DB_NAME)
            // 重建数据库，种子数据由 DatabaseInitializer 在首次查询时自动填充
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "无法打开数据库文件，尝试删除并重建: ${e.message}")
            context.deleteDatabase(DB_NAME)
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDefDao(database: AppDatabase): CategoryDefDao =
        database.categoryDefDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideRecurringTemplateDao(database: AppDatabase): RecurringTemplateDao =
        database.recurringTemplateDao()

    @Provides
    @Singleton
    fun provideDatabaseInitializer(categoryDefDao: CategoryDefDao): DatabaseInitializer =
        DatabaseInitializer(categoryDefDao)
}
