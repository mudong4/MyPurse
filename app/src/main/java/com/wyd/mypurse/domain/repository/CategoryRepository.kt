package com.wyd.mypurse.domain.repository

import com.wyd.mypurse.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 分类管理 Repository 接口。定义在 domain 层，不依赖 Android 框架。
 */
interface CategoryRepository {

    /** 获取所有分类（含隐藏），实时 Flow */
    fun getAllCategories(): Flow<List<Category>>

    /** 获取所有分类（一次性） */
    suspend fun getAllCategoriesOnce(): List<Category>

    /** 获取所有一级分类 */
    fun getTopLevelCategories(): Flow<List<Category>>

    /** 获取指定 flowSign 的一级分类（实时） */
    fun getTopLevelCategoriesBySign(flowSign: Int): Flow<List<Category>>

    /** 获取指定 flowSign 的一级分类（一次性） */
    suspend fun getTopLevelCategoriesBySignOnce(flowSign: Int): List<Category>

    /** 获取某级下的二级分类 */
    fun getSubCategories(parentId: Long): Flow<List<Category>>

    /** 获取某级下的二级分类（一次性） */
    suspend fun getSubCategoriesOnce(parentId: Long): List<Category>

    /** 根据 ID 获取分类 */
    suspend fun getCategoryById(id: Long): Category?

    /** 新增分类，返回新 ID。color=0 时自动取轮转色 */
    suspend fun addCategory(name: String, parentId: Long?, isDefault: Boolean, flowSign: Int = -1, color: Long = 0): Long

    /** 批量新增分类（用于恢复默认） */
    suspend fun addCategories(categories: List<Category>)

    /** 更新分类名称 */
    suspend fun updateCategory(id: Long, name: String)

    /** V1.1 更新分类颜色 */
    suspend fun updateCategoryColor(id: Long, color: Long)

    /** 更新分类排序 */
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /** 批量更新分类排序（一次事务） */
    suspend fun batchUpdateSortOrder(sortMap: Map<Long, Int>)

    /** 删除分类（返回被删分类信息，供迁移用） */
    suspend fun deleteCategory(id: Long): Category?

    /** 删除分类及所有关联记录（含子分类和流水） */
    suspend fun deleteCategoryWithRecords(categoryId: Long)

    /** 判断分类是否已存在（仅按 name + parentId 检查） */
    suspend fun isCategoryExists(name: String, parentId: Long?): Boolean

    /** 判断分类是否已存在（按 name + parentId + flowSign 精确检查） */
    suspend fun isCategoryExists(name: String, parentId: Long?, flowSign: Int): Boolean

    /** 恢复默认分类（跳过已存在的） */
    suspend fun restoreDefaultCategories()

    /** V1.2 合并分类：将源分类下的流水迁移到目标分类后删除源分类 */
    suspend fun mergeCategoryAndDelete(sourceId: Long, targetId: Long)
}
