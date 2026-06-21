package com.wyd.mypurse.data.repository

import com.wyd.mypurse.data.local.dao.RecurringTemplateDao
import com.wyd.mypurse.data.local.entity.RecurringTemplateEntity
import com.wyd.mypurse.domain.model.RecurringTemplate
import com.wyd.mypurse.domain.repository.RecurringTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 固定收支模板 Repository 本地实现。
 * 通过 Room DAO 完成 CRUD，并做 Entity ↔ Domain 映射。
 */
@Singleton
class RecurringTemplateRepositoryImpl @Inject constructor(
    private val dao: RecurringTemplateDao
) : RecurringTemplateRepository {

    override fun getAllTemplates(): Flow<List<RecurringTemplate>> =
        dao.getAllTemplates().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getActiveTemplates(): List<RecurringTemplate> =
        dao.getActiveTemplates().map { it.toDomain() }

    override suspend fun getTemplateById(id: Long): RecurringTemplate? =
        dao.getTemplateById(id)?.toDomain()

    override suspend fun insertTemplate(template: RecurringTemplate): Long =
        dao.insertTemplate(template.toEntity())

    override suspend fun updateTemplate(template: RecurringTemplate) =
        dao.updateTemplate(template.toEntity().copy(id = template.id))

    override suspend fun deleteTemplate(id: Long) =
        dao.deleteTemplate(id)

    override suspend fun deleteAllTemplates() =
        dao.deleteAllTemplates()

    // ---- 映射 ----

    private fun RecurringTemplateEntity.toDomain() = RecurringTemplate(
        id = id,
        flowType = flowType,
        categoryL1Id = categoryL1Id,
        categoryL2Id = categoryL2Id,
        amount = amount,
        note = note,
        cycleType = cycleType,
        cycleValue = cycleValue,
        isActive = isActive,
        lastExecutedDate = lastExecutedDate,
        nextExecutionDate = nextExecutionDate,
        createTime = createTime
    )

    private fun RecurringTemplate.toEntity() = RecurringTemplateEntity(
        id = id,
        flowType = flowType,
        categoryL1Id = categoryL1Id,
        categoryL2Id = categoryL2Id,
        amount = amount,
        note = note,
        cycleType = cycleType,
        cycleValue = cycleValue,
        isActive = isActive,
        lastExecutedDate = lastExecutedDate,
        nextExecutionDate = nextExecutionDate,
        createTime = createTime
    )
}
