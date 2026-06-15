package com.wyd.mypurse.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wyd.mypurse.ui.about.AboutScreen
import com.wyd.mypurse.ui.addtransaction.AddTransactionScreen
import com.wyd.mypurse.ui.budget.BudgetScreen
import com.wyd.mypurse.ui.categorymanage.CategoryManageScreen
import com.wyd.mypurse.ui.home.HomeScreen
import com.wyd.mypurse.ui.recurring.RecurringTemplateEditScreen
import com.wyd.mypurse.ui.recurring.RecurringTemplateListScreen
import com.wyd.mypurse.ui.settings.SettingsScreen
import com.wyd.mypurse.ui.statistics.StatisticsScreen
import com.wyd.mypurse.ui.transactionlist.TransactionListScreen

/**
 * 底部导航栏 Tab 定义。
 */
data class BottomNavItem(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Route.Home, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Route.Statistics, "统计", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    BottomNavItem(Route.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * 应用根导航组件。管理底部导航栏的显示/隐藏以及所有页面路由。
 */
@Composable
fun MyPurseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 需要隐藏底部导航栏的页面路由
    val hideBottomBarRoutes = setOf(
        Route.AddTransaction::class,
        Route.TransactionList::class,
        Route.CategoryManage::class,
        Route.Budget::class,
        Route.RecurringTemplateList::class,
        Route.RecurringTemplateEdit::class,
        Route.About::class,
    )

    val showBottomBar = hideBottomBarRoutes.none { routeClass ->
        currentDestination?.hasRoute(routeClass) == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 首页 Tab
            composable<Route.Home> {
                HomeScreen(
                    onNavigateToAddTransaction = {
                        navController.navigate(Route.AddTransaction())
                    },
                    onNavigateToTransactionList = { granularity ->
                        navController.navigate(Route.TransactionList(timeGranularity = granularity))
                    },
                    onNavigateToBudget = {
                        navController.navigate(Route.Budget())
                    },
                    onNavigateToStatistics = { view ->
                        navController.navigate(Route.Statistics)
                    }
                )
            }

            // 统计 Tab
            composable<Route.Statistics> {
                StatisticsScreen(
                    onNavigateToTransactionList = { granularity, category ->
                        navController.navigate(
                            Route.TransactionList(
                                timeGranularity = granularity,
                                categoryFilter = category
                            )
                        )
                    }
                )
            }

            // 设置 Tab
            composable<Route.Settings> {
                SettingsScreen(
                    onNavigateToCategoryManage = {
                        navController.navigate(Route.CategoryManage)
                    },
                    onNavigateToBudget = {
                        navController.navigate(Route.Budget())
                    },
                    onNavigateToRecurringTemplate = {
                        navController.navigate(Route.RecurringTemplateList)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Route.About)
                    },
                    onExportCsv = { /* TODO: 阶段 4 */ },
                    onImportCsv = { /* TODO: 阶段 4 */ },
                    onClearAllData = { /* TODO: 阶段 5 */ }
                )
            }

            // 记一笔页（隐藏底部导航栏）
            composable<Route.AddTransaction> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.AddTransaction>()
                AddTransactionScreen(
                    defaultFlowType = route.defaultFlowType,
                    defaultDate = route.defaultDate,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategoryManage = {
                        navController.navigate(Route.CategoryManage)
                    }
                )
            }

            // 流水列表页
            composable<Route.TransactionList> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.TransactionList>()
                TransactionListScreen(
                    timeGranularity = route.timeGranularity,
                    categoryFilter = route.categoryFilter,
                    timeRangeStart = route.timeRangeStart,
                    timeRangeEnd = route.timeRangeEnd,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 分类管理页
            composable<Route.CategoryManage> {
                CategoryManageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 预算设置页
            composable<Route.Budget> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Budget>()
                BudgetScreen(
                    year = route.year,
                    month = route.month,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 固定收支模板列表页
            composable<Route.RecurringTemplateList> {
                RecurringTemplateListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { templateId ->
                        navController.navigate(Route.RecurringTemplateEdit(templateId = templateId))
                    }
                )
            }

            // 固定收支模板编辑页
            composable<Route.RecurringTemplateEdit> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.RecurringTemplateEdit>()
                RecurringTemplateEditScreen(
                    templateId = route.templateId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 关于页
            composable<Route.About> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
