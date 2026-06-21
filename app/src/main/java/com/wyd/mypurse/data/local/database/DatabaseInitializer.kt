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
     * 同时清理历史重复数据，并回填 V1.0.1 新增的 color 字段（老用户升级场景）。
     */
    suspend fun initializeIfNeeded() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            val allCategories = categoryDefDao.getAllCategoriesOnce()
            if (allCategories.isNotEmpty()) {
                // 清理历史重复数据
                cleanDuplicateCategories(allCategories)
                // 回填内置分类的颜色（MIGRATION_4_5 仅加列，未补数据）
                backfillCategoryColors(allCategories)
                // 回填旧自定义分类的颜色（V1.0 创建的 color=0 分类）
                backfillCustomCategoryColors(allCategories)
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
     * 回填内置分类的颜色（V1.0.1 升级场景）。
     * MIGRATION_4_5 为 category_def 添加 color 列时 DEFAULT 0，未对已有内置分类
     * 填充 CategoryDefaults 中定义的默认颜色。此方法匹配name + flowSign 查找对应颜色并更新。
     *
     * 仅处理 isDefault=true 且 color=0 的分类（跳过用户自定义分类和已有颜色的分类）。
     * 一级分类按 name + flowSign 精确匹配 CategoryDefaults 中的颜色；
     * 二级分类按父分类的 flowSign 取对应的默认语义色（支出浅红 / 收入浅绿）。
     */
    private suspend fun backfillCategoryColors(allCategories: List<CategoryDefEntity>) {
        // 构建 top-level name → color 查找表
        val topLevelColorMap = mutableMapOf<String, Long>()
        val allDefaults = CategoryDefaults.expenseCategories + CategoryDefaults.incomeCategories
        for (cat in allDefaults) {
            topLevelColorMap[cat.name] = cat.color
        }

        for (category in allCategories) {
            if (category.color != 0L || !category.isDefault) continue

            if (category.parentId == null) {
                // 一级分类：按名称匹配默认颜色
                val color = topLevelColorMap[category.name] ?: continue
                categoryDefDao.updateCategory(category.copy(color = color))
            } else {
                // 二级分类：按父分类 flowSign 取语义默认色
                val parent = allCategories.firstOrNull { it.id == category.parentId } ?: continue
                val defaultColor = if (parent.flowSign >= 0)
                    CategoryDefaults.incomeDefaultColor
                else
                    CategoryDefaults.expenseDefaultColor
                categoryDefDao.updateCategory(category.copy(color = defaultColor))
            }
        }
    }

    /**
     * 回填旧自定义分类的颜色（V1.0 → V1.1 升级场景）。
     *
     * V1.0 创建的自定义分类 color=0，未设置颜色。这些分类在所有图表中
     * 只能依赖 chartPalette 按列表索引轮转，同一分类在不同页面因排序不同
     * 可能呈现不同颜色，体验不一致。
     *
     * 本方法按 flowSign 分组、按 id 稳定排序后，使用 8 色轮转表分配颜色，
     * 确保同一分类在所有页面显示一致颜色。
     */
    private suspend fun backfillCustomCategoryColors(allCategories: List<CategoryDefEntity>) {
        val rotationColors = CategoryDefaults.rotationColors
        // 只处理 isDefault=false 且 color=0 的自定义分类
        val customCategories = allCategories.filter { !it.isDefault && it.color == 0L }
        if (customCategories.isEmpty()) return

        // 按 flowSign 分组，每组按 id 稳定排序后轮转分配颜色
        val expenseList = customCategories.filter { it.flowSign < 0 }.sortedBy { it.id }
        val incomeList = customCategories.filter { it.flowSign >= 0 }.sortedBy { it.id }

        for ((index, category) in expenseList.withIndex()) {
            categoryDefDao.updateCategory(
                category.copy(color = rotationColors[index % rotationColors.size])
            )
        }
        for ((index, category) in incomeList.withIndex()) {
            categoryDefDao.updateCategory(
                category.copy(color = rotationColors[index % rotationColors.size])
            )
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
