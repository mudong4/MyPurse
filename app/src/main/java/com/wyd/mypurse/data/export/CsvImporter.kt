package com.wyd.mypurse.data.export

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * CSV 导入结果。
 */
data class ImportResult(
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList()
) {
    fun toSummary(): String = "成功 $successCount 条，跳过 $skippedCount 条，错误 $errorCount 条"
}

/**
 * CSV 解析后的单条记录。
 */
data class ParsedTransaction(
    val flowType: String,
    val categoryL1: String,
    val categoryL2: String?,
    val amount: BigDecimal,
    val note: String?,
    val date: Long
)

/**
 * CSV 导入工具。
 * 解析 UTF-8 编码的 CSV 文件，校验格式，返回解析结果。
 * 格式规范见 requirements.md §3.6.1。
 */
object CsvImporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val requiredHeaders = listOf("流水类型", "一级分类", "二级分类", "金额", "备注", "日期")
    private val validFlowTypes = setOf("支出", "收入")

    /**
     * 解析 CSV 输入流，返回解析结果。
     * 校验失败的行跳过，不阻塞其他行。
     */
    fun parse(inputStream: InputStream): Pair<List<ParsedTransaction>, ImportResult> {
        val transactions = mutableListOf<ParsedTransaction>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0

        // 读取前处理 BOM
        val rawBytes = inputStream.readBytes()
        val content = if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            String(rawBytes, 3, rawBytes.size - 3, Charsets.UTF_8)
        } else {
            String(rawBytes, Charsets.UTF_8)
        }

        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Pair(emptyList(), ImportResult(errorCount = 1, errors = listOf("文件为空")))
        }

        // 解析表头，建立列索引映射
        val headerLine = lines.first().trim()
        val headers = parseCsvLine(headerLine)
        val columnIndex = mutableMapOf<String, Int>()
        for (h in requiredHeaders) {
            val idx = headers.indexOf(h)
            if (idx == -1) {
                return Pair(emptyList(), ImportResult(
                    errorCount = 1,
                    errors = listOf("缺少必需列：$h（表头：$headerLine）")
                ))
            }
            columnIndex[h] = idx
        }

        // 逐行解析数据
        for (i in 1 until lines.size) {
            val lineNum = i + 1 // 行号（1-based，含表头）
            val line = lines[i].trim()
            if (line.isBlank()) {
                skippedCount++
                continue
            }
            try {
                val fields = parseCsvLine(line)
                // 确保字段数足够
                if (fields.size < requiredHeaders.size) {
                    errors.add("第 $lineNum 行：列数不足（期望 ${requiredHeaders.size}，实际 ${fields.size}）")
                    errorCount++
                    continue
                }
                val parsed = parseRow(fields, columnIndex, lineNum)
                if (parsed != null) {
                    transactions.add(parsed)
                    successCount++
                } else {
                    // parseRow 内部已记录错误
                    errorCount++
                }
            } catch (e: Exception) {
                errors.add("第 $lineNum 行：解析异常 - ${e.message}")
                errorCount++
            }
        }

        return Pair(
            transactions,
            ImportResult(
                successCount = successCount,
                skippedCount = skippedCount,
                errorCount = errorCount,
                errors = errors.take(20) // 最多保留前 20 条错误
            )
        )
    }

    /**
     * 解析单行数据为 ParsedTransaction，校验失败返回 null。
     */
    private fun parseRow(
        fields: List<String>,
        columnIndex: Map<String, Int>,
        lineNum: Int
    ): ParsedTransaction? {
        fun get(idx: Int): String = if (idx < fields.size) fields[idx] else ""

        val flowType = get(columnIndex["流水类型"]!!)
        val categoryL1 = get(columnIndex["一级分类"]!!)
        val categoryL2Raw = get(columnIndex["二级分类"]!!)
        val amountRaw = get(columnIndex["金额"]!!)
        val noteRaw = get(columnIndex["备注"]!!)
        val dateRaw = get(columnIndex["日期"]!!)

        // 校验流水类型
        if (flowType !in validFlowTypes) {
            return null
        }

        // 校验一级分类不为空
        if (categoryL1.isBlank()) {
            return null
        }

        // 校验金额
        val amount = try {
            val d = BigDecimal(amountRaw.trim())
            if (d <= BigDecimal.ZERO) return null
            d
        } catch (e: NumberFormatException) {
            return null
        }

        // 校验日期
        val date = try {
            dateFormat.parse(dateRaw.trim())?.time ?: return null
        } catch (e: Exception) {
            return null
        }

        return ParsedTransaction(
            flowType = flowType,
            categoryL1 = categoryL1.trim(),
            categoryL2 = categoryL2Raw.trim().takeIf { it.isNotEmpty() },
            amount = amount,
            note = noteRaw.trim().takeIf { it.isNotEmpty() },
            date = date
        )
    }

    /**
     * 解析 CSV 单行为字段列表。
     * 处理双引号包裹的字段（含逗号、换行、转义双引号）。
     */
    internal fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> {
                    inQuotes = true
                }
                ch == '"' && inQuotes -> {
                    // 检查是否是转义双引号 ""
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // 跳过一个引号
                    } else {
                        inQuotes = false
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(ch)
                }
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
