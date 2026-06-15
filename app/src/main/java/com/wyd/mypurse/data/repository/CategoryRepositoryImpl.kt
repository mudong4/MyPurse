package com.wyd.mypurse.data.repository

import com.wyd.mypurse.data.local.dao.CategoryDefDao
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
    private val categoryDefDao: CategoryDefDao
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
        icon: String,
        isDefault: Boolean,
        flowSign: Int
    ): Long {
        val maxOrder = categoryDefDao.getMaxSortOrder(parentId)
        val entity = CategoryDefEntity(
            name = name,
            parentId = parentId,
            icon = icon,
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

    override suspend fun updateCategory(id: Long, name: String, icon: String) {
        val existing = categoryDefDao.getCategoryById(id) ?: return
        categoryDefDao.updateCategory(existing.copy(name = name, icon = icon))
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
        // TODO: 阶段 2 实现——需要删除 transaction 表中关联记录
        categoryDefDao.deleteCategory(categoryId)
    }

    override suspend fun migrateCategoryRecords(
        fromCategoryId: Long,
        toCategoryL1Id: Long?,
        toCategoryL2Id: Long?,
        toCategoryL1Name: String,
        toCategoryL2Name: String?
    ) {
        // TODO: 阶段 2 实现——需要批量更新 transaction 表
    }

    override suspend fun isCategoryExists(name: String, parentId: Long?): Boolean {
        return categoryDefDao.getCategoryByName(name, parentId) != null
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

        // 2. 补充缺失的一级分类
        val existingNames = existing.map { it.name to it.parentId }.toSet()
        val toAdd = defaults.filter { (it.name to it.parentId) !in existingNames }
            .map { toEntity(it) }
        if (toAdd.isNotEmpty()) {
            categoryDefDao.insertCategories(toAdd)
        }

        // 3. 恢复二级分类：补全缺失的二级分类
        val subDefaults = CategoryDefaults.subCategories
        for ((parentName, subs) in subDefaults) {
            val parent = categoryDefDao.getCategoryByName(parentName, null) ?: continue
            val existingSubs = categoryDefDao.getSubCategoriesOnce(parent.id)
            val existingSubNames = existingSubs.map { it.name }.toSet()

            for ((index, sub) in subs.withIndex()) {
                val (subName, subIcon) = sub
                if (subName !in existingSubNames) {
                    categoryDefDao.insertCategory(
                        CategoryDefEntity(
                            name = subName,
                            parentId = parent.id,
                            icon = subIcon,
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
        icon = entity.icon,
        isDefault = entity.isDefault,
        isHidden = entity.isHidden,
        sortOrder = entity.sortOrder,
        flowSign = entity.flowSign
    )

    private fun toEntity(model: Category): CategoryDefEntity = CategoryDefEntity(
        id = model.id,
        name = model.name,
        parentId = model.parentId,
        icon = model.icon,
        isDefault = model.isDefault,
        isHidden = model.isHidden,
        sortOrder = model.sortOrder,
        flowSign = model.flowSign
    )
}
