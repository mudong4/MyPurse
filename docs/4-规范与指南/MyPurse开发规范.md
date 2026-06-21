# MyPurse Android 项目开发规范

> **文档版本**：V1.15 (2026-06-21)

---

## 1. 命名约定 (V1.0)

### 1.1 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| Entity | 名词 + Entity | `TransactionEntity` |
| DAO | 名词 + Dao | `TransactionDao` |
| Repository 接口 | 名词 + Repository | `TransactionRepository` |
| Repository 实现 | 形容词 + 名词 + RepositoryImpl | `LocalTransactionRepositoryImpl` |
| UseCase | 动词 + 名词 + UseCase | `AddTransactionUseCase` |
| ViewModel | 页面名 + ViewModel | `HomeViewModel` |
| Composable Screen | 页面名 + Screen | `HomeScreen` |
| UI 状态类 | 页面名 + UiState | `HomeUiState` |
| Hilt Module | 模块名 + Module | `DatabaseModule` |

### 1.2 变量命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 布尔值 | 以 is/has/should 开头 | `isLoading`, `hasBudget`, `shouldShowError` |
| 金额 | 以 amount 结尾 | `totalAmount`, `budgetAmount` |
| 列表 | 复数名词 | `transactions`, `categories` |
| 状态类 | 使用 data class，字段用 val | `data class HomeUiState(val balance: BigDecimal)` |
| 自动生成标记 | 通过 `recurringTemplateId != null` 判断 | 不单独新增布尔字段 |

### 1.3 资源文件命名

| 资源类型 | 前缀规则 | 示例 |
|------|------|------|
| 颜色 | 语义命名 | `expense_red`, `income_green`, `budget_blue` |
| 字符串 | 模块_描述 | `home_title`, `add_transaction_save` |
| 图标 | ic_描述_尺寸 | `ic_add_24` |

---

## 2. 架构规范 (V1.0)

### 2.1 分层架构

```
ui/  ← 只依赖 domain/
 │
domain/  ← 不依赖任何 Android 框架
 │
data/  ← 实现 domain 层的接口
```

**严格依赖方向**：`ui → domain ← data`

### 2.2 各层职责

| 层 | 职责 | 允许依赖 |
|----|------|----------|
| **ui/** | Compose UI + ViewModel | domain、Android SDK |
| **domain/** | 业务模型、UseCase、Repository 接口 | 无（纯 Kotlin） |
| **data/** | Entity、DAO、Repository 实现、TypeConverter | domain、Room、SQLite |

### 2.3 公共组件复用原则 (V1.4)

> **背景**：本项目曾因 `ChineseDatePickerDialog` 放在 `ui/addtransaction/` 下被误认为私有组件，导致流水列表编辑页独立实现了另一套日期选择器（Material3 原生英文日历），产生功能不一致和代码重复。这是架构层面的组织缺陷。

#### 2.3.1 组件归属规则

| 组件被使用次数 | 应放位置 | 示例 |
|---|---|---|
| 1 个页面 | 该页面目录下 | `HomeScreen` 内部的 `TrendChart` |
| ≥ 2 个页面 | `ui/components/` | `ChineseDatePickerDialog`、`EmptyStateView` |

**强制**：当某个页面目录下的组件被第二个页面引用时，必须将其**移动**到 `ui/components/` 下，并更新所有 import。

> **关于"三场景原则"**：业界有"第三次出现时才抽象"（Rule of Three）的说法，但本项目采用更严格的标准——**≥2 即抽取**。原因：(1) 项目规模小，过早抽象的成本低；(2) 两个独立实现已经足够产生不一致问题（如时间选择器 #30），等第三个场景出现才动手只是推迟已知问题。

#### 2.3.2 新页面开发前的组件搜索（强制）

开发新页面涉及以下功能时，**必须先执行全局搜索**，确认是否已有对应公共组件：

| 功能 | 搜索关键词 | 已有组件（截至当前） |
|---|---|---|
| 日期选择 | `DatePicker`、`Calendar` | `ChineseDatePickerDialog`（`ui/components/`） |
| 分类选择 | `CategoryPicker`、`CategorySheet` | 记一笔内联（待抽取） |
| 金额输入 | `AmountInput`、`NumberKeyboard` | 记一笔内联（待抽取） |
| 滚轮选择 | `WheelPicker` | `WheelPicker`（`ui/components/`） |
| 空状态 | `EmptyState` | `EmptyStateView`、`EmptyStateText`（`ui/components/`） |

```bash
# 搜索示例：找日期选择器
grep -r "DatePicker\|Calendar\|日期选择" --include="*.kt" app/src/main/java/
```

#### 2.3.3 同功能同组件原则（强制）

> 同一个交互功能在整个 App 中**有且只有一个组件实现**。

- 如需在不同页面表现不同行为，通过**参数**控制（如 `ChineseDatePickerDialog` 的 `todayYear`/`yearList` 参数），而非复制代码。
- **禁止**"复制一份改改"（如统计页的 `WheelPickerForStat` 是 `WheelPicker` 的副本），这种复制是技术债务。
- 如果现有组件无法通过参数满足新需求，应**扩展组件**而非**复制组件**。

#### 2.3.4 组件文档

`ui/components/` 下每个文件头部必须包含：
```kotlin
/**
 * [组件名] — [一句话描述]
 * 
 * 使用场景：
 * - [页面A]：[用途]
 * - [页面B]：[用途]
 * 
 * @param xxx 参数说明
 */
