package com.wyd.mypurse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Calendar

/** 日历选择器弹窗的背景色（底部 Sheet 默认背景） */
private val SheetBackground = com.wyd.mypurse.ui.theme.AppSheetBg

/**
 * 中文日历选择器（底部 Sheet 样式）。
 * 从底部弹出，自己绘制日历网格，月份显示为数字（如"6月"），不存在英文月份问题。
 *
 * 点击顶部的"2026年6月"文字可展开双列滚轮选择器（年份 | 月份），
 * 再点击文字收起。
 *
 * 两种典型用法：
 * - **记一笔**：不限年份，不标"今天"。传 `todayYear = null`，`yearList = null`。
 * - **流水列表**：年份受数据范围约束，标出真实今天。传 `todayYear` 和 `yearList`。
 *
 * @param initialYear   初始选中的年份
 * @param initialMonth  初始选中的月份，1-12
 * @param initialDay    初始选中的日期
 * @param yearList      年份下拉列表。为 null 时自动生成当前选中年份 ±30 年。
 * @param todayYear     真实今天的年份，用于高亮标记。为 null 时不标今天。
 * @param todayMonth    真实今天的月份
 * @param todayDay      真实今天的日期
 * @param onSelected    用户点击"确定"时回调 (year, month, day)
 * @param onDismiss     取消/关闭时回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChineseDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    yearList: List<Int>? = null,
    todayYear: Int? = null,
    todayMonth: Int? = null,
    todayDay: Int? = null,
    onSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 计算当月天数和第一天是星期几（1=周日 ... 7=周六）
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        Pair(cal.get(Calendar.DAY_OF_WEEK), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    // 年份列表。如果外部未提供或为空，则自动生成当前选中年份 ±30 年的范围
    val resolvedYearList = remember(yearList, selectedYear) {
        yearList?.takeIf { it.isNotEmpty() }?.sortedDescending()
            ?: ((selectedYear - 30)..(selectedYear + 30)).toList().sortedDescending()
    }

    // 月份列表
    val monthList = remember { (1..12).toList() }

    // 调整日期不超出当月天数
    fun adjustDay(year: Int, month: Int) {
        val maxDay = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    // ========== 独立的年月滚轮选择器 BottomSheet ==========
    if (showYearMonthPicker) {
        YearMonthWheelSheet(
            yearList = resolvedYearList,
            monthList = monthList,
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onConfirm = { y, m ->
                selectedYear = y
                selectedMonth = m
                adjustDay(selectedYear, selectedMonth)
                showYearMonthPicker = false
            },
            onDismiss = { showYearMonthPicker = false }
        )
    }

    // ========== 日历 BottomSheet ==========
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 年月导航行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedMonth == 1) {
                        selectedYear--
                        selectedMonth = 12
                    } else {
                        selectedMonth--
                    }
                    adjustDay(selectedYear, selectedMonth)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上月")
                }

                // 年月文字，点击弹出独立的滚轮选择器
                Text(
                    text = "${selectedYear}年${selectedMonth}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showYearMonthPicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                IconButton(onClick = {
                    if (selectedMonth == 12) {
                        selectedYear++
                        selectedMonth = 1
                    } else {
                        selectedMonth++
                    }
                    adjustDay(selectedYear, selectedMonth)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下月")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 星期头：日 一 二 三 四 五 六
            val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 日期网格：6 行 x 7 列
            val (firstDayOfWeek, maxDay) = daysInMonth
            val startOffset = firstDayOfWeek - 1
            val totalCells = startOffset + maxDay

            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = when {
                            cellIndex < startOffset -> null
                            cellIndex >= totalCells -> null
                            else -> cellIndex - startOffset + 1
                        }
                        val isToday =
                            todayYear != null &&
                            dayNumber != null &&
                            dayNumber == todayDay &&
                            selectedMonth == todayMonth &&
                            selectedYear == todayYear
                        val isSelected = dayNumber == selectedDay

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(if (dayNumber != null) Modifier.clickable { selectedDay = dayNumber } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber != null) {
                                Text(
                                    text = "$dayNumber",
                                    color = when {
                                        isSelected && !isToday -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = when {
                                        isSelected || isToday -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                // 如果这行之后所有格子都是空，就不显示后续行了
                if ((row + 1) * 7 > totalCells) break
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 分割线 + 确定按钮
            HorizontalDivider(color = com.wyd.mypurse.ui.theme.AppDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onSelected(selectedYear, selectedMonth, selectedDay) }) {
                    Text("确定", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * 年月双列滚轮选择器（公共组件，BottomSheet）。
 *
 * 使用场景：
 * - ChineseDatePickerDialog 日历内切换年月
 * - 统计页月粒度时间选择
 * - 流水列表月粒度时间选择
 *
 * @param yearList       可选年份列表
 * @param monthList      可选月份列表
 * @param currentYear    初始年份
 * @param currentMonth   初始月份
 * @param title          顶部标题，null 则不显示。动态值可用 lambda：{ "${pickerYear}年${pickerMonth}月" }
 * @param titleColor     标题颜色，默认 primary
 * @param highlightColor 滚轮高亮条颜色，默认 primary 10% 透明
 * @param selectedTextColor 滚轮选中文字颜色，默认 primary
 * @param unselectedTextColor 滚轮未选中文字颜色，默认 onSurfaceVariant
 * @param backgroundColor 滚轮背景色，默认 SheetBackground
 * @param onConfirm      确定回调 (year, month)
 * @param onDismiss      取消/关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearMonthWheelSheet(
    yearList: List<Int>,
    monthList: List<Int>,
    currentYear: Int,
    currentMonth: Int,
    title: (@Composable () -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    selectedTextColor: Color = MaterialTheme.colorScheme.primary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = SheetBackground,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerYear by remember { mutableIntStateOf(currentYear) }
    var pickerMonth by remember { mutableIntStateOf(currentMonth) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 可选的标题行
            if (title != null) {
                title()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 滚轮区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                WheelPicker(
                    items = yearList,
                    selectedIndex = yearList.indexOf(pickerYear)
                        .takeIf { it >= 0 } ?: 0,
                    displayText = { "${it}年" },
                    onSelected = { pickerYear = it },
                    modifier = Modifier.weight(1f),
                    highlightColor = highlightColor,
                    selectedTextColor = selectedTextColor,
                    unselectedTextColor = unselectedTextColor,
                    backgroundColor = backgroundColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                WheelPicker(
                    items = monthList,
                    selectedIndex = monthList.indexOf(pickerMonth)
                        .takeIf { it >= 0 } ?: 0,
                    displayText = { "${it}月" },
                    onSelected = { pickerMonth = it },
                    modifier = Modifier.weight(1f),
                    highlightColor = highlightColor,
                    selectedTextColor = selectedTextColor,
                    unselectedTextColor = unselectedTextColor,
                    backgroundColor = backgroundColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 分割线 + 按钮
            HorizontalDivider(color = com.wyd.mypurse.ui.theme.AppDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onConfirm(pickerYear, pickerMonth) }) {
                    Text("确定", color = confirmColor)
                }
            }
        }
    }
}

/**
 * 年份单列滚轮选择器（公共组件，BottomSheet）。
 *
 * 使用场景：
 * - 统计页年粒度时间选择
 * - 统计页 QuarterSheet 内切换年份
 * - 流水列表年粒度时间选择
 *
 * @param years         可选年份列表
 * @param selectedYear  初始选中年份
 * @param highlightColor 滚轮高亮条颜色
 * @param selectedTextColor 滚轮选中文字颜色
 * @param unselectedTextColor 滚轮未选中文字颜色
 * @param backgroundColor 滚轮背景色
 * @param confirmColor  确定按钮颜色
 * @param onConfirm     确定回调
 * @param onDismiss     取消/关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearWheelSheet(
    years: List<Int>,
    selectedYear: Int,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    selectedTextColor: Color = MaterialTheme.colorScheme.primary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = SheetBackground,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var curYear by remember { mutableIntStateOf(selectedYear) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Text(
                "选择年份",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                textAlign = TextAlign.Center
            )
            HorizontalDivider(color = com.wyd.mypurse.ui.theme.AppDivider)
            WheelPicker(
                items = years,
                selectedIndex = years.indexOf(curYear).coerceAtLeast(0),
                displayText = { "${it}年" },
                onSelected = { curYear = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                highlightColor = highlightColor,
                selectedTextColor = selectedTextColor,
                unselectedTextColor = unselectedTextColor,
                backgroundColor = backgroundColor
            )
            HorizontalDivider(color = com.wyd.mypurse.ui.theme.AppDivider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = { onConfirm(curYear) }) {
                    Text("确定", color = confirmColor)
                }
            }
        }
    }
}

/**
 * 仿滚轮选择器。
 * 使用 LazyColumn 实现：每个 item 高度固定 40dp，总共显示 5 行（可视区域 200dp），
 * 通过监听滚动位置自动吸附到最近的一项，中间项高亮显示为选中。
 *
 * 使用场景：
 * - ChineseDatePickerDialog 内的年月滚轮
 * - 统计页 YearMonthSheet 内的年月滚轮
 *
 * @param items           可选项列表
 * @param selectedIndex   初始选中的索引
 * @param displayText     将 item 转为显示文本
 * @param onSelected      选中回调
 * @param highlightColor  中间高亮条颜色（默认 primary 10% 透明）
 * @param backgroundColor 渐变遮罩背景色（默认 #F5F5F5）
 */
