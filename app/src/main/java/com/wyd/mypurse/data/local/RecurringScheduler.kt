package com.wyd.mypurse.data.local

import android.util.Log
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.RecurringTemplateRepository
import com.wyd.mypurse.domain.repository.TransactionInsertSpec
import com.wyd.mypurse.domain.repository.TransactionRepository
import java.util.Calendar
import java.util.GregorianCalendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V1.1 自动记账调度器。
 *
 * 每次 App 冷启动时调用 [execute]，检查所有启用模板的遗漏周期并补记。
 *
 * 核心逻辑：
 * 1. 查询所有 isActive=true 的模板
 * 2. 对每个模板，生成 [上次执行, 今天] 范围内所有应执行日期（最多回溯 90 天）
 * 3. 对每个日期检查是否已有同一 recurringTemplateId 的记录（防重复）
 * 4. 未创建记录的日期，创建对应流水
 * 5. 更新 lastExecutedDate 和 nextExecutionDate
 *
 * 性能约束：不增加冷启动耗时超过 200ms。
 */
@Singleton
class RecurringScheduler @Inject constructor(
    private val templateRepository: RecurringTemplateRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    companion object {
        private const val TAG = "RecurringScheduler"
        /** 最多补记过去 90 天 */
        private const val MAX_BACKFILL_DAYS = 90
        private const val ONE_DAY_MS = 86_400_000L
    }

    /**
     * 执行自动记账检查。应在冷启动时调用，不阻塞 UI。
     * 返回创建的记录数（用于日志）。
     */
    suspend fun execute(): Int {
        val startTime = System.currentTimeMillis()
        var createdCount = 0

        try {
            val templates = templateRepository.getActiveTemplates()
            if (templates.isEmpty()) return 0

            val today = dayStart(Calendar.getInstance())

            for (template in templates) {
                try {
                    // 确定回溯起点
                    val lastExec = template.lastExecutedDate
                    val effectiveStart = if (lastExec != null) {
                        // 从上次执行日期的后一天开始
                        dayStartOf(lastExec) + ONE_DAY_MS
                    } else {
                        // 从未执行过，从模板创建日开始
                        val createdDay = dayStartOf(template.createTime)
                        // 但首次执行时 nextExecutionDate 应该已被正确设置，
                        // 以 nextExecutionDate 为准作为起始
                        val nextDay = dayStartOf(template.nextExecutionDate)
                        maxOf(createdDay, nextDay)
                    }

                    // 回溯范围上限：90 天
                    val backfillLimit = today - ONE_DAY_MS * MAX_BACKFILL_DAYS
                    val rangeStart = maxOf(effectiveStart, backfillLimit)

                    if (rangeStart > today) continue // 没有需要补记的

                    // 生成范围内所有应执行日期
                    val calendar = GregorianCalendar()
                    calendar.timeInMillis = rangeStart

                    val pendingSpecs = mutableListOf<TransactionInsertSpec>()
                    var latestExecuted: Long? = lastExec

                    while (calendar.timeInMillis <= today) {
                        if (matchesTemplate(calendar, template.cycleType, template.cycleValue)) {
                            val dateMs = calendar.timeInMillis
                            val dayEndMs = dayEnd(calendar)

                            // 防重复
                            val count = transactionRepository.countByTemplateAndDate(
                                templateId = template.id,
                                dayStart = dateMs,
                                dayEnd = dayEndMs
                            )
                            if (count == 0) {
                                // 解析分类名称
                                val l1Name = template.categoryL1Id?.let {
                                    categoryRepository.getCategoryById(it)?.name
                                } ?: ""
                                val l2Name = template.categoryL2Id?.let {
                                    categoryRepository.getCategoryById(it)?.name
                                }

                                pendingSpecs.add(
                                    TransactionInsertSpec(
                                        flowType = template.flowType,
                                        categoryL1Id = template.categoryL1Id,
                                        categoryL2Id = template.categoryL2Id,
                                        categoryL1 = l1Name,
                                        categoryL2 = l2Name,
                                        amount = template.amount,
                                        note = template.note,
                                        date = dateMs,
                                        recurringTemplateId = template.id
                                    )
                                )
                            }
                            latestExecuted = dateMs
                        }
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    // 批量写入（单事务），避免逐条插入被中断导致模板状态不一致
                    if (pendingSpecs.isNotEmpty()) {
                        transactionRepository.insertTransactions(pendingSpecs)
                        createdCount += pendingSpecs.size
                        Log.i(TAG, "批量创建自动记录: templateId=${template.id}, count=${pendingSpecs.size}")
                    }

                    // 更新模板状态
                    if (latestExecuted != null) {
                        val nextExec = calcNextExecutionDate(latestExecuted, template.cycleType, template.cycleValue)
                        templateRepository.updateTemplate(
                            template.copy(
                                lastExecutedDate = latestExecuted,
                                nextExecutionDate = nextExec
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理模板 ${template.id} 失败", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "自动记账执行失败", e)
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.i(TAG, "自动记账完成: 创建 $createdCount 条记录, 耗时 ${elapsed}ms")
        return createdCount
    }

    // ========== 日期工具 ==========

    private fun dayStart(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayStartOf(timestamp: Long): Long {
        val cal = GregorianCalendar()
        cal.timeInMillis = timestamp
        return dayStart(cal)
    }

    private fun dayEnd(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    // ========== 周期匹配 ==========

    /**
     * 判断指定日期是否匹配周期性模板的执行日。
     */
    private fun matchesTemplate(cal: Calendar, cycleType: String, cycleValue: Long): Boolean {
        return when (cycleType) {
            "DAILY" -> true // 每天
            "WEEKLY" -> matchesWeekday(cal, cycleValue.toInt())
            "MONTHLY" -> matchesMonthlyDay(cal, cycleValue.toInt())
            "YEARLY" -> matchesYearlyDate(cal, cycleValue.toInt())
            else -> false
        }
    }

    /** WEEKLY: cycleValue 1=周一 ~ 7=周日 */
    private fun matchesWeekday(cal: Calendar, weekday: Int): Boolean {
        val calDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        // 映射: weekday 1→MONDAY(2), 7→SUNDAY(1)
        val expected = when (weekday) {
            7 -> Calendar.SUNDAY
            else -> weekday + 1
        }
        return calDayOfWeek == expected
    }

    /** MONTHLY: cycleValue 1-31，当月无此日期时退到月末最后一天 */
    private fun matchesMonthlyDay(cal: Calendar, day: Int): Boolean {
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = day.coerceAtMost(lastDay)
        return todayDay == targetDay
    }

    /** YEARLY: cycleValue = MMDD 格式，如 0101 表示 1 月 1 日 */
    private fun matchesYearlyDate(cal: Calendar, mmdd: Int): Boolean {
        val month = mmdd / 100
        val day = mmdd % 100
        val calMonth = cal.get(Calendar.MONTH) + 1
        val calDay = cal.get(Calendar.DAY_OF_MONTH)
        if (calMonth != month) return false

        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = day.coerceAtMost(lastDay)
        return calDay == targetDay
    }

    // ========== 下次执行日期计算 ==========

    /**
     * 给定最近一次执行日期，计算下一次执行日期。
     */
    private fun calcNextExecutionDate(lastExecuted: Long, cycleType: String, cycleValue: Long): Long {
        val cal = GregorianCalendar()
        cal.timeInMillis = lastExecuted
        return when (cycleType) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            "WEEKLY" -> {
                val weekday = cycleValue.toInt()
                val expected = when (weekday) {
                    7 -> Calendar.SUNDAY
                    else -> weekday + 1
                }
                // 找到下一周的同天
                cal.add(Calendar.DAY_OF_MONTH, 1)
                while (cal.get(Calendar.DAY_OF_WEEK) != expected) {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                cal.timeInMillis
            }
            "MONTHLY" -> {
                val targetDay = cycleValue.toInt()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(lastDay))
                cal.timeInMillis
            }
            "YEARLY" -> {
                val month = (cycleValue / 100).toInt()
                val day = (cycleValue % 100).toInt()
                cal.add(Calendar.YEAR, 1)
                cal.set(Calendar.MONTH, month - 1)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(lastDay))
                cal.timeInMillis
            }
            else -> lastExecuted // fallback
        }
    }

}
