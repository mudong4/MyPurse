package com.wyd.mypurse.ui.navigation

import kotlinx.serialization.Serializable

/**
 * 应用路由定义。使用 @Serializable 确保类型安全传参，禁止字符串拼接路由。
 */
@Serializable
sealed class Route {

    /** 首页 Tab */
    @Serializable
    data object Home : Route()

    /** 统计 Tab */
    @Serializable
    data object Statistics : Route()

    /** 设置 Tab */
    @Serializable
    data object Settings : Route()

    /** 记一笔页（独立路由，无底部导航栏）。编辑模式传入 transactionId > 0 */
    @Serializable
    data class AddTransaction(
        val defaultFlowType: String = "支出",
        val defaultDate: Long = System.currentTimeMillis(),
        val transactionId: Long = 0,
        val defaultAmount: String = "",
        val defaultNote: String = "",
        val preselectRefundCategory: Boolean = false
    ) : Route()

    /** 流水列表页 */
    @Serializable
    data class TransactionList(
        val timeGranularity: String = "month",
        val categoryL1Id: Long? = null,
        val timeRangeStart: Long? = null,
        val timeRangeEnd: Long? = null
    ) : Route()

    /** 分类管理页 */
    @Serializable
    data object CategoryManage : Route()

    /** 预算设置页 */
    @Serializable
    data class Budget(
        val year: Int? = null,
        val month: Int? = null
    ) : Route()

    /** 关于页 */
    @Serializable
    data object About : Route()

    /** 固定收支模板列表页 */
    @Serializable
    data object TemplateList : Route()

    /** 固定收支模板编辑页（新建/编辑），templateId=null 为新建 */
    @Serializable
    data class TemplateEdit(
        val templateId: Long? = null
    ) : Route()
}