@Composable
internal fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    displayText: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    selectedTextColor: Color = MaterialTheme.colorScheme.primary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = SheetBackground
) {
    val itemHeightDp = 40.dp
    val visibleItems = 5
    val spacerCount = 1 // LazyColumn 中 items 列表之前有 1 个顶部 spacer item（80dp）

    // 使用 selectedIndex 作为 key，确保外部传入的选中索引变化时重建 listState
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (spacerCount + selectedIndex - visibleItems / 2).coerceAtLeast(0)
    )

    // 当外部 selectedIndex 变化时，重新滚动到居中位置
    LaunchedEffect(selectedIndex) {
        val targetGlobalIdx = spacerCount + selectedIndex
        // 让 targetGlobalIdx 对应的 item 居中：firstVisible = targetGlobalIdx - visibleItems/2
        listState.scrollToItem((targetGlobalIdx - visibleItems / 2).coerceAtLeast(0))
    }

    val coroutineScope = rememberCoroutineScope()

    // 如果 items 为空，不渲染滚轮（防御性处理，避免 derivedStateOf 崩溃）
    if (items.isEmpty()) return

    // 使用 derivedStateOf 实时计算当前居中的 item 索引
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                selectedIndex.coerceIn(0, items.lastIndex)
            } else {
                // 找到最接近视口中心的 item
                val viewportCenter = layoutInfo.viewportEndOffset / 2
                val centerItem = visible.minByOrNull {
                    kotlin.math.abs(it.offset + it.size / 2 - viewportCenter)
                }
                // 转换为 items 列表索引
                val itemsIdx = (centerItem?.index ?: spacerCount) - spacerCount
                itemsIdx.coerceIn(0, items.lastIndex)
            }
        }
    }

    // 当 centerIndex 变化时，触发 onSelected 回调
    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices) {
            onSelected(items[centerIndex])
        }
    }

    Box(modifier = modifier) {
        // 中间高亮条（选中项背景）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .background(highlightColor)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * visibleItems),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(itemHeightDp * (visibleItems / 2))) }
            items(items.size) { index ->
                val item = items[index]
                val isCenter = index == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .clickable {
                            onSelected(item)
                            coroutineScope.launch {
                                // 滚动使该 item 居中：全局索引 - visibleItems/2
                                listState.animateScrollToItem(
                                    (index + spacerCount - visibleItems / 2).coerceAtLeast(0)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText(item),
                        fontSize = if (isCenter) 18.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) selectedTextColor else unselectedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(itemHeightDp * (visibleItems / 2))) }
        }

        // 顶部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * (visibleItems / 2))
                .align(Alignment.TopCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0f)
                        )
                    )
                )
        )

        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * (visibleItems / 2))
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0f),
                            backgroundColor
                        )
                    )
                )
        )
    }
}
