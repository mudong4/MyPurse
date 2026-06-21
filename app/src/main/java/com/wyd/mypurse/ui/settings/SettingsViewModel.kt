package com.wyd.mypurse.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.data.export.CsvExporter
import com.wyd.mypurse.data.export.CsvImporter
import com.wyd.mypurse.data.export.ImportResult
import com.wyd.mypurse.data.export.ParsedTransaction
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ========== 导出 CSV ==========

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionRepository.getAllTransactionsOnce()
                }
                if (transactions.isEmpty()) {
                    _uiState.update {
                        it.copy(isExporting = false, errorMessage = "暂无记录可导出")
                    }
                    return@launch
                }
                val fileName = "MyPurse_${dateFormat.format(Date())}.csv"
                val uri = withContext(Dispatchers.IO) {
                    CsvExporter.export(application, transactions, fileName)
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportedFileUri = uri.toString()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "导出失败：${e.message}")
                }
            }
        }
    }

    fun onExportShared() {
        _uiState.update { it.copy(exportedFileUri = null) }
    }

    // ========== 导入 CSV ==========

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResultMessage = null, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val inputStream = application.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("无法打开文件")
                    val (parsed, parseResult) = inputStream.use { CsvImporter.parse(it) }

                    if (parsed.isEmpty() && parseResult.errorCount > 0) {
                        return@withContext parseResult.toSummary() +
                                if (parseResult.errors.isNotEmpty()) {
                                    "\n错误详情：\n${parseResult.errors.joinToString("\n")}"
                                } else ""
                    }
                    if (parsed.isEmpty()) {
                        return@withContext "没有可导入的记录"
                    }

                    // 逐条导入：匹配分类或自动创建
                    val allCategories = categoryRepository.getAllCategoriesOnce()
                    var successCount = 0
                    var skipCount = 0
                    val importErrors = mutableListOf<String>()

                    for ((index, parsedTx) in parsed.withIndex()) {
                        try {
                            val result = importOneTransaction(parsedTx, allCategories)
                            if (result) successCount++ else skipCount++
                        } catch (e: Exception) {
                            importErrors.add("第 ${index + 2} 行（CSV）：${e.message}")
                        }
                    }

                    val finalResult = ImportResult(
                        successCount = successCount,
                        skippedCount = parseResult.skippedCount + skipCount,
                        errorCount = parseResult.errorCount + importErrors.size,
                        errors = parseResult.errors + importErrors
                    )
                    finalResult.toSummary() +
                            if (finalResult.errors.isNotEmpty()) {
                                "\n错误详情：\n${finalResult.errors.take(10).joinToString("\n")}"
                            } else ""
                }
                _uiState.update {
                    it.copy(isImporting = false, importResultMessage = result)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, errorMessage = "导入失败：${e.message}")
                }
            }
        }
    }

    fun dismissImportResult() {
        _uiState.update { it.copy(importResultMessage = null) }
    }

    /**
     * 导入单条记录：匹配已有分类或自动创建。
     * @return true 表示导入成功，false 表示跳过
     */
    private suspend fun importOneTransaction(
        parsed: ParsedTransaction,
        allCategories: List<com.wyd.mypurse.domain.model.Category>
    ): Boolean {
        // 确定 flowSign
        val flowSign = if (parsed.flowType == "支出") 0 else 1

        // 查找一级分类
        var l1Category = allCategories.find {
            it.parentId == null && it.name == parsed.categoryL1 && it.flowSign == flowSign
        }
        val l1Id: Long
        if (l1Category != null) {
            l1Id = l1Category.id
        } else {
            // 自动创建一级分类
            l1Id = categoryRepository.addCategory(
                name = parsed.categoryL1,
                parentId = null,
                isDefault = false,
                flowSign = flowSign
            )
        }

        // 查找或创建二级分类
        val l2Id: Long? = if (!parsed.categoryL2.isNullOrBlank()) {
            val subs = if (l1Category != null) {
                categoryRepository.getSubCategoriesOnce(l1Id)
            } else emptyList()
            var l2Category = subs.find { it.name == parsed.categoryL2 }
            if (l2Category != null) {
                l2Category.id
            } else {
                categoryRepository.addCategory(
                    name = parsed.categoryL2,
                    parentId = l1Id,
                    isDefault = false,
                    flowSign = flowSign
                )
            }
        } else null

        // 插入记录
        transactionRepository.insertTransaction(
            flowType = parsed.flowType,
            categoryL1Id = l1Id,
            categoryL2Id = l2Id,
            categoryL1 = parsed.categoryL1,
            categoryL2 = parsed.categoryL2,
            amount = parsed.amount,
            note = parsed.note,
            date = parsed.date
        )
        return true
    }

    // ========== 清除所有数据 ==========

    fun showClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }

    fun dismissClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }

    fun confirmClearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(showClearDataDialog = false, isClearingData = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    transactionRepository.clearAllData()
                }
                _uiState.update { it.copy(isClearingData = false, clearDataComplete = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isClearingData = false, errorMessage = "清除失败：${e.message}")
                }
            }
        }
    }

    fun onClearDataCompleteHandled() {
        _uiState.update { it.copy(clearDataComplete = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

}
