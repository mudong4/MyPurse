package com.wyd.mypurse.data.local.database

import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import com.wyd.mypurse.data.repository.CategoryDefaults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val mutex = Mutex()

    /**
     * 执行首次初始化。如果数据库已有数据则跳过。
     * 调用方应在协程中调用此方法。
     * 使用 Mutex 防止并发重复插入（如 ViewModel init 和 LaunchedEffect 同时触发）。
     * 同时清理历史重复数据（由旧版本 race condition 导致的 name+parentId 重复项）。
     */
    suspend fun initializeIfNeeded() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            val allCategories = categoryDefDao.getAllCategoriesOnce()
            if (allCategories.isNotEmpty()) {
                // 清理历史重复数据
                cleanDuplicateCategories(allCategories)
            } else {
                seedDefaultCategories()
            }
            initialized = true
        }
    }

    /**
     * 清理历史重复的分类记录（name + parentId + flowSign 相同的保留第一条）。
     */
    private suspend fun cleanDuplicateCategories(allCategories: List<CategoryDefEntity>) {
        val seen = mutableSetOf<Triple<String, Long?, Int>>()
        val toDelete = mutableListOf<Long>()

        for (category in allCategories) {
            val key = Triple(category.name, category.parentId, category.flowSign)
            if (key in seen) {
                toDelete.add(category.id)
            } else {
                seen.add(key)
            }
        }

        for (id in toDelete) {
            categoryDefDao.deleteCategory(id)
        }
    }

    /**
     * 强制重新播种（用于"清除所有数据"后）。重置 initialized 标志后调用 initializeIfNeeded。
     */
    suspend fun forceReinitialize() {
        initialized = false
        seedDefaultCategories()
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
        subMap: Map<String, List<String>>
    ) {
        val defaultColor = if (topLevel.firstOrNull()?.flowSign?.let { it >= 0 } == true)
            CategoryDefaults.incomeDefaultColor
        else
            CategoryDefaults.expenseDefaultColor

        for (category in topLevel) {
            val id = categoryDefDao.insertCategory(
                CategoryDefEntity(
                    name = category.name,
                    parentId = null,
                    isDefault = true,
                    sortOrder = category.sortOrder,
                    flowSign = category.flowSign,
                    color = category.color
                )
            )
            val subs = subMap[category.name] ?: continue
            subs.forEachIndexed { index, name ->
                categoryDefDao.insertCategory(
                    CategoryDefEntity(
                        name = name,
                        parentId = id,
                        isDefault = true,
                        sortOrder = index + 1,
                        flowSign = category.flowSign,
                        color = defaultColor
                    )
                )
            }
        }
    }
}
