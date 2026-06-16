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
        flowSign: Int
    ): Long {
        val maxOrder = categoryDefDao.getMaxSortOrder(parentId)
        val entity = CategoryDefEntity(
            name = name,
            parentId = parentId,
            isDefault = isDefault,
            sortOrder = maxOrder + 1,
            flowSign = flowSign
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

        // 1. 恢复一级分类排序：将已存在的一级分类 sortOrder 重置为默认值
        for (default in defaults) {
            val match = existing.find { it.name == default.name && it.parentId == null }
            if (match != null && match.sortOrder != default.sortOrder) {
                categoryDefDao.updateSortOrder(match.id, default.sortOrder)
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
                    categoryDefDao.insertCategory(
                        CategoryDefEntity(
                            name = subName,
                            parentId = parent.id,
                            isDefault = true,
                            sortOrder = index + 1,
                            flowSign = parent.flowSign
                        )
                    )
                }
            }
        }
    }

    private fun toDomain(entity: CategoryDefEntity): Category = Category(
        id = entity.id,
        name = entity.name,
        parentId = entity.parentId,
        isDefault = entity.isDefault,
        isHidden = entity.isHidden,
        sortOrder = entity.sortOrder,
        flowSign = entity.flowSign
    )

    private fun toEntity(model: Category): CategoryDefEntity = CategoryDefEntity(
        id = model.id,
        name = model.name,
        parentId = model.parentId,
        isDefault = model.isDefault,
        isHidden = model.isHidden,
        sortOrder = model.sortOrder,
        flowSign = model.flowSign
    )
}
