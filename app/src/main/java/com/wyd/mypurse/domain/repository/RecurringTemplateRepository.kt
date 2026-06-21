package com.wyd.mypurse.domain.repository

import com.wyd.mypurse.domain.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 固定收支模板 Repository 接口。
 * 定义数据访问契约，不依赖任何 Android 框架。
 */
interface RecurringTemplateRepository {

    /** Flow 查询全部模板（供列表页响应式更新） */
    fun getAllTemplates(): Flow<List<RecurringTemplate>>

    /** 挂起查询全部启用模板（供引擎使用） */
    suspend fun getActiveTemplates(): List<RecurringTemplate>

    /** 按 ID 查询单个模板 */
    suspend fun getTemplateById(id: Long): RecurringTemplate?

    /** 新增模板，返回新 ID */
    suspend fun insertTemplate(template: RecurringTemplate): Long

    /** 更新模板 */
    suspend fun updateTemplate(template: RecurringTemplate)

    /** 删除模板 */
    suspend fun deleteTemplate(id: Long)

    /** 清空全部模板 */
    suspend fun deleteAllTemplates()
}