```

同时，在 `docs/1-项目概览/项目介绍.md` 中维护"已有公共组件清单"表格，每次新增公共组件时更新。

#### 2.3.5 长远扩展性设计原则 (V1.13)

> **背景**：本项目曾出现流水列表、记一笔、统计页各自独立实现日历/时间导航组件，导致三处代码重复、风格不一致。以及颜色配置设计时未预留用户自定义扩展点，后续改造困难。这些问题的根因是"设计时只考虑当前需求，未想长远"。

**核心原则**：涉及跨页面共享功能或用户可能想自定义的配置时，设计阶段必须多问一步——"将来会不会扩展？"

**强制检查清单**（设计新功能/组件前必答）：

| 检查项 | 触发条件 | 要求 |
|--------|----------|------|
| **跨页面复用** | 新功能涉及时间选择、分类选择、金额显示、列表项等通用交互 | 必须先全局搜索已有组件，优先扩展而非新建 |
| **用户可自定义** | 涉及颜色、主题、排序、显示格式等外观/行为配置 | 数据必须存储在配置层（DataStore/数据库），而非硬编码在 UI 中 |
| **数据层扩展** | 新增字段、表、查询条件 | 考虑将来是否需要同类但不同维度的查询（如"本月支出排行"→ 将来可能需要"本周""本年"），接口设计预留参数 |
| **同类功能一致性** | 多个页面有相似展示（如统计页和首页都有支出排行） | 必须共用同一数据源和同一颜色映射逻辑，禁止各自实现 |

**反例（已有教训）**：

| 问题 | 当时做法 | 应做做法 |
|------|----------|----------|
| 三处日历组件各自实现 | 流水列表用 ChineseDatePickerDialog，记一笔用另一套，统计用点击式年月选择 | 统一使用一个可配置的时间导航组件 |
| 分类颜色硬编码 | 统计页和首页各自定义颜色映射 | 颜色配置中心统一管理，预留用户自定义 |
| 支出排行颜色分散 | 首页排行和统计构成分析各自写颜色逻辑 | 共用 `CategoryColorConfig`（或类似统一数据源） |

**执行机制**：
- Agent 在开发新功能前，必须对照本清单自检（纳入 13.2.1 必读清单）
- 人类在审查时，对涉及颜色、分类、时间等功能的改动，需额外关注扩展性（纳入 13.2.2 审查要点）

#### 2.3.6 全局影响分析制度（V1.14 新增）

> **背景**：V1.0 开发中出现了"局部改动引发多处不一致"的典型问题——颜色在 5 个页面各自为政、时间选择器有 3 套独立实现、分类展示逻辑分散。这些问题在编码时未被发现，因为当时只改了"一个页面"。本节将"先分析再动手"固化为强制流程。

**触发条件**（满足任一即触发）：

| 触发条件 | 典型场景 |
|---|---|
| 涉及颜色值、色板、颜色映射 | 新增/修改分类颜色、图表配色、主题色 |
| 涉及时间/日期选择器 | 统计页、流水列表、记一笔等 |
| 涉及分类数据展示（图标、颜色、名称格式） | 首页排行、统计构成、分类选择器等 |
| 涉及多个页面共用的 UI 组件 | 空状态、确认对话框、加载状态等 |

**执行流程（4 步）**：

```
1. 列出所有使用该元素的页面/组件
2. 分析当前各自如何实现（是否重复、是否一致）
3. 确定统一方案（抽取公共组件 or 统一数据源 or 统一色板变量）
4. 方案确定后再开始编码
```

**输出要求**：分析结果必须记录在需求文档的"全局影响分析基线"章节（或会话的开发日志中），包含：
- 各页面当前实现方式对比表
- 统一方案说明
- 开发顺序建议

**反例（已有教训）**：

| 问题 | 当时做法 | 应做做法 |
|------|----------|----------|
| 三处日历组件各自实现 | 流水列表用 ChineseDatePickerDialog，记一笔用另一套，统计用点击式年月选择 | 统一使用一个可配置的时间导航组件 |
| 分类颜色硬编码 | 统计页和首页各自定义颜色映射 | 颜色配置中心统一管理，预留用户自定义 |
| 支出排行颜色分散 | 首页排行和统计构成分析各自写颜色逻辑 | 共用 `CategoryColorConfig`（或类似统一数据源） |
| `TransactionDetailScreen` 5 处颜色硬编码 | 直接写 `Color(0xFFE53935)` 等 | 使用 `AppColors.kt` 中已有变量 |

**V1.0 全局影响分析基线参考**：详见 `docs/2-需求规格/requirements-v1x.md` 附录 A（颜色体系、时间选择器、分类展示三个维度的现状分析和统一方向）。后续版本迭代时，应在本规范基础上生成对应版本的分析基线。

### 2.4 文件组织 (V1.0)

```
com.wyd.mypurse/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAO 接口
│   │   ├── entity/       # Room 实体类
│   │   └── database/     # RoomDatabase 定义
│   ├── repository/       # Repository 接口的本地实现
│   └── sync/             # SyncManager 接口及空实现（V1.0 预留）
├── domain/
│   ├── model/            # 纯 Kotlin 业务模型
│   ├── usecase/          # 每个用例一个类
│   └── repository/       # Repository 接口定义
├── ui/
│   ├── home/             # 首页（Screen + ViewModel + UiState）
│   ├── addtransaction/   # 记一笔
│   ├── statistics/       # 统计页
│   ├── transactionlist/  # 流水列表
│   ├── categorymanage/   # 分类管理
│   ├── budget/           # 预算设置
│   ├── recurring/        # 固定收支模板
│   ├── settings/         # 设置页
│   └── components/       # 可复用 Compose 组件
├── di/                   # Hilt 模块
└── MyPurseApplication.kt
```

---

## 3. 代码风格 (V1.0)

### 3.1 Kotlin 风格

- **禁止使用 `!!` 非空断言**。用 `?.`、`?:`、`let` 安全处理。
- **函数体不超过 30 行**。超过则拆分为私有辅助函数。
- **类不超过 300 行**。超过则考虑拆分。
- **避免使用 `var`**，优先使用 `val`。可变状态用 `MutableStateFlow` 或 `mutableStateOf`。
- **使用 `when` 代替多层 `if-else`**。
- **字符串拼接使用模板**：`"已花 ¥$spent"` 而不是 `"已花 ¥" + spent`。

### 3.2 Compose 风格

- **Composable 函数参数顺序**：`modifier`（可选）→ 数据状态 → 事件回调 → 其他。
- **Composable 函数不超过 80 行**。超过则拆分为子组件。
- **不在 Composable 中写业务逻辑**。业务逻辑在 ViewModel 中处理。
- **使用 `remember` 和 `derivedStateOf`** 优化重组。
- **列表使用 `LazyColumn`**，不用 `Column` + `forEach`。
- **状态收集使用 `collectAsStateWithLifecycle()`**。

### 3.3 注释规范

- **每个公开类必须有注释**，说明其职责。中文即可。
- **每个公开函数必须有注释**，说明参数和返回值。
- **注释说"为什么"，不是说"是什么"**。代码本身说明"是什么"。
- **复杂逻辑必须有行内注释**。

```kotlin
/**
 * 计算当前结余（全部收入 - 全部支出）。
 * 计算方式：总收入 SUM - 总支出 SUM，使用 BigDecimal 精确计算。
 */
class GetBalanceUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<BigDecimal> { ... }
}
```

---

## 4. 依赖注入规范（Hilt）(V1.0)

- **所有 ViewModel 使用 `@HiltViewModel`**，构造函数使用 `@Inject`。
- **Repository 使用 `@Singleton` 作用域**。
- **Database 使用 `@Singleton` 作用域**。
- **禁止手动创建对象**。所有依赖通过构造函数注入。
- **Hilt Module 统一放在 `di/` 包下**。

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() { ... }
```

---

## 5. 数据层规范 (V1.0)

### 5.1 Room

- **所有 DAO 方法必须是 `suspend` 或返回 `Flow`**。
- **数据库升级必须写 `Migration`**，不得使用 `fallbackToDestructiveMigration()`。
- **Entity 字段使用 lowerCamelCase**，数据库列名通过 `@ColumnInfo(name = "snake_case")` 映射。
- **查询条件参数使用命名参数**。

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
}
```

### 5.2 Room 聚合查询规范（统计专用）(V1.1)

- **统计类查询必须使用 SQL 聚合函数（SUM、GROUP BY），禁止在 Kotlin 层做内存遍历**。
- **统计类 DAO 方法命名需体现"粒度+维度"**，如：
  - `getExpenseByCategoryForMonth(year: Int, month: Int)` — 按月查各分类支出
  - `getExpenseTrendByMonth(startMonth: String, endMonth: String)` — 月度支出趋势
  - `getBudgetExecutionByQuarter(year: Int)` — 季度预算执行率
- **涉及固定收支模板的查询，必须加 `recurringTemplateId` 过滤条件**，避免重复生成记录。

```kotlin
@Dao
interface TransactionDao {
    // ✅ 正确：SQL 聚合，按分类分组
    @Query("""
        SELECT categoryL1, SUM(amount) as total 
        FROM transaction 
        WHERE date BETWEEN :start AND :end AND flowType = '支出' 
        GROUP BY categoryL1 
        ORDER BY total DESC
    """)
    suspend fun getExpenseByCategory(start: Long, end: Long): List<CategoryAmountEntity>

    // ✅ 正确：按时间粒度分组，用于趋势图
    @Query("""
        SELECT strftime('%Y-%m', date/1000, 'unixepoch') as month, 
               SUM(amount) as total 
        FROM transaction 
        WHERE flowType = '支出'
        GROUP BY month 
        ORDER BY month
    """)
    fun getMonthlyExpenseTotal(): Flow<List<MonthlyTotalEntity>>
    
