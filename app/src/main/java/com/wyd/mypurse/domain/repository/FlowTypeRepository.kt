package com.wyd.mypurse.domain.repository

/**
 * 流水类型定义。
 *
 * V1.0 仅内置"支出"和"收入"两种，不开放自定义。
 * sign 用于驱动分类过滤：-1 显示支出侧分类，1 显示收入侧分类。
 * 预留 add/delete/restore 接口供后续版本扩展。
 *
 * @param name 类型名称
 * @param sign 正负归属：-1 计入支出侧，1 计入收入侧
 * @param isDefault 是否内置
 */
data class FlowType(
    val name: String,
    val sign: Int = -1,
    val isDefault: Boolean = false
) {
    companion object {
        val EXPENSE = FlowType(name = "支出", sign = -1, isDefault = true)
        val INCOME = FlowType(name = "收入", sign = 1, isDefault = true)
    }
}

/**
 * 流水类型管理 Repository 接口。
 * V1.0 仅返回内置的支出/收入，自定义功能留待后续版本。
 */
interface FlowTypeRepository {

    /** 获取所有流水类型（V1.0 固定返回支出 + 收入） */
    suspend fun getAllFlowTypes(): List<FlowType>

    /** [预留] 新增自定义流水类型 */
    suspend fun addFlowType(flowType: FlowType)

    /** [预留] 删除流水类型 */
    suspend fun deleteFlowType(name: String)

    /** [预留] 恢复默认流水类型 */
    suspend fun restoreDefaultFlowTypes()
}
