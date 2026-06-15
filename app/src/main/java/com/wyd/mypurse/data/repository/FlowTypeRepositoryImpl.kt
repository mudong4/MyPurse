package com.wyd.mypurse.data.repository

import com.wyd.mypurse.domain.repository.FlowType
import com.wyd.mypurse.domain.repository.FlowTypeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 流水类型 Repository 实现。
 * V1.0 固定返回内置的支出/收入，自定义功能留待后续版本。
 * 后续扩展时可改为 SharedPreferences 或 Room 存储。
 */
@Singleton
class FlowTypeRepositoryImpl @Inject constructor() : FlowTypeRepository {

    override suspend fun getAllFlowTypes(): List<FlowType> {
        return listOf(FlowType.EXPENSE, FlowType.INCOME)
    }

    override suspend fun addFlowType(flowType: FlowType) {
        // V1.0 不支持自定义流水类型
    }

    override suspend fun deleteFlowType(name: String) {
        // V1.0 不支持删除流水类型
    }

    override suspend fun restoreDefaultFlowTypes() {
        // V1.0 无自定义类型，无需恢复
    }
}