    // ❌ 错误：查询全部数据后在 Kotlin 中用 filter/groupBy 处理
    // fun getAllTransactions(): List<TransactionEntity> // 禁止这样做统计
}
```

### 5.3 金额处理

- **所有金额字段使用 `BigDecimal`**。
- **数据库存储用 `String`**，通过 `TypeConverter` 转换。
- **所有计算用 `BigDecimal` 的方法**（`add()`、`subtract()`、`multiply()`），**禁止使用算术运算符**。

```kotlin
class BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
```

### 5.4 事务

- **涉及多表操作的逻辑必须在一个数据库事务中完成**。
- 使用 `@Transaction` 注解或 `database.runInTransaction()`。

### 5.5 自动记账模板执行规范 (V1.1)

`RecurringTemplateRepository` 实现类中，"检查并执行自动记账"的方法必须包含以下四步：

1. **事务包裹**：所有检查和插入操作在一个事务中完成，避免部分执行失败导致数据不一致。
2. **90 天补记上限校验**：计算应执行日期时，忽略超过 90 天前的遗漏周期。
3. **重复记录校验**：插入前按 `recurringTemplateId + 执行日期` 查询，已存在则跳过。
4. **无效日期处理**：如每月 31 日但当月只有 30 天，自动退到当月最后一天。

自动生成的交易记录：
- `date` 字段为模板指定的应执行日期（不是系统当前日期）
- `createTime` 字段为系统实际执行时间
- `recurringTemplateId` 字段指向来源模板（非 null 即表示自动生成，UI 层据此显示 🔄 标记）

### 5.6 CSV 导入导出规范 (V1.1)

**导出**：
- 导出方法需支持"时间范围筛选"参数（全部/近一年/近一月/自定义）
- 字段顺序固定为：流水类型 → 一级分类 → 二级分类 → 金额 → 备注 → 日期
- 通过系统分享 API 发送文件，不申请存储权限

**导入**：
- 先校验 CSV 首行列名是否匹配
- 逐行校验：金额是否为合法数字、日期是否可解析、必填字段是否为空
- 校验失败的行记录错误原因（行号 + 错误描述）
- 导入完成后返回 `Result`，包含"成功 X 条 / 跳过 X 条 / 错误 X 条"
- 导入时自动创建不存在的分类

---

## 6. UI 层规范 (V1.0)

### 6.1 ViewModel

- **使用 `StateFlow` 暴露 UI 状态**，不使用 `LiveData`。
- **UI 状态定义为不可变 `data class`**。
- **事件处理通过方法调用**，不使用 `EventBus`。
- **ViewModel 中不持有 `Context`、`View` 等 Android 框架引用**。

```kotlin
data class HomeUiState(
    val balance: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
```

### 6.2 状态收集

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 使用 uiState 渲染界面
}
```

### 6.3 连续记账模式规范 (V1.1)

"记一笔"页面的 ViewModel 需维护 `continuousMode` 状态（Boolean，默认 false）。

**保存后的行为**：
1. 调用 UseCase 保存记录到数据库
2. 清空 `amount` 和 `note` 的 StateFlow
3. 保留 `flowType`、`categoryL1`、`categoryL2`、`date` 状态
4. `continuousMode` 设为 true
5. 触发 UI 层将金额输入框重新获得焦点

**退出连续记账**：
- 用户点击"保存并关闭" → `continuousMode` 设为 false → 返回上一页
- "保存并关闭"点击事件必须通过 `SharedFlow` 发送，防止快速双击导致重复保存

```kotlin
// ViewModel 中
private val _saveAndCloseEvent = MutableSharedFlow<Unit>()
val saveAndCloseEvent: SharedFlow<Unit> = _saveAndCloseEvent.asSharedFlow()

fun onSaveAndClose() {
    viewModelScope.launch {
        _saveAndCloseEvent.emit(Unit) // 单次事件，不重复
    }
}
```

### 6.4 列表性能规范 (V1.1)

流水列表使用 `LazyColumn` 展示时，必须满足以下要求：

1. **使用 `itemKey`**：`LazyColumn { items(list, key = { it.id }) { ... } }`，避免条目复用错乱。
2. **预加载相邻月份数据**：通过 `LazyListState` 监听滚动位置，当滚动接近当前月份边界时，加载上/下一个月的数据。
3. **年视图摘要模式**：使用独立的 `LazyColumn` 项布局，不嵌套滑动组件。
4. **筛选/搜索过滤逻辑**：在 ViewModel 中通过 `derivedStateOf` 缓存过滤结果，避免每次 Compose 重组都重新过滤整个列表。

```kotlin
// ✅ 正确：derivedStateOf 缓存过滤结果
val filteredTransactions by remember {
    derivedStateOf {
        if (searchQuery.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter { it.matches(searchQuery) }
        }
    }
}
```

### 6.5 颜色语义

| 含义 | 颜色 | 色值 |
|------|------|------|
| 支出 | 红色 | #E53935 |
| 收入 | 绿色 | #43A047 |
| 预算正常 | 蓝色 | #1E88E5 |
| 预算接近（≥80%） | 橙色 | #FB8C00 |
| 预算超支 | 红色 | #D32F2F |
| 已删除分类 | 灰色 | #9E9E9E |

### 6.6 金额显示

- 支出：显示 `-¥30.00`，红色
- 收入：显示 `+¥5,000.00`，绿色
- 结余正数：默认色
- 结余负数：红色

### 6.7 导航规范 (V1.1)

- **导航库**：使用 `androidx.navigation:navigation-compose`。
- **路由定义**：统一在 `ui/navigation/` 包下使用 `sealed class` 或 `@Serializable` 对象定义路由，禁止使用字符串拼接路由。
- **类型安全传参**：路由参数使用 type-safe 的 Serializable 对象，不使用 `?key={value}` 字符串拼接。
- **ViewModel 不持有 NavController**：导航操作通过回调（lambda）传出，在 Screen 层调用 `navController.navigate()`。
- **底部导航栏**（首页、设置两个 Tab）使用 `NavigationBar` + `NavHost`。统计页已从底部 Tab 移除，改为通过首页 TopAppBar 图标进入。
- **记一笔页（B）** 不使用底部导航栏，通过独立路由进入。

```kotlin
// 路由定义示例
@Serializable
sealed class Route {
    @Serializable
    data object Home : Route()
    
    @Serializable
    data class TransactionList(
        val timeGranularity: String = "month",
        val categoryFilter: String? = null
    ) : Route()
}
```

---

## 7. 错误处理规范

- **使用 `sealed class` 封装结果**：

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val details: List<String> = emptyList()) : Result<Nothing>()
}
```

- **Room 操作异常在 Repository 层捕获**，转换为 `Result` 返回。
- **UI 层根据 `Result` 状态显示对应的 Snackbar 或空状态**。
- **协程异常通过 `CoroutineExceptionHandler` 统一处理**。

---

## 8. 性能规范 (V1.0)

### 8.1 冷启动优化

1. **数据库初始化延迟到首次使用**：通过 `lazy` 或 Hilt 的 `@Singleton` 懒加载，不在 Application 的 `onCreate` 中初始化数据库。
2. **首页统计数据**：V1.0 使用 Room 的 SQL SUM 聚合查询直接计算（几万条数据在几十毫秒内完成）。V1.1 可优化为增量缓存（仅计算新增/修改的记录差异，而非全表扫描）。
3. **图表渲染**：在 `LaunchedEffect` 中异步获取数据，避免阻塞 Compose 首次重组。
4. **自动记账与数据库初始化**：固定收支模板的自动记账逻辑在数据库首次初始化完成后触发（在 Repository 层首次访问时通过协程执行），不在 `Application.onCreate` 中阻塞启动。

### 8.2 安装包大小控制 (V1.1)

1. **图表库**：必须使用 Vico（轻量级 Compose 原生图表库），禁止引入 MPAndroidChart。使用前需验证 Vico 对饼图、双柱对比图、柱+折叠加图的支持情况，如不支持需提前选定备选方案。
2. **R8/ProGuard**：Release 构建必须开启混淆和资源压缩。
3. **图标**：统一使用 Vector Asset（矢量图），禁止使用位图资源（PNG/JPG）。

### 8.3 零权限 (V1.1)

1. `AndroidManifest.xml` 中**禁止声明任何权限**（包括 `INTERNET`、`WRITE_EXTERNAL_STORAGE` 等）。
2. CSV 导出通过系统分享 API（`Intent.ACTION_SEND`）实现，**无需存储权限**。
3. 所有数据存储严格限定在应用内部存储（`context.filesDir`），**禁止写入外部存储**，确保卸载后数据自动清空。

---

## 9. 测试规范

- **数据层必须写单元测试**（测试 DAO 和 Repository）。
- **UseCase 必须写单元测试**（使用 MockK 模拟依赖）。
- **ViewModel 建议写单元测试**（使用 MockK + Turbine 测试 StateFlow）。
- **测试文件放在 `src/test/` 目录下**，包路径与源文件一致。
- **测试类命名**：被测类名 + `Test`（如 `TransactionDaoTest`）。

### 9.1 核心场景测试用例要求 (V1.1)

以下场景必须有测试覆盖：

1. **金额计算测试**：
   - 核心公式："收入 - 支出 = 结余"、"预算剩余 = 预算 - 已花"
   - 边界值：0.01 元、999999.99 元
2. **自动记账测试**：
   - 错过 3 个周期时是否正确补记
   - 同一模板+同一日期的记录不会重复生成
   - 每月 31 日在只有 30 天的月份是否正确退到月末
3. **分类删除测试**：
   - "直接删除（保留记录）"后历史记录是否保留快照名
   - "迁移至其他分类"后历史记录是否批量更新
   - "删除分类及记录"后关联记录是否全部删除
4. **性能测试**：
   - 插入 5 万条交易记录后，列表滑动帧率 ≥ 60fps
   - 统计图表加载 ≤ 1 秒

---

## 10. Git 提交规范

- **提交信息格式**：`类型: 描述`
- **类型**：
  - `feat`：新功能
  - `fix`：修复 Bug
  - `refactor`：重构
  - `docs`：文档变更
  - `style`：代码格式
  - `test`：测试
  - `chore`：构建/配置

**示例**：
```
feat: 实现记一笔页面UI
fix: 修复分类删除后历史记录图标显示错误
refactor: 将金额计算统一使用 BigDecimal
```

- **每次提交只做一件事**，不要混合多个不相关的修改。

---

## 11. 禁止事项清单 (V1.0, 持续更新)

| 禁止 | 原因 |
|------|------|
| ❌ 使用 `!!` | 可能导致空指针崩溃 |
| ❌ 在 Composable 中调用 `viewModel.xxx()` 产生副作用 | 应使用 `LaunchedEffect` |
| ❌ 使用 `Float`/`Double` 存金额 | 浮点精度误差 |
| ❌ `fallbackToDestructiveMigration()` | 用户数据丢失 |
| ❌ 在主线程执行数据库操作 | 阻塞 UI，Room 会报错 |
| ❌ 硬编码字符串/颜色/尺寸 | 应放入资源文件或常量 |
| ❌ ViewModel 持有 Context | 内存泄漏 |
| ❌ 使用 `LiveData` | 项目统一用 StateFlow |
| ❌ 手动 `new` 对象 | 使用依赖注入 |
| ❌ 统计用内存遍历代替 SQL 聚合 | 性能差，5 万条数据时卡顿明显 |
| ❌ 声明任何系统权限 | V1.0 为零权限应用 |
| ❌ 使用 MPAndroidChart | 体积大，改用 Vico |
| ❌ 在页面目录下放被多处引用的组件 | 应移至 `ui/components/` |
| ❌ 复制粘贴已有组件代码 | 应通过参数扩展原组件 |

---

## 12. 代码审查检查清单 (V1.0, 持续更新)

每次收到 AI 交付的代码后，对照以下清单逐条检查：

**基础检查**：
- [ ] 包名是否以 `com.wyd.mypurse` 开头？包结构是否正确？
- [ ] 类命名是否符合规范？
- [ ] 依赖方向是否正确？（ui → domain ← data）
- [ ] ViewModel 中是否有 Android 框架引用？
- [ ] 是否使用了 `!!`？
- [ ] 金额是否使用了 `BigDecimal`？
- [ ] Composable 参数顺序是否正确？
- [ ] 是否有硬编码字符串/颜色？
- [ ] 数据库操作是否在协程中？
- [ ] 是否使用了依赖注入而非手动创建？
- [ ] 删除操作是否有二次确认？
- [ ] 新增的 UI 组件是否被 ≥2 个页面使用？若是，是否已放在 `ui/components/` 下？
- [ ] 是否存在复制已有组件代码的情况？（应扩展原组件）
- [ ] 是否涉及颜色/分类/时间选择器等跨页面共用元素？若是，是否已执行全局影响分析？

**业务逻辑检查**：
- [ ] 统计类查询是否全部通过 SQL 聚合实现，无内存遍历？
- [ ] 金额计算是否全部使用 `BigDecimal` 的 `add/subtract` 方法，无算术运算符？
- [ ] 连续记账模式下，保存后状态重置是否符合需求（清空金额+备注，保留类型+分类+日期）？
- [ ] "保存并关闭"是否使用 SharedFlow 防抖？
- [ ] 自动记账模板执行逻辑是否包含：事务包裹、90 天补记上限、重复校验、无效日期处理？
- [ ] 自动生成的交易记录是否通过 `recurringTemplateId != null` 标记（UI 据此显示 🔄 图标）？
- [ ] 分类删除时，是否提供了"直接删除/迁移至其他分类/删除分类及记录"三个选项？
- [ ] 预算修改是否遵循"按月独立存储，不影响历史"规则？
- [ ] CSV 导出字段顺序是否正确（流水类型→一级分类→二级分类→金额→备注→日期）？
- [ ] CSV 导入是否包含格式校验、错误原因记录、成功/跳过/错误数量反馈？
- [ ] 5 万条记录下，列表是否使用了 `itemKey` 和预加载？滑动是否无卡顿？

---

此规范即日起生效，所有代码提交必须符合规范要求。如需修改，按版本号迭代。

---

## 13. 开发过程管理规范 (V1.6)

> **核心理念**：代码之外的过程记录与代码本身同等重要。没有过程记录的项目，在脱离当前开发上下文后将变得难以维护。

### 13.1 通用规范引用

开发过程管理的通用规范（文档体系、开发日志模板、问题记录模板、需求变更记录规范、临时文件清理、版本号管理规范等）统一由 `4-规范与指南/通用开发规范.md` 第 6 章定义。本节只补充 **MyPurse 项目特有的映射和差异**。

#### 13.1.1 文档角色 → MyPurse 文件名映射

通用开发规范 6.1 节定义了 6 类内容角色，MyPurse 的对应关系如下：

| 内容角色（通用规范定义） | MyPurse 实际文件 | 存放目录 |
|------------------------|-----------------|---------|
| 项目概览 | `项目介绍.md` | `docs/1-项目概览/` |
| 开发日志 | `开发日志.md` | `docs/3-开发过程/` |
| 问题/Bug 记录 | `问题记录.md` | `docs/3-开发过程/` |
| 需求规格 | `requirements.md` | `docs/2-需求规格/` |
| 进度追踪 | `开发阶段.md` | `docs/3-开发过程/` |
| 项目编码规范 | `开发规范.md`（本文档） | `docs/4-规范与指南/` |

#### 13.1.2 MyPurse 项目特有的差异

| 通用规范条款 | MyPurse 差异 | 原因 |
|-------------|-------------|------|
| 6.1.1 需求变更记录章节编号"因项目而异" | MyPurse 需求文档的变更记录在第 10 章 | 本项目需求文档章节结构已固定 |
| 6.1 文档体系列 6 类内容 | MyPurse 文档体系目录下还包含通用规范（`通用开发规范.md`）、阶段规划指南等跨项目复用的文档 | 通用规范不是项目实例文档，但存放在项目 docs/ 下方便 Agent 读取 |

### 13.2 AI Agent 协作规范

本项目可能由不同的 AI agent 在不同对话中开发。为确保上下文连续性：

**给 Agent 的初始提示应包括**：
1. 阅读 `docs/1-项目概览/项目介绍.md` 了解项目全貌
2. 阅读 `docs/3-开发过程/开发日志.md` 了解最新改动
3. 阅读 `docs/3-开发过程/问题记录.md` 了解已知坑点
4. 阅读 `docs/2-需求规格/requirements.md` 了解功能规格
5. 阅读本文档（`开发规范.md`）了解编码规范
6. 阅读 `docs/4-规范与指南/通用开发规范.md` 了解通用开发过程规范

**Agent 开发结束后必须**（遵循通用开发规范第 0 章）：
1. 更新 `docs/3-开发过程/开发日志.md`（记录本次改动）
2. 同步更新 `docs/3-开发过程/开发日志-精简版.md`
3. 如有 Bug 修复 → 更新 `docs/3-开发过程/问题记录.md`
4. 同步更新 `docs/3-开发过程/问题记录-精简版.md`
5. 如阶段完成 → 更新 `docs/3-开发过程/开发阶段.md`
6. 如有需求变更 → 更新 `docs/2-需求规格/requirements.md` 正文 + 第 10 章变更记录（含原文摘要和决策理由）
7. 清理临时文件（编译输出、调试日志等）

#### 13.2.1 Agent 开发前必读章节清单

> **目的**：确保 Agent 在开始开发前了解必须遵守的规范，而非依赖 Agent 自觉查找。

**第一优先级（必须阅读）**：

| 章节 | 说明 | 原因 |
|------|------|------|
| 2.3 公共组件复用原则 | 禁止复制粘贴式复用 | 防止引入技术债务 |
| 2.3.5 长远扩展性设计原则 | 设计前多问"将来会不会扩展" | 防止短视设计导致后期重构 |
| 2.3.6 全局影响分析制度 | 涉及多页面共用元素时先分析再动手 | 防止局部改动引发多处不一致 |
| 11 禁止事项清单 | 明确不能做的事 | 避免踩已知坑点 |
| 14.6 代码安全操作规范 | 文件编辑原则、编译验证 | 防止编译失败 |
| 《通用开发规范》0.3 编译验证 | 每次改动后编译 | 基本保障 |

**第二优先级（按场景阅读）**：

| 场景 | 需阅读章节 |
|------|-----------|
| 新增数据库表/字段 | 数据层规范、Room 规范 |
| 新增 UI 页面 | UI 层规范、导航规范 |
| 新增网络请求 | 网络层规范 |
| 涉及金额计算 | 金额处理规范 |

**Agent 初始提示模板**（可在对话开头使用）：

```
请先阅读以下文档：
1. docs/1-项目概览/项目介绍.md
2. docs/3-开发过程/开发日志-精简版.md
3. docs/3-开发过程/问题记录-精简版.md
4. docs/4-规范与指南/MyPurse开发规范.md（重点关注 2.3、11、14.6 节）
5. docs/4-规范与指南/通用开发规范.md（重点关注 0.3 节）
```

#### 13.2.2 人类审查要点规范

> **目的**：人类在审查 Agent 产出时，需要明确检查什么，而非凭感觉。

**每次审查必查项**：

| 审查项 | 检查方法 | 严重程度 |
|--------|----------|----------|
| 编译通过 | `./gradlew compileDebugKotlin` | 🔴 阻塞 |
| 修订记录已更新 | 检查被修改文档末尾的修订记录表 | 🟠 重要 |
| 开发日志已更新 | 检查开发日志是否有本次会话记录 | 🟠 重要 |
| 精简版已同步 | 检查精简版文档是否同步更新 | 🟡 建议 |
| 扩展性考量 | 涉及颜色/分类/时间/配置等改动时，检查是否预留了用户自定义扩展点 | 🟡 建议 |
| 公共组件未重复实现 | 新增 UI 组件前检查是否已有类似组件可扩展复用 | 🟠 重要 |
| 全局影响分析已执行 | 涉及多页面共用元素时，检查是否已做全局影响分析并记录方案 | 🟠 重要 |
| 临时文件已清理 | 检查是否有多余的日志/编译输出文件 | 🟡 建议 |

**审查流程**：

```
1. 编译验证 → 失败则打回
2. 快速浏览开发日志 → 确认改动范围
3. 检查修订记录 → 确认文档变更可追溯
4. 代码 diff 审查 → 关注架构、安全、性能
5. 功能验证（如已安装 APK）→ 核心流程走通
```

**Agent 应主动询问的问题**：
- "这个设计是否有扩展性考虑？" — 当涉及颜色、分类等用户可能想自定义的配置时
- "这个组件是否已有公共实现？" — 当新增 UI 组件时
- "这个改动是否影响其他页面？" — 当修改数据层或公共组件时

#### 13.2.3 精简版文档管理规范

> **背景**：随着项目发展，文档内容会越来越长，一次性读完难度增大。精简版文档提供快速查阅入口。

**精简版文档列表**：

| 完整版本 | 精简版本 | 用途 |
|----------|----------|------|
| `开发日志.md` | `开发日志-精简版.md` | 快速了解项目开发历程 |
| `问题记录.md` | `问题记录-精简版.md` | 快速定位问题和解决方案 |

**更新规范**：
- 完整版本是**权威来源**，记录详细信息
- 精简版本是**索引/速查工具**，只包含关键摘要
- 更新顺序：先更新完整版本，再同步更新精简版本
- 精简版本必须包含指向完整版本的链接

**Agent 使用规范**：
- **开发前**：先查阅精简版了解概况，遇到具体问题再查看完整版本
- **开发后**：同步更新两个版本
- **问题排查**：先在精简版速查表中定位，再到完整版本查看详细解决方案

#### 13.2.4 版本迭代时的文档处理规范

> **背景**：V1.0 发布后如需进入后续优化，部分文档需要明确如何处理版本过渡。

| 文档 | 版本迭代策略 | 说明 |
|------|-------------|------|
| **需求文档** (`requirements.md`) | **新开文档** | V1.0 需求封版后，后续新需求写入新文档（如 `requirements-v1x.md`）。内容性质变化（从功能开发变为体验优化），分开更清晰 |
| **问题记录** (`问题记录.md`) | **同文件追加，加版本分节** | V1.0 问题编号 `V1.0-#N`，后续问题编号从 `V1.0-#N` 继续递增。如未来开新主版本则用对应前缀分隔 |
| **开发日志** (`开发日志.md`) | **同文件追加** | 日志是时间线，自然按日期追加。后续会话记录接在 V1.0 之后即可 |
| **开发阶段** (`开发阶段.md`) | **同文件追加新阶段** | 后续优化任务追加为"阶段 10"，接在阶段 9 之后 |

