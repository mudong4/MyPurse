package com.wyd.mypurse.di

import com.wyd.mypurse.data.repository.CategoryRepositoryImpl
import com.wyd.mypurse.data.repository.FlowTypeRepositoryImpl
import com.wyd.mypurse.data.repository.TransactionRepositoryImpl
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.FlowTypeRepository
import com.wyd.mypurse.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 绑定 Hilt 模块。将接口与实现类绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindFlowTypeRepository(impl: FlowTypeRepositoryImpl): FlowTypeRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
