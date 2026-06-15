package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wyd.mypurse.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * 预算数据访问对象。阶段 3 完整实现，阶段 1 建基础结构。
 */
@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budget WHERE year = :year AND month = :month")
    suspend fun getBudget(year: Int, month: Int): BudgetEntity?

    @Query("SELECT * FROM budget ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
}
