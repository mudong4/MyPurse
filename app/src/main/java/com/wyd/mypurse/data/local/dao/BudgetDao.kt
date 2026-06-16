package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wyd.mypurse.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * 预算数据访问对象。极简方案：仅操作一条记录（id=1）。
 */
@Dao
interface BudgetDao {

    /** 插入或更新预算（id=1 唯一，冲突时覆盖） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    /** 获取当前预算，null 表示从未设置 */
    @Query("SELECT * FROM budget WHERE id = 1")
    suspend fun getBudget(): BudgetEntity?

    /** 监听预算变化（用于首页实时更新） */
    @Query("SELECT * FROM budget WHERE id = 1")
    fun observeBudget(): Flow<BudgetEntity?>

    /** 删除预算记录（取消预算） */
    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}
