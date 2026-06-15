package com.wyd.mypurse.data.local.database

import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import com.wyd.mypurse.data.repository.CategoryDefaults
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库初始化器。首次启动时插入内置分类种子数据。
 * 在 Repository 首次访问时通过协程触发，不在 Application.onCreate 中阻塞启动。
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val categoryDefDao: CategoryDefDao
) {
    /** 是否已初始化 */
    private var initialized = false

    /**
     * 执行首次初始化。如果数据库已有数据则跳过。
     * 调用方应在协程中调用此方法。
     */
    suspend fun initializeIfNeeded() {
        if (initialized) return
        val count = categoryDefDao.getAllCategoriesOnce().size
        if (count == 0) {
            seedDefaultCategories()
        }
        initialized = true
    }

    /**
     * 插入内置分类种子数据。
     * 支出侧和收入侧分别插入，每个一级分类关联对应的二级分类。
     */
    private suspend fun seedDefaultCategories() {
        // 插入支出侧分类
        seedCategoriesByFlowType(CategoryDefaults.expenseCategories, CategoryDefaults.expenseSubCategories)
        // 插入收入侧分类
        seedCategoriesByFlowType(CategoryDefaults.incomeCategories, CategoryDefaults.incomeSubCategories)
    }

    private suspend fun seedCategoriesByFlowType(
        topLevel: List<com.wyd.mypurse.domain.model.Category>,
        subMap: Map<String, List<Pair<String, String>>>
    ) {
        for (category in topLevel) {
            val id = categoryDefDao.insertCategory(
                CategoryDefEntity(
                    name = category.name,
                    parentId = null,
                    icon = category.icon,
                    isDefault = true,
                    sortOrder = category.sortOrder,
                    flowSign = category.flowSign
                )
            )
            val subs = subMap[category.name] ?: continue
            subs.forEachIndexed { index, (name, icon) ->
                categoryDefDao.insertCategory(
                    CategoryDefEntity(
                        name = name,
                        parentId = id,
                        icon = icon,
                        isDefault = true,
                        sortOrder = index + 1,
                        flowSign = category.flowSign
                    )
                )
            }
        }
    }
}
