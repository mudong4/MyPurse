package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wyd.mypurse.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 固定收支模板数据访问对象。阶段 4 完整实现，阶段 1 建基础结构。
 */
@Dao
interface RecurringTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: RecurringTemplateEntity)

    @Query("SELECT * FROM recurring_template ORDER BY create_time DESC")
    fun getAllTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_template WHERE is_active = 1")
    suspend fun getActiveTemplates(): List<RecurringTemplateEntity>

    @Query("SELECT * FROM recurring_template WHERE id = :id")
    suspend fun getTemplateById(id: Long): RecurringTemplateEntity?

    @Query("DELETE FROM recurring_template WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    /** 清空全部模板记录 */
    @Query("DELETE FROM recurring_template")
    suspend fun deleteAllTemplates()
}
