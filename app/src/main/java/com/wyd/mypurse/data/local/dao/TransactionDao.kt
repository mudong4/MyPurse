package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wyd.mypurse.data.local.entity.CategoryAmountEntity
import com.wyd.mypurse.data.local.entity.MonthlyAmountEntity
import com.wyd.mypurse.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * 收支记录数据访问对象。聚合查询用于统计和首页。
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /** V1.1.x 批量插入（自动事务包裹），供 RecurringScheduler 使用 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(entities: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM `transaction` ORDER BY date DESC, create_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("DELETE FROM `transaction` WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    /** 删除指定一级分类的所有流水记录 */
    @Query("DELETE FROM `transaction` WHERE category_l1_id = :categoryId")
    suspend fun deleteTransactionsByCategoryL1Id(categoryId: Long)

    /** 删除指定二级分类的所有流水记录 */
    @Query("DELETE FROM `transaction` WHERE category_l2_id = :categoryId")
    suspend fun deleteTransactionsByCategoryL2Id(categoryId: Long)

    // ========== 流水列表查询 ==========

    /**
     * 按时间范围获取交易列表（Flow，支持分页）。
     */
    @Query("""
        SELECT * FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
        ORDER BY date DESC, create_time DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getTransactionsByRange(
        rangeStart: Long,
        rangeEnd: Long,
        limit: Int,
        offset: Int
    ): Flow<List<TransactionEntity>>

    /**
     * 按时间范围 + 一级分类 ID 筛选获取交易列表。
     * 使用 category_l1_id 过滤，不依赖快照名，分类重命名后旧交易仍能正确匹配。
     */
    @Query("""
        SELECT * FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
          AND category_l1_id = :categoryL1Id
        ORDER BY date DESC, create_time DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getTransactionsByRangeAndCategoryL1Id(
        rangeStart: Long,
        rangeEnd: Long,
        categoryL1Id: Long,
        limit: Int,
        offset: Int
    ): Flow<List<TransactionEntity>>

    /**
     * 搜索交易记录（匹配备注、分类名、金额）。
     */
    @Query("""
        SELECT * FROM `transaction`
        WHERE note LIKE '%' || :keyword || '%'
           OR category_l1 LIKE '%' || :keyword || '%'
           OR category_l2 LIKE '%' || :keyword || '%'
           OR CAST(amount AS TEXT) LIKE '%' || :keyword || '%'
        ORDER BY date DESC, create_time DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchTransactions(
        keyword: String,
        limit: Int,
        offset: Int
    ): Flow<List<TransactionEntity>>

    /**
     * 获取指定月份的交易数量（用于分页判断）。
     */
    @Query("""
        SELECT COUNT(*) FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
    """)
    suspend fun getTransactionCountByRange(rangeStart: Long, rangeEnd: Long): Int

    /**
     * 获取指定月份各天是否有交易记录（用于日历标记）。
     */
    @Query("""
        SELECT DISTINCT 
            CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year,
            CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) AS month,
            CAST(strftime('%d', date / 1000, 'unixepoch') AS INTEGER) AS day
        FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
    """)
    suspend fun getActiveDaysInRange(rangeStart: Long, rangeEnd: Long): List<ActiveDay>

    // ========== 聚合查询 ==========

    /**
     * 获取总余额：全部收入 SUM - 全部支出 SUM。
     */
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM `transaction` WHERE flow_type != '支出'),
            0
        ) - COALESCE(
            (SELECT SUM(amount) FROM `transaction` WHERE flow_type = '支出'),
            0
        ) AS balance
    """)
    fun getBalance(): Flow<BigDecimal>

    /**
     * 指定日期的支出和收入合计。
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN flow_type = '支出' THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN flow_type != '支出' THEN amount ELSE 0 END), 0) AS income
        FROM `transaction`
        WHERE date BETWEEN :dayStart AND :dayEnd
    """)
    suspend fun getDaySummary(dayStart: Long, dayEnd: Long): DaySummaryTuple

    /**
     * 指定时间范围内的支出和收入合计。
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN flow_type = '支出' THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN flow_type != '支出' THEN amount ELSE 0 END), 0) AS income
        FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
    """)
    suspend fun getRangeSummary(rangeStart: Long, rangeEnd: Long): DaySummaryTuple

    /**
     * 近 N 个月每月支出趋势（按 yyyy-MM 分组）。
     */
    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year,
            CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) AS month,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date >= :sinceTimestamp
        GROUP BY year, month
        ORDER BY year, month
    """)
    fun getMonthlyExpenseTrend(sinceTimestamp: Long): Flow<List<MonthlyAmountEntity>>

    /**
     * 指定月份按一级分类汇总的支出排名。
     * JOIN category_def 获取分类颜色和实时名称。
     * 业务保证 category_l1_id 不会指向已删除的分类（删除分类必级联删/迁移流水）。
     */
    @Query("""
        SELECT 
            t.category_l1_id AS categoryL1Id,
            cd.name AS categoryL1,
            SUM(t.amount) AS total,
            cd.color AS color
        FROM `transaction` t
        JOIN category_def cd ON t.category_l1_id = cd.id
        WHERE t.flow_type = '支出' 
          AND CAST(strftime('%Y', t.date / 1000, 'unixepoch') AS INTEGER) = :year
          AND CAST(strftime('%m', t.date / 1000, 'unixepoch') AS INTEGER) = :month
        GROUP BY t.category_l1_id
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopCategoriesByMonth(year: Int, month: Int, limit: Int): List<CategoryAmountEntity>

    // ========== 统计页聚合查询 ==========

    /**
     * 按时间范围和流水类型汇总各一级分类金额（构成分析）。
     * JOIN category_def 获取分类颜色和实时名称。
     * 业务保证 category_l1_id 不会指向已删除的分类（删除分类必级联删/迁移流水）。
     */
    @Query("""
        SELECT 
            t.category_l1_id AS categoryL1Id,
            cd.name AS categoryL1,
            SUM(t.amount) AS total,
            cd.color AS color
        FROM `transaction` t
        JOIN category_def cd ON t.category_l1_id = cd.id
        WHERE t.date BETWEEN :rangeStart AND :rangeEnd
          AND (:flowType IS NULL OR t.flow_type = :flowType)
        GROUP BY t.category_l1_id
        ORDER BY total DESC
    """)
    suspend fun getCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?
    ): List<CategoryAmountEntity>

    /**
     * 按时间范围和一级分类汇总二级分类金额（构成分析 - 展开二级）。
     * 二级分类颜色取父级分类（category_def）的颜色，名称取实时名。
     * 业务保证 category_l2_id 不会指向已删除的分类（删除分类必级联删/迁移流水）。
     */
    @Query("""
        SELECT 
            cd.name AS categoryL1,
            SUM(t.amount) AS total,
            cd.color AS color
        FROM `transaction` t
        JOIN category_def cd ON t.category_l2_id = cd.id
        WHERE t.date BETWEEN :rangeStart AND :rangeEnd
          AND (:flowType IS NULL OR t.flow_type = :flowType)
          AND t.category_l1_id = :categoryL1Id
          AND t.category_l2 IS NOT NULL
        GROUP BY t.category_l2_id
        ORDER BY total DESC
    """)
    suspend fun getSubCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?,
        categoryL1Id: Long
    ): List<CategoryAmountEntity>

    /**
     * 按日分组趋势（过去 N 天每天支出合计）。
     */
    @Query("""
        SELECT 
            strftime('%Y-%m-%d', date / 1000, 'unixepoch') AS label,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date BETWEEN :rangeStart AND :rangeEnd
        GROUP BY label
        ORDER BY label
    """)
    suspend fun getDailyExpenseTrend(rangeStart: Long, rangeEnd: Long): List<TrendTuple>

    /**
     * 按周分组趋势（过去 N 周每周支出合计）。
     */
    @Query("""
        SELECT 
            strftime('%Y', date / 1000, 'unixepoch') || '-W' ||
            CASE 
                WHEN CAST(strftime('%W', date / 1000, 'unixepoch') AS INTEGER) < 10 
                THEN '0' || strftime('%W', date / 1000, 'unixepoch')
                ELSE strftime('%W', date / 1000, 'unixepoch')
            END AS label,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date BETWEEN :rangeStart AND :rangeEnd
        GROUP BY label
        ORDER BY label
    """)
    suspend fun getWeeklyExpenseTrend(rangeStart: Long, rangeEnd: Long): List<TrendTuple>

    /**
     * 按季度分组趋势。
     */
    @Query("""
        SELECT 
            strftime('%Y', date / 1000, 'unixepoch') || '-Q' ||
            CAST((CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) + 2) / 3 AS TEXT) AS label,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date BETWEEN :rangeStart AND :rangeEnd
        GROUP BY label
        ORDER BY label
    """)
    suspend fun getQuarterlyExpenseTrend(rangeStart: Long, rangeEnd: Long): List<TrendTuple>

    /**
     * 按年分组趋势。
     */
    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) || '' AS label,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出'
        GROUP BY label
        ORDER BY label
    """)
    suspend fun getYearlyExpenseTrend(): List<TrendTuple>

    /**
     * 按月分组趋势（一次性查询版本，供统计页使用）。
     */
    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) || '-' ||
            CASE 
                WHEN CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) < 10 
                THEN '0' || strftime('%m', date / 1000, 'unixepoch')
                ELSE strftime('%m', date / 1000, 'unixepoch')
            END AS label,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date BETWEEN :rangeStart AND :rangeEnd
        GROUP BY label
        ORDER BY label
    """)
    suspend fun getMonthlyExpenseTrendOnce(rangeStart: Long, rangeEnd: Long): List<TrendTuple>

    /**
     * 获取数据库中所有有交易记录的年份（降序）。
     */
    @Query("""
        SELECT DISTINCT CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year
        FROM `transaction`
        ORDER BY year DESC
    """)
    suspend fun getAvailableYears(): List<Int>

    // ========== 数据管理 ==========

    /** 一次性获取全部交易记录（供 CSV 导出使用） */
    @Query("SELECT * FROM `transaction` ORDER BY date DESC, create_time DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    /** 一次性按时间范围获取交易记录（供 CSV 导出使用） */
    @Query("""
        SELECT * FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
        ORDER BY date DESC, create_time DESC
    """)
    suspend fun getTransactionsByRangeOnce(rangeStart: Long, rangeEnd: Long): List<TransactionEntity>

    /** 清空全部交易记录 */
    @Query("DELETE FROM `transaction`")
    suspend fun deleteAllTransactions()

    /** 获取总记录数 */
    @Query("SELECT COUNT(*) FROM `transaction`")
    suspend fun getTransactionCount(): Int

    /** 检查指定模板在指定日期是否已有记录（防重复） */
    @Query("SELECT COUNT(*) FROM `transaction` WHERE recurring_template_id = :templateId AND date BETWEEN :dayStart AND :dayEnd")
    suspend fun countByTemplateAndDate(templateId: Long, dayStart: Long, dayEnd: Long): Int

    // ========== V1.2 合并分类迁移 ==========

    /** 将所有一级分类 ID 为 oldId 的流水更新为 newId */
    @Query("UPDATE `transaction` SET category_l1_id = :newId WHERE category_l1_id = :oldId")
    suspend fun updateCategoryL1Id(oldId: Long, newId: Long)

    /** 将所有二级分类 ID 为 oldId 的流水更新为 newId */
    @Query("UPDATE `transaction` SET category_l2_id = :newId WHERE category_l2_id = :oldId")
    suspend fun updateCategoryL2Id(oldId: Long, newId: Long)

    /** 批量更新一级分类快照名（分类改名/合并时同步） */
    @Query("UPDATE `transaction` SET category_l1 = :newName WHERE category_l1_id = :categoryId")
    suspend fun updateCategoryL1Snapshot(categoryId: Long, newName: String)

    /** 批量更新二级分类快照名（分类改名/合并时同步） */
    @Query("UPDATE `transaction` SET category_l2 = :newName WHERE category_l2_id = :categoryId")
    suspend fun updateCategoryL2Snapshot(categoryId: Long, newName: String)
}

/**
 * 日/周/月/年汇总查询的中间结果。
 */
data class DaySummaryTuple(
    val expense: BigDecimal,
    val income: BigDecimal
)

/**
 * 活跃日期（有交易记录的日期）。
 */
data class ActiveDay(
    val year: Int,
    val month: Int,
    val day: Int
)

/**
 * 趋势查询中间结果（标签 + 金额）。label 格式因粒度而异（YYYY-MM-DD / YYYY-Www / YYYY-Qn / YYYY）。
 */
data class TrendTuple(
    val label: String,
    val total: BigDecimal
)