**版本号使用规范**：

| 文档类型 | 是否需要版本号 | 原因 |
|----------|---------------|------|
| **需求规格文档（已封版）** | ❌ 不需要 | V1.0 需求已封版不动，`requirements.md` 不再修改，无需版本号 |
| **需求规格文档（迭代中）** | ✅ 需要 | 新开迭代文档内部还有多次修订，需要版本号追踪 |
| **规范文档** | ✅ 需要 | 规则变更需要追溯，修订记录配合版本号使用 |
| **开发日志** | ❌ 不需要 | 流水账型文档，日期+会话编号已提供足够追溯信息 |
| **问题记录** | ❌ 不需要 | 问题按版本分节编号（V1.0-#N），不需要文档级版本号 |
| **开发阶段** | ✅ 需要 | 阶段状态变化需要追踪 |

**作者标注规范**：

| 场景 | 是否需要标注作者 | 说明 |
|------|-----------------|------|
| **规范/需求文档修订记录** | ✅ 需要 | 修订记录中标注每次变更的作者（遵循《通用开发规范》14.4 节） |
| **开发日志每条会话** | ✅ 需要，但不逐条 | 同作者连续多条时，只在第一条标注；切换作者时重新标注。不可完全省略 |
| **代码注释** | 可选 | 重要设计决策可在注释中标注，日常改动不需要 |

