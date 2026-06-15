package com.wyd.mypurse.di

import android.content.Context
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
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mypurse.db"
        )
            // TODO: 阶段 5 替换为正式 Migration，移除 fallbackToDestructiveMigration
            .fallbackToDestructiveMigration()
            .build()
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
