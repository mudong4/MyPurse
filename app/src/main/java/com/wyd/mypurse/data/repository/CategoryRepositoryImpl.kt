package com.wyd.mypurse.data.repository

import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.dao.TransactionDao
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类管理 Repository 本地实现。将 Room Entity 与 domain model 互相转换。
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDefDao: CategoryDefDao,
    private val transactionDao: TransactionDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDefDao.getAllCategories().map { it.map(::toDomain) }

    override suspend fun getAllCategoriesOnce(): List<Category> =
        categoryDefDao.getAllCategoriesOnce().map(::toDomain)

    override fun getTopLevelCategories(): Flow<List<Category>> =
        categoryDefDao.getTopLevelCategories().map { it.map(::toDomain) }

    override fun getTopLevelCategoriesBySign(flowSign: Int): Flow<List<Category>> =
        categoryDefDao.getTopLevelCategoriesBySign(flowSign).map { it.map(::toDomain) }

    override suspend fun getTopLevelCategoriesBySignOnce(flowSign: Int): List<Category> =
        categoryDefDao.getTopLevelCategoriesBySignOnce(flowSign).map(::toDomain)

    override fun getSubCategories(parentId: Long): Flow<List<Category>> =
        categoryDefDao.getSubCategories(parentId).map { it.map(::toDomain) }

    override suspend fun getSubCategoriesOnce(parentId: Long): List<Category> =
        categoryDefDao.getSubCategoriesOnce(parentId).map(::toDomain)

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDefDao.getCategoryById(id)?.let(::toDomain)

    override suspend fun addCategory(
        name: String,
        parentId: Long?,
        isDefault: Boolean,
        flowSign: Int,
        color: Long
    ): Long {
        val maxOrder = categoryDefDao.getMaxSortOrder(parentId)
        val defaultColor = when {
            color != 0L -> color                                 // 明确传入的颜色
            isDefault -> CategoryDefaults.expenseDefaultColor    // 内置分类（一般不会走到这）
            else -> {                                            // 自定义分类：轮转色
                val count = categoryDefDao.getCustomCategoryCount(flowSign)
                CategoryDefaults.rotationColors[count % CategoryDefaults.rotationColors.size]
            }
        }
        val entity = CategoryDefEntity(
            name = name,
            parentId = parentId,
            isDefault = isDefault,
            sortOrder = maxOrder + 1,
            flowSign = flowSign,
            color = defaultColor
        )
        return categoryDefDao.insertCategory(entity)
    }

    override suspend fun addCategories(categories: List<Category>) {
        val entities = categories.map { toEntity(it) }
        categoryDefDao.insertCategories(entities)
    }

    override suspend fun updateCategory(id: Long, name: String) {
        val existing = categoryDefDao.getCategoryById(id) ?: return
        categoryDefDao.updateCategory(existing.copy(name = name))
        // 同步更新流水快照，保证快照始终与 category_def 一致
        if (existing.parentId == null) {
            transactionDao.updateCategoryL1Snapshot(id, name)
        } else {
            transactionDao.updateCategoryL2Snapshot(id, name)
        }
    }

    override suspend fun updateCategoryColor(id: Long, color: Long) {
        val existing = categoryDefDao.getCategoryById(id) ?: return
        categoryDefDao.updateCategory(existing.copy(color = color))
    }

    override suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        categoryDefDao.updateSortOrder(id, sortOrder)
    }

    override suspend fun batchUpdateSortOrder(sortMap: Map<Long, Int>) {
        categoryDefDao.batchUpdateSortOrder(sortMap)
    }

    override suspend fun deleteCategory(id: Long): Category? {
        val entity = categoryDefDao.getCategoryById(id) ?: return null
        categoryDefDao.deleteCategory(id)
        return toDomain(entity)
    }

    override suspend fun deleteCategoryWithRecords(categoryId: Long) {
        // 判断是一级还是二级分类，分别删除关联流水
        val category = categoryDefDao.getCategoryById(categoryId)
        if (category != null) {
            if (category.parentId == null) {
                // 一级分类：删除该一级分类下的所有流水 + 其子分类下的流水
                transactionDao.deleteTransactionsByCategoryL1Id(categoryId)
                // 同时删除子分类定义
                val subs = categoryDefDao.getSubCategoriesOnce(categoryId)
                subs.forEach { sub ->
                    transactionDao.deleteTransactionsByCategoryL2Id(sub.id)
                    categoryDefDao.deleteCategory(sub.id)
                }
            } else {
                // 二级分类：删除该二级分类下的流水
                transactionDao.deleteTransactionsByCategoryL2Id(categoryId)
            }
            categoryDefDao.deleteCategory(categoryId)
        }
    }

    override suspend fun isCategoryExists(name: String, parentId: Long?): Boolean {
        return categoryDefDao.getCategoryByName(name, parentId) != null
    }

    override suspend fun isCategoryExists(name: String, parentId: Long?, flowSign: Int): Boolean {
        return categoryDefDao.getCategoryByNameAndSign(name, parentId, flowSign) != null
    }

    override suspend fun restoreDefaultCategories() {
        val defaults = CategoryDefaults.allCategories
        val existing = categoryDefDao.getAllCategoriesOnce()

        // 1. 恢复一级分类排序和颜色：将已存在的一级分类 sortOrder/color 重置为默认值
        for (default in defaults) {
            val match = existing.find { it.name == default.name && it.parentId == null }
            if (match != null) {
                val needsSortUpdate = match.sortOrder != default.sortOrder
                val needsColorUpdate = match.color == 0L && default.color != 0L
                if (needsSortUpdate || needsColorUpdate) {
                    categoryDefDao.updateCategory(
                        match.copy(
                            sortOrder = if (needsSortUpdate) default.sortOrder else match.sortOrder,
                            color = if (needsColorUpdate) default.color else match.color
                        )
                    )
                }
            }
        }

        // 2. 补充缺失的一级分类（按 name + parentId + flowSign 三元组去重）
        val existingNames = existing.map { Triple(it.name, it.parentId, it.flowSign) }.toSet()
        val toAdd = defaults.filter { Triple(it.name, it.parentId, it.flowSign) !in existingNames }
            .map { toEntity(it) }
        if (toAdd.isNotEmpty()) {
            categoryDefDao.insertCategories(toAdd)
        }

        // 3. 恢复二级分类：补全缺失的二级分类（按 name + parentId 去重）
        val subDefaults = CategoryDefaults.subCategories
        for ((parentName, subs) in subDefaults) {
            val parent = categoryDefDao.getCategoryByName(parentName, null) ?: continue
            val existingSubs = categoryDefDao.getSubCategoriesOnce(parent.id)
            val existingSubNames = existingSubs.map { it.name }.toSet()

            for ((index, subName) in subs.withIndex()) {
                if (subName !in existingSubNames) {
                    val subColor = if (parent.flowSign >= 0) CategoryDefaults.incomeDefaultColor
                                  else CategoryDefaults.expenseDefaultColor
                    categoryDefDao.insertCategory(
                        CategoryDefEntity(
                            name = subName,
                            parentId = parent.id,
                            isDefault = true,
                            sortOrder = index + 1,
                            flowSign = parent.flowSign,
                            color = subColor
                        )
                    )
                }
            }
        }
    }

    override suspend fun mergeCategoryAndDelete(sourceId: Long, targetId: Long) {
        val source = categoryDefDao.getCategoryById(sourceId)
            ?: throw IllegalArgumentException("源分类不存在")
        val target = categoryDefDao.getCategoryById(targetId)
            ?: throw IllegalArgumentException("目标分类不存在")

        if (source.parentId == null) {
            // 一级分类合并
            // 1. 将该一级分类下的所有流水迁移到目标一级分类
            transactionDao.updateCategoryL1Id(sourceId, targetId)
            transactionDao.updateCategoryL1Snapshot(targetId, target.name)
            // 2. 处理源分类的二级分类：按名称匹配迁移到目标的二级分类下
            val sourceSubs = categoryDefDao.getSubCategoriesOnce(sourceId)
            val targetSubs = categoryDefDao.getSubCategoriesOnce(targetId)
            for (sub in sourceSubs) {
                val matchingTarget = targetSubs.find { it.name == sub.name }
                if (matchingTarget != null) {
                    // 同名二级：流水归入目标二级，删除源二级
                    transactionDao.updateCategoryL2Id(sub.id, matchingTarget.id)
                    transactionDao.updateCategoryL2Snapshot(matchingTarget.id, matchingTarget.name)
                    categoryDefDao.deleteCategory(sub.id)
                } else {
                    // 不同名二级：将二级分类的 parentId 改为目标
                    val newSortOrder = categoryDefDao.getMaxSortOrder(targetId) + 1
                    categoryDefDao.updateCategory(
                        sub.copy(parentId = targetId, sortOrder = newSortOrder)
                    )
                }
            }
            // 3. 删除源一级分类
            categoryDefDao.deleteCategory(sourceId)
        } else {
            // 二级分类合并
            transactionDao.updateCategoryL2Id(sourceId, targetId)
            transactionDao.updateCategoryL2Snapshot(targetId, target.name)
            categoryDefDao.deleteCategory(sourceId)
        }
    }

    private fun toDomain(entity: CategoryDefEntity): Category = Category(
        id = entity.id,
        name = entity.name,
        parentId = entity.parentId,
        isDefault = entity.isDefault,
        isHidden = entity.isHidden,
        sortOrder = entity.sortOrder,
        flowSign = entity.flowSign,
        color = entity.color
    )

    private fun toEntity(model: Category): CategoryDefEntity = CategoryDefEntity(
        id = model.id,
        name = model.name,
        parentId = model.parentId,
        isDefault = model.isDefault,
        isHidden = model.isHidden,
        sortOrder = model.sortOrder,
        flowSign = model.flowSign,
        color = model.color
    )
}
