package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wyd.mypurse.data.local.entity.CategoryDefEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类定义数据访问对象。
 */
@Dao
interface CategoryDefDao {

    /** 获取所有未隐藏的分类，按 sortOrder 排序 */
    @Query("SELECT * FROM category_def WHERE is_hidden = 0 ORDER BY sort_order ASC")
    fun getAllCategories(): Flow<List<CategoryDefEntity>>

    /** 获取所有分类（含隐藏），按 sortOrder 排序 */
    @Query("SELECT * FROM category_def ORDER BY sort_order ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryDefEntity>

    /** 获取指定 flowSign 的所有一级分类（parentId 为 null） */
    @Query("SELECT * FROM category_def WHERE parent_id IS NULL AND is_hidden = 0 AND flow_sign = :flowSign ORDER BY sort_order ASC")
    fun getTopLevelCategoriesBySign(flowSign: Int): Flow<List<CategoryDefEntity>>

    /** 获取指定 flowSign 的所有一级分类（一次性） */
    @Query("SELECT * FROM category_def WHERE parent_id IS NULL AND is_hidden = 0 AND flow_sign = :flowSign ORDER BY sort_order ASC")
    suspend fun getTopLevelCategoriesBySignOnce(flowSign: Int): List<CategoryDefEntity>

    /** 获取所有一级分类（parentId 为 null） */
    @Query("SELECT * FROM category_def WHERE parent_id IS NULL AND is_hidden = 0 ORDER BY sort_order ASC")
    fun getTopLevelCategories(): Flow<List<CategoryDefEntity>>

    /** 获取某一级分类下的所有二级分类 */
    @Query("SELECT * FROM category_def WHERE parent_id = :parentId AND is_hidden = 0 ORDER BY sort_order ASC")
    fun getSubCategories(parentId: Long): Flow<List<CategoryDefEntity>>

    /** 获取某一级分类下的所有二级分类（一次性） */
    @Query("SELECT * FROM category_def WHERE parent_id = :parentId AND is_hidden = 0 ORDER BY sort_order ASC")
    suspend fun getSubCategoriesOnce(parentId: Long): List<CategoryDefEntity>

    /** 根据 ID 获取单个分类 */
    @Query("SELECT * FROM category_def WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryDefEntity?

    /** 根据名称和父级查找分类 */
    @Query("SELECT * FROM category_def WHERE name = :name AND parent_id IS :parentId")
    suspend fun getCategoryByName(name: String, parentId: Long?): CategoryDefEntity?

    /** 插入分类 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryDefEntity): Long

    /** 批量插入分类 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryDefEntity>)

    /** 更新分类 */
    @Update
    suspend fun updateCategory(category: CategoryDefEntity)

    /** 删除分类 */
    @Query("DELETE FROM category_def WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    /** 获取最大 sortOrder 值 */
    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM category_def WHERE parent_id IS :parentId")
    suspend fun getMaxSortOrder(parentId: Long?): Int

    /** 批量更新排序 */
    @Query("UPDATE category_def SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /** 批量更新排序（一次事务中完成） */
    @androidx.room.Transaction
    suspend fun batchUpdateSortOrder(sortMap: Map<Long, Int>) {
        for ((id, order) in sortMap) {
            updateSortOrder(id, order)
        }
    }
}