### 13.3 经验沉淀制度

| 文件 | 路径 | 说明 |
|------|------|------|
| 版本号配置 | `app/build.gradle` | `versionCode`、`versionName` |
| 更新日志 | `docs/3-开发过程/开发日志.md` | 记录每个版本的改动 |
| 需求规格 | `docs/2-需求规格/requirements*.md` | 版本规划 |

---

## 修订记录

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| V1.0 | 2026-06 | CodeBuddy | 初始版本：命名、架构、代码风格、依赖注入、数据层、UI 层、错误处理、性能、测试、Git 提交、禁止事项、审查清单 |
| V1.1 | 2026-06 | CodeBuddy | 补充：Room 聚合查询规范、连续记账模式规范、列表性能规范、导航规范、金额处理、CSV 导入导出、自动记账模板、颜色语义、零权限、安装包大小控制、核心场景测试用例 |
| V1.2 | 2026-06-17 | CodeBuddy | 新增第 13 章「开发过程管理规范」：文档体系、开发日志制度、文档同步制度、AI Agent 协作规范、临时文件管理、版本回退指南、经验沉淀制度 |
| V1.3 | 2026-06-17 | CodeBuddy | 新增 13.3.1「需求变更记录规范」：正文干净+历史可查原则、第10章变更记录模板（6列表格含原文摘要+决策理由）、重大变更特殊处理、HTML批注用法；文档同步表需求变更行细化；Agent结束动作新增第4步 |
| V1.4 | 2026-06-17 | CodeBuddy | 新增 2.3「公共组件复用原则」：组件归属规则（1个页面 vs ≥2个页面）、新页面开发前组件搜索清单、同功能同组件原则、组件文档要求；禁止事项新增2条；审查清单新增2条 |
| V1.5 | 2026-06-17 | CodeBuddy | 新增 13.8「版本号管理规范」：两类版本号定义（文档修订/产品功能）、递增规则、标注要求、节级版本标注；标题版本号从 V1.2 修正为 V1.5；所有一级节和关键子节补加版本标注 |
| V1.6 | 2026-06-17 | CodeBuddy | 第 13 章去重：将重复的通用过程管理规范替换为对《通用开发规范》第 6 章的引用 + MyPurse 文件名映射表；删除重复的修订记录；修订记录增加"作者"列 |
| V1.7 | 2026-06-20 | trae | 新增第 14 章「文件改动检查清单」：改动类型与对应操作、检查流程（8步）、作者标注规范引用、修订记录模板、常见遗漏场景；源于会话 #25 遗漏修订记录的教训 ⚠️ 第 14 章正文在代码回退中丢失（2026-06-21 确认：仍未恢复，功能由 13.2.2 审查要点部分覆盖） |
| V1.8 | 2026-06-20 | trae | **代码安全操作规范强化**：①通用开发规范 0.3 节新增第 0 条"编译验证"为强制前置步骤；②MyPurse开发规范新增 14.6「代码安全操作规范」：文件编辑原则、编译验证规范、构建缓存问题处理、Android/Kotlin 常见错误速查；源于会话 #29 编译失败教训 ⚠️ 14.6 节正文在代码回退中丢失（2026-06-21 确认：仍未恢复，编译验证流程由通用开发规范 0.3 节覆盖） |
| V1.9 | 2026-06-20 | trae | 新增第 15 章「版本更新流程规范」：版本定义与命名规则、版本发布流程（需求冻结→代码冻结→测试验证→文档同步→构建发布）、发布前检查清单、回滚流程；解决版本更新缺乏指导的问题 ⚠️ 第 15 章正文在代码回退中丢失（2026-06-21 确认：仍未恢复，发布流程由 13.2.4 版本迭代文档处理规范部分覆盖） |
| V1.10 | 2026-06-20 | trae | **规范强化**：①新增 13.2.1「Agent 开发前必读章节清单」— ✅ 已由 V1.12 恢复；②新增 13.2.2「人类审查要点规范」— ✅ 已由 V1.12 恢复；③15.5 新增「版本规划指导」— ⚠️ 正文仍丢失；④15.6 新增「流程规范职责划分」— ⚠️ 正文仍丢失 |
| V1.11 | 2026-06-20 | trae | 新增 13.2.4「精简版文档管理规范」：精简版文档列表、更新规范、Agent 使用规范；Agent 开发结束后步骤新增精简版同步；解决文档过长难以快速查阅的问题 |
| V1.12 | 2026-06-20 | codebuddy | **文档体系完善**：①恢复 13.2.1「Agent 开发前必读章节清单」和 13.2.2「人类审查要点规范」（Trae 会话 #26 回退后丢失）；②新增 13.2.4「版本迭代时的文档处理规范」；③修正版本号使用规范表（区分需要/不需要版本号的文档类型）；④修正作者标注规范（开发日志需要标注但不逐条）；⑤修复 13.2.5 被 15.4 内容污染的表格错误 |
| V1.13 | 2026-06-20 | codebuddy | **长远扩展性设计原则**：①新增 2.3.5「长远扩展性设计原则」— 跨页面复用、用户可自定义、数据层扩展、同类功能一致性 4 项强制检查清单 + 已有教训反例表；②更新 13.2.1 必读清单（加入 2.3.5）；③更新 13.2.2 审查要点（新增扩展性考量、公共组件未重复实现）；④修正版本号使用规范表（区分已封版需求文档 vs 迭代中需求文档）；⑤`requirements.md` 标题标注"V1.0 封版" |
| V1.14 | 2026-06-21 | codebuddy | **全局影响分析制度**：①新增 2.3.6「全局影响分析制度」— 触发条件、4 步执行流程、输出要求、反例表；②更新 13.2.1 必读清单（加入 2.3.6）；③更新 13.2.2 审查要点（新增全局影响分析已执行检查项）；④更新第 12 章审查清单（新增全局影响分析检查项） |
| V1.15 | 2026-06-21 | codebuddy | **澄清"三场景原则"冲突**：①2.3.1 补充说明块，明确本项目采用 ≥2 即抽取（非业界 Rule of Three），解释原因；②同步修正 `问题记录.md`、`requirements-v1x.md`、`V1.0回退后待修复清单.md` 三处引用为规范一致表述 |