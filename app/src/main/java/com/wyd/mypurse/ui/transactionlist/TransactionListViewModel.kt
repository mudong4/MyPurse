package com.wyd.mypurse.ui.transactionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentOffset = 0
    private val pageSize = 50

    /** 非分页视图的最大加载量，替代 Int.MAX_VALUE 作为 limit 参数 */
    private val maxLoadSize = 500

    fun initialize(
        granularity: String,
        categoryFilter: String?,
        timeRangeStart: Long?,
        timeRangeEnd: Long?
    ) {
        val g = TimeGranularity.fromString(granularity)
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val day = now.get(Calendar.DAY_OF_MONTH)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)

        var targetYear = year
        var targetMonth = month
        var targetDay = day
        var targetWeekOfYear = weekOfYear

        if (timeRangeStart != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = timeRangeStart }
            targetYear = cal.get(Calendar.YEAR)
            targetMonth = cal.get(Calendar.MONTH) + 1
            targetDay = cal.get(Calendar.DAY_OF_MONTH)
            targetWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        }

        _uiState.update {
            it.copy(
                granularity = g,
                currentYear = targetYear,
                currentMonth = targetMonth,
                currentDay = targetDay,
                currentWeekOfYear = targetWeekOfYear,
                weekDateRange = computeWeekDateRange(targetYear, targetWeekOfYear)
            )
        }
        loadData()
    }

    // ========== 粒度 ==========

    fun setGranularity(granularity: TimeGranularity) {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val day = now.get(Calendar.DAY_OF_MONTH)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)

        _uiState.update {
            it.copy(
                granularity = granularity,
                currentYear = year,
                currentMonth = month,
                currentDay = day,
                currentWeekOfYear = weekOfYear,
                weekDateRange = computeWeekDateRange(year, weekOfYear),
                expandedGroups = emptySet()
            )
        }
        currentOffset = 0
        loadData()
    }

    // ========== 分组模式（分类维度）==========

    /**
     * 设置分组模式：TIME / CATEGORY_L1 / CATEGORY_L2
     */
    fun setGroupMode(mode: GroupMode) {
        _uiState.update {
            it.copy(
                groupMode = mode,
                expandedGroups = emptySet()
            )
        }
        currentOffset = 0
        loadData()
    }

    // ========== 折叠 ==========

    fun toggleGroupExpanded(groupKey: String) {
        _uiState.update { state ->
            val newSet = state.expandedGroups.toMutableSet()
            if (newSet.contains(groupKey)) {
                newSet.remove(groupKey)
            } else {
                newSet.add(groupKey)
            }
            state.copy(expandedGroups = newSet)
        }
    }

    // ========== 搜索 ==========

    fun setSearchKeyword(keyword: String) {
        _uiState.update { it.copy(searchKeyword = keyword) }
        currentOffset = 0
        if (keyword.isBlank()) {
            loadData()
        } else {
            searchData(keyword)
        }
    }

    // ========== 时间导航 ==========

    fun navigateTime(direction: Int) {
        val state = _uiState.value
        val cal = when (state.granularity) {
            TimeGranularity.DAY -> Calendar.getInstance().apply {
                set(state.currentYear, state.currentMonth - 1, state.currentDay)
            }
            TimeGranularity.WEEK -> getWeekStartCalendar(state.currentYear, state.currentWeekOfYear)
            TimeGranularity.MONTH -> Calendar.getInstance().apply {
                set(state.currentYear, state.currentMonth - 1, 1)
            }
            TimeGranularity.YEAR -> Calendar.getInstance().apply {
                set(state.currentYear, Calendar.JANUARY, 1)
            }
        }

        when (state.granularity) {
            TimeGranularity.DAY -> cal.add(Calendar.DAY_OF_MONTH, direction)
            TimeGranularity.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, direction)
            TimeGranularity.MONTH -> cal.add(Calendar.MONTH, direction)
            TimeGranularity.YEAR -> cal.add(Calendar.YEAR, direction)
        }

        _uiState.update {
            it.copy(
                currentYear = cal.get(Calendar.YEAR),
                currentMonth = cal.get(Calendar.MONTH) + 1,
                currentDay = cal.get(Calendar.DAY_OF_MONTH),
                currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR),
                weekDateRange = computeWeekDateRange(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)),
                expandedGroups = emptySet()
            )
        }
        currentOffset = 0
        loadData()
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply { set(year, month - 1, dayOfMonth) }
        _uiState.update {
            it.copy(
                currentYear = year,
                currentMonth = month,
                currentDay = dayOfMonth,
                currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR),
                weekDateRange = computeWeekDateRange(year, cal.get(Calendar.WEEK_OF_YEAR)),
                showDatePicker = false
            )
        }
        currentOffset = 0
        loadData()
    }

    fun onYearMonthSelected(year: Int, month: Int) {
        _uiState.update {
            it.copy(
                currentYear = year,
                currentMonth = month,
                showDatePicker = false,
                expandedGroups = emptySet()
            )
        }
        currentOffset = 0
        loadData()
    }

    fun onYearSelected(year: Int) {
        _uiState.update {
            it.copy(
                currentYear = year,
                showDatePicker = false,
                expandedGroups = emptySet()
            )
        }
        currentOffset = 0
        loadData()
    }

    fun toggleDatePicker() {
        _uiState.update { it.copy(showDatePicker = !it.showDatePicker) }
        if (!_uiState.value.showDatePicker) return
        // 打开选择器时异步加载可用年份
        viewModelScope.launch {
            try {
                val years = transactionRepository.getAvailableYears()
                _uiState.update { it.copy(availableYears = years) }
            } catch (_: Exception) {
                // 静默失败，选择器会 fallback 到空列表
            }
        }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    // ========== 分页 ==========

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoading) return
        if (state.granularity != TimeGranularity.DAY || state.groupMode != GroupMode.TIME) return
        loadData(append = true)
    }

    // ========== 删除 ==========

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction.id)
                currentOffset = 0
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "删除失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ========== 内部方法 ==========

    private fun loadData(append: Boolean = false) {
        val state = _uiState.value
        // 取消前一个加载任务（旧 Job 的 CancellationException 不对外暴露）
        loadJob?.let { job ->
            job.invokeOnCompletion { /* 静默处理取消，避免系统 Toast */ }
            job.cancel()
        }
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!append) currentOffset = 0

            try {
                val (rangeStart, rangeEnd) = calculateTimeRange(state)

                when {
                    state.isSearching -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    state.groupMode == GroupMode.CATEGORY_L1 ||
                    state.groupMode == GroupMode.CATEGORY_L2 -> {
                        loadCategoryView(state, rangeStart, rangeEnd)
                    }
                    else -> when (state.granularity) {
                        TimeGranularity.YEAR -> loadYearView(state, rangeStart, rangeEnd)
                        TimeGranularity.MONTH -> loadMonthView(state, rangeStart, rangeEnd)
                        TimeGranularity.WEEK -> loadWeekView(state, rangeStart, rangeEnd)
                        TimeGranularity.DAY -> loadDayView(state, rangeStart, rangeEnd, append)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消是正常行为（如用户快速切换筛选条件），不视为错误
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "加载失败")
                }
            }
        }
    }

    // ========== 按分类分组加载 ==========

    private suspend fun loadCategoryView(
        state: TransactionListUiState,
        rangeStart: Long,
        rangeEnd: Long
    ) {
        val allTransactions = transactionRepository.getTransactionsByRange(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            categoryFilter = null,
            limit = maxLoadSize,
            offset = 0
        ).first()

        if (allTransactions.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, timeGroups = emptyList(), hasMore = false)
            }
            return
        }

        val groups = if (state.groupMode == GroupMode.CATEGORY_L2) {
            // 按二级分类分组
            allTransactions
                .groupBy { tx ->
                    val l1 = tx.categoryL1
                    val l2 = tx.categoryL2 ?: "其他"
                    "$l1 · $l2"
                }
                .map { (label, list) ->
                    val sorted = list.sortedByDescending { it.date }
                    TimeGroup(
                        label = label,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        totalExpense = sorted.filter { it.flowType == "支出" }.sumOf { it.amount },
                        totalIncome = sorted.filter { it.flowType != "支出" }.sumOf { it.amount },
                        transactions = sorted
                    )
                }
                .sortedByDescending { it.totalExpense }
        } else {
            // 按一级分类分组
            allTransactions
                .groupBy { it.categoryL1 }
                .map { (catName, list) ->
                    val sorted = list.sortedByDescending { it.date }
                    TimeGroup(
                        label = catName,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        totalExpense = sorted.filter { it.flowType == "支出" }.sumOf { it.amount },
                        totalIncome = sorted.filter { it.flowType != "支出" }.sumOf { it.amount },
                        transactions = sorted
                    )
                }
                .sortedByDescending { it.totalExpense }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                timeGroups = groups,
                hasMore = false
            )
        }
    }

    // ========== 按时间分组加载 ==========

    private suspend fun loadYearView(
        state: TransactionListUiState,
        rangeStart: Long,
        rangeEnd: Long
    ) {
        val allTransactions = transactionRepository.getTransactionsByRange(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            categoryFilter = null,
            limit = maxLoadSize,
            offset = 0
        ).first()

        if (allTransactions.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, timeGroups = emptyList(), hasMore = false)
            }
            return
        }

        val groups = allTransactions
            .groupBy { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                cal.get(Calendar.MONTH) + 1
            }
            .map { (month, list) ->
                val sortedList = list.sortedByDescending { it.date }
                val cal = Calendar.getInstance().apply {
                    set(state.currentYear, month - 1, 1)
                }
                val endCal = cal.clone() as Calendar
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))

                TimeGroup(
                    label = "${month}月",
                    rangeStart = getDayStart(cal),
                    rangeEnd = getDayEnd(endCal),
                    totalExpense = list.filter { it.flowType == "支出" }.sumOf { it.amount },
                    totalIncome = list.filter { it.flowType != "支出" }.sumOf { it.amount },
                    transactions = sortedList
                )
            }
            .sortedByDescending { it.rangeStart }

        _uiState.update {
            it.copy(isLoading = false, timeGroups = groups, hasMore = false)
        }
    }

    private suspend fun loadMonthView(
        state: TransactionListUiState,
        rangeStart: Long,
        rangeEnd: Long
    ) {
        val allTransactions = transactionRepository.getTransactionsByRange(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            categoryFilter = null,
            limit = maxLoadSize,
            offset = 0
        ).first()

        if (allTransactions.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, timeGroups = emptyList(), hasMore = false)
            }
            return
        }

        val groups = allTransactions
            .groupBy { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                getDayStart(cal)
            }
            .map { (weekStartMs, list) ->
                val sortedList = list.sortedByDescending { it.date }
                val startCal = Calendar.getInstance().apply { timeInMillis = weekStartMs }
                val endCal = startCal.clone() as Calendar
                endCal.add(Calendar.DAY_OF_MONTH, 6)

                val weekNum = startCal.get(Calendar.WEEK_OF_YEAR)
                val label = "第${weekNum}周 " +
                        "${startCal.get(Calendar.MONTH) + 1}.${startCal.get(Calendar.DAY_OF_MONTH)}-" +
                        "${endCal.get(Calendar.MONTH) + 1}.${endCal.get(Calendar.DAY_OF_MONTH)}"

                TimeGroup(
                    label = label,
                    rangeStart = weekStartMs,
                    rangeEnd = getDayEnd(endCal),
                    totalExpense = list.filter { it.flowType == "支出" }.sumOf { it.amount },
                    totalIncome = list.filter { it.flowType != "支出" }.sumOf { it.amount },
                    transactions = sortedList
                )
            }
            .sortedByDescending { it.rangeStart }

        _uiState.update {
            it.copy(isLoading = false, timeGroups = groups, hasMore = false)
        }
    }

    private suspend fun loadWeekView(
        state: TransactionListUiState,
        rangeStart: Long,
        rangeEnd: Long
    ) {
        val allTransactions = transactionRepository.getTransactionsByRange(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            categoryFilter = null,
            limit = maxLoadSize,
            offset = 0
        ).first()

        if (allTransactions.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, timeGroups = emptyList(), hasMore = false)
            }
            return
        }

        val dayOfWeekNames = listOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")

        val groups = allTransactions
            .groupBy { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                getDayStart(cal)
            }
            .map { (dayStartMs, list) ->
                val sortedList = list.sortedByDescending { it.date }
                val cal = Calendar.getInstance().apply { timeInMillis = dayStartMs }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val label = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日 ${dayOfWeekNames[dow]}"

                TimeGroup(
                    label = label,
                    rangeStart = dayStartMs,
                    rangeEnd = getDayEnd(cal),
                    totalExpense = list.filter { it.flowType == "支出" }.sumOf { it.amount },
                    totalIncome = list.filter { it.flowType != "支出" }.sumOf { it.amount },
                    transactions = sortedList
                )
            }
            .sortedByDescending { it.rangeStart }

        _uiState.update {
            it.copy(isLoading = false, timeGroups = groups, hasMore = false)
        }
    }

    private suspend fun loadDayView(
        state: TransactionListUiState,
        rangeStart: Long,
        rangeEnd: Long,
        append: Boolean
    ) {
        val flow = transactionRepository.getTransactionsByRange(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            categoryFilter = null,
            limit = pageSize,
            offset = currentOffset
        )

        flow.catch { e ->
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载失败") }
        }.collect { transactions ->
            val groups = groupByDay(transactions)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    timeGroups = if (append) it.timeGroups + groups else groups,
                    hasMore = transactions.size == pageSize
                )
            }
            currentOffset += transactions.size
        }
    }

    // ========== 搜索 ==========

    private fun searchData(keyword: String) {
        loadJob?.let { job ->
            job.invokeOnCompletion { /* 静默处理取消 */ }
            job.cancel()
        }
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                transactionRepository.searchTransactions(
                    keyword = keyword,
                    limit = pageSize,
                    offset = currentOffset
                ).catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "搜索失败") }
                }.collect { transactions ->
                    val groups = groupByDay(transactions)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSearching = true,
                            timeGroups = groups,
                            hasMore = transactions.size == pageSize
                        )
                    }
                    currentOffset += transactions.size
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            }
        }
    }

    private fun groupByDay(transactions: List<Transaction>): List<TimeGroup> {
        return transactions
            .groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                Triple(y, m, d)
            }
            .map { (key, list) ->
                val cal = Calendar.getInstance().apply {
                    set(key.first, key.second - 1, key.third)
                }
                TimeGroup(
                    label = "",
                    rangeStart = getDayStart(cal),
                    rangeEnd = getDayEnd(cal),
                    totalExpense = list.filter { it.flowType == "支出" }.sumOf { it.amount },
                    totalIncome = list.filter { it.flowType != "支出" }.sumOf { it.amount },
                    transactions = list.sortedByDescending { it.date }
                )
            }
            .sortedByDescending { it.rangeStart }
    }

    // ========== 时间范围计算 ==========

    private fun calculateTimeRange(state: TransactionListUiState): Pair<Long, Long> {
        return when (state.granularity) {
            TimeGranularity.DAY -> {
                val cal = Calendar.getInstance().apply {
                    set(state.currentYear, state.currentMonth - 1, state.currentDay)
                }
                getDayStart(cal) to getDayEnd(cal)
            }
            TimeGranularity.WEEK -> {
                val cal = getWeekStartCalendar(state.currentYear, state.currentWeekOfYear)
                val start = getDayStart(cal)
                val endCal = cal.clone() as Calendar
                endCal.add(Calendar.DAY_OF_MONTH, 6)
                start to getDayEnd(endCal)
            }
            TimeGranularity.MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(state.currentYear, state.currentMonth - 1, 1)
                }
                val start = getDayStart(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                start to getDayEnd(cal)
            }
            TimeGranularity.YEAR -> {
                val cal = Calendar.getInstance().apply {
                    set(state.currentYear, Calendar.JANUARY, 1)
                }
                val start = getDayStart(cal)
                cal.set(Calendar.YEAR, state.currentYear)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                start to getDayEnd(cal)
            }
        }
    }

    private fun getWeekStartCalendar(year: Int, weekOfYear: Int): Calendar {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, weekOfYear)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
    }

    private fun computeWeekDateRange(year: Int, weekOfYear: Int): String {
        val cal = getWeekStartCalendar(year, weekOfYear)
        val startMonth = cal.get(Calendar.MONTH) + 1
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val endCal = cal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 6)
        val endMonth = endCal.get(Calendar.MONTH) + 1
        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        return "$startMonth.$startDay - $endMonth.$endDay"
    }

    private fun getDayStart(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getDayEnd(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
