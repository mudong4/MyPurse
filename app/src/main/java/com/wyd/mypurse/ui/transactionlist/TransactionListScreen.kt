package com.wyd.mypurse.ui.transactionlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.ui.components.ChineseDatePickerDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    timeGranularity: String,
    categoryFilter: String?,
    timeRangeStart: Long?,
    timeRangeEnd: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showDeleteDialog by remember { mutableStateOf<Transaction?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(timeGranularity, categoryFilter, timeRangeStart, timeRangeEnd) {
        viewModel.initialize(timeGranularity, categoryFilter, timeRangeStart, timeRangeEnd)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && uiState.hasMore && !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                viewModel.setSearchKeyword(it)
                            },
                            placeholder = { Text("搜索备注、分类、金额") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text("流水记录")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (showSearchBar) {
                        IconButton(onClick = {
                            showSearchBar = false
                            searchText = ""
                            viewModel.setSearchKeyword("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                        }
                    } else {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "搜索")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
            bottomBar = {
            if (!showSearchBar) {
                BottomToolbar(
                    granularity = uiState.granularity,
                    groupMode = uiState.groupMode,
                    timeLabel = buildTimeLabel(
                        granularity = uiState.granularity,
                        year = uiState.currentYear,
                        month = uiState.currentMonth,
                        day = uiState.currentDay,
                        weekDateRange = uiState.weekDateRange
                    ),
                    onGranularitySelect = { viewModel.setGranularity(it) },
                    onGroupModeSelect = { viewModel.setGroupMode(it) },
                    onNavigateTime = { direction -> viewModel.navigateTime(direction) },
                    onTimeLabelClick = { viewModel.toggleDatePicker() }
                )
            }
        }
    ) { innerPadding ->
        // 列表内容
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading && uiState.timeGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.timeGroups.isEmpty() -> {
                    EmptyState(
                        message = when {
                            uiState.isSearching -> "未找到匹配记录"
                            uiState.groupMode != GroupMode.TIME -> "该分类暂无流水记录"
                            uiState.granularity == TimeGranularity.YEAR -> "${uiState.currentYear}年暂无流水记录"
                            uiState.granularity == TimeGranularity.MONTH -> "${uiState.currentMonth}月暂无流水记录"
                            uiState.granularity == TimeGranularity.WEEK -> "本周暂无流水记录"
                            else -> "暂无流水记录"
                        }
                    )
                }
                else -> {
                    GroupedTransactionList(
                        groups = uiState.timeGroups,
                        groupMode = uiState.groupMode,
                        granularity = uiState.granularity,
                        expandedGroups = uiState.expandedGroups,
                        onToggleGroup = { key -> viewModel.toggleGroupExpanded(key) },
                        onTransactionClick = { onNavigateToEdit(it.id) },
                        onTransactionDelete = { showDeleteDialog = it },
                        listState = listState,
                        isLoadingMore = uiState.isLoading
                    )
                }
            }
        }
    }

    // 日历选择器弹窗
    if (uiState.showDatePicker) {
        when (uiState.granularity) {
            TimeGranularity.DAY, TimeGranularity.WEEK -> {
                ChineseDatePickerDialog(
                    initialYear = uiState.currentYear,
                    initialMonth = uiState.currentMonth,
                    initialDay = uiState.currentDay,
                    yearList = uiState.availableYears,
                    todayYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    todayMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                    todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH),
                    onSelected = { year, month, day ->
                        viewModel.onDateSelected(year, month, day)
                    },
                    onDismiss = { viewModel.dismissDatePicker() }
                )
            }
            TimeGranularity.MONTH -> {
                YearMonthPickerDialog(
                    currentYear = uiState.currentYear,
                    currentMonth = uiState.currentMonth,
                    availableYears = uiState.availableYears,
                    onSelected = { year, month ->
                        viewModel.onYearMonthSelected(year, month)
                    },
                    onDismiss = { viewModel.dismissDatePicker() }
                )
            }
            TimeGranularity.YEAR -> {
                YearPickerDialog(
                    currentYear = uiState.currentYear,
                    availableYears = uiState.availableYears,
                    onYearSelected = { viewModel.onYearSelected(it) },
                    onDismiss = { viewModel.dismissDatePicker() }
                )
            }
        }
    }

    // 删除确认弹窗
    showDeleteDialog?.let { transaction ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = {
                val isRecurring = transaction.recurringTemplateId != null
                Text(
                    if (isRecurring)
                        "该记录由固定模板自动生成，删除后不会影响模板设置。\n\n确定删除？"
                    else
                        "确定删除这条记录？删除后无法恢复。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ========== 底部工具栏 ==========

/**
 * 底部工具栏：粒度 | 分类级别 | < 时间 >
 * 分类级别和时间导航在右侧
 */
@Composable
private fun BottomToolbar(
    granularity: TimeGranularity,
    groupMode: GroupMode,
    timeLabel: String,
    onGranularitySelect: (TimeGranularity) -> Unit,
    onGroupModeSelect: (GroupMode) -> Unit,
    onNavigateTime: (Int) -> Unit,
    onTimeLabelClick: () -> Unit
) {
    var showGranularityMenu by remember { mutableStateOf(false) }
    var showGroupModeMenu by remember { mutableStateOf(false) }

    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 粒度下拉
            Box(modifier = Modifier.weight(0.5f)) {
                Row(
                    modifier = Modifier
                        .clickable { showGranularityMenu = true }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = granularity.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showGranularityMenu,
                    onDismissRequest = { showGranularityMenu = false }
                ) {
                    TimeGranularity.entries.forEach { g ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    g.label,
                                    fontWeight = if (g == granularity) FontWeight.Bold else FontWeight.Normal,
                                    color = if (g == granularity) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onGranularitySelect(g)
                                showGranularityMenu = false
                            }
                        )
                    }
                }
            }

            // 2. 分类级别下拉
            Box(modifier = Modifier.weight(0.7f)) {
                Row(
                    modifier = Modifier
                        .clickable { showGroupModeMenu = true }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = groupMode.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (groupMode != GroupMode.TIME) FontWeight.Bold else FontWeight.Normal,
                        color = if (groupMode != GroupMode.TIME) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showGroupModeMenu,
                    onDismissRequest = { showGroupModeMenu = false }
                ) {
                    GroupMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    mode.label,
                                    fontWeight = if (mode == groupMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == groupMode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onGroupModeSelect(mode)
                                showGroupModeMenu = false
                            }
                        )
                    }
                }
            }

            // 3. 时间导航：< 时间文本 >
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onNavigateTime(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上一个",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable(onClick = onTimeLabelClick)
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                )

                IconButton(
                    onClick = { onNavigateTime(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下一个",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ========== 统一的分组折叠列表 ==========

@Composable
private fun GroupedTransactionList(
    groups: List<TimeGroup>,
    groupMode: GroupMode,
    granularity: TimeGranularity,
    expandedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onTransactionDelete: (Transaction) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoadingMore: Boolean
) {
    // 分类模式或年/月/周视图：可折叠；日视图 TIME 模式：直接平铺
    val isFoldable = groupMode != GroupMode.TIME || granularity != TimeGranularity.DAY

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        groups.forEach { group ->
            if (isFoldable) {
                // 时间模式下用 rangeStart 作为 key；分类模式下用 label（因为分类模式 rangeStart 相同）
                val groupKey = if (groupMode != GroupMode.TIME) group.label else group.rangeStart.toString()
                val isExpanded = expandedGroups.contains(groupKey)

                item(key = "header_$groupKey") {
                    CollapsibleGroupHeader(
                        group = group,
                        isExpanded = isExpanded,
                        onClick = { onToggleGroup(groupKey) }
                    )
                }

                item(key = "content_$groupKey") {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            group.transactions.forEach { transaction ->
                                SwipeableTransactionItem(
                                    transaction = transaction,
                                    onDelete = { onTransactionDelete(transaction) },
                                    onClick = { onTransactionClick(transaction) }
                                )
                            }
                            if (group.transactions.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "暂无记录",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 日视图 TIME 模式：直接平铺
                items(
                    items = group.transactions,
                    key = { it.id }
                ) { transaction ->
                    SwipeableTransactionItem(
                        transaction = transaction,
                        onDelete = { onTransactionDelete(transaction) },
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ========== 可折叠的分组 Header ==========

@Composable
private fun CollapsibleGroupHeader(
    group: TimeGroup,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val expenseStr = formatMoney(group.totalExpense)
    val incomeStr = formatMoney(group.totalIncome)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = group.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${group.transactions.size}笔",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            if (group.totalExpense > BigDecimal.ZERO) {
                Text(
                    text = "支出 $expenseStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935)
                )
            }
            if (group.totalIncome > BigDecimal.ZERO) {
                Text(
                    text = "收入 $incomeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF43A047)
                )
            }
        }
    }
}

// ========== 交易条目 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("删除", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        TransactionItem(transaction = transaction, onClick = onClick)
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd日 HH:mm", Locale.CHINA) }
    val isExpense = transaction.flowType == "支出"
    val amountColor = if (isExpense) Color(0xFFE53935) else Color(0xFF43A047)
    val amountPrefix = if (isExpense) "-" else "+"
    val categoryText = buildString {
        append(transaction.categoryL1)
        if (!transaction.categoryL2.isNullOrBlank()) {
            append(" · ")
            append(transaction.categoryL2)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isExpense) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.categoryL1.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isExpense) Color(0xFFE53935) else Color(0xFF43A047),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.recurringTemplateId != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("\uD83D\uDD01", fontSize = 12.sp)
                    }
                }
                Row {
                    Text(
                        text = dateFormat.format(Date(transaction.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!transaction.note.isNullOrBlank()) {
                        Text(
                            text = "  ${transaction.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "$amountPrefix${formatMoney(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ========== 选择器弹窗 ==========

@Composable
private fun YearMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    availableYears: List<Int>,
    onSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var showYearList by remember { mutableStateOf(false) }

    // 可用年份列表：数据库实际年份 + 当前年（确保当前年始终可选）
    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending()
    }

    if (showYearList) {
        // 步骤1：选年份
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedYear = year
                                    showYearList = false
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearList = false }) {
                    Text("取消")
                }
            }
        )
    } else {
        // 步骤2：选月份（点击年份可切换）
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${selectedYear}年",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showYearList = true }
                    )
                }
            },
            text = {
                Column {
                    val months = listOf("1月", "2月", "3月", "4月", "5月", "6月",
                                        "7月", "8月", "9月", "10月", "11月", "12月")
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..3) {
                                val monthIdx = row * 4 + col
                                val month = monthIdx + 1
                                val isSelected = selectedYear == currentYear && month == currentMonth
                                Button(
                                    onClick = { onSelected(selectedYear, month) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        months[monthIdx],
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 可用年份：数据库实际年份 + 当前年（确保当前年始终可选）
    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending()
    }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(years) { year ->
                        val isSelected = year == currentYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onYearSelected(year) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\uD83D\uDCCB",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ========== 工具函数 ==========

private fun buildTimeLabel(
    granularity: TimeGranularity,
    year: Int,
    month: Int,
    day: Int,
    weekDateRange: String
): String = when (granularity) {
    TimeGranularity.DAY -> "${year}年${month}月${day}日"
    TimeGranularity.WEEK -> weekDateRange.ifBlank { "${year}年" }
    TimeGranularity.MONTH -> "${year}年${month}月"
    TimeGranularity.YEAR -> "${year}年"
}

private fun formatMoney(amount: BigDecimal): String {
    val nf = NumberFormat.getNumberInstance(Locale.CHINA)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return nf.format(amount)
}
