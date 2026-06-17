package com.wyd.mypurse.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.wyd.mypurse.domain.model.Transaction
import java.io.File
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV 导出工具。
 * 格式规范见 requirements.md §3.6.1：UTF-8 BOM、逗号分隔、双引号转义、日期 yyyy-MM-dd。
 */
object CsvExporter {

    private const val HEADER = "流水类型,一级分类,二级分类,金额,备注,日期"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * 将交易列表导出为 CSV 文件，返回文件 Uri 供系统分享。
     * 文件写入应用专属外部存储（无需权限）。
     */
    fun export(context: Context, transactions: List<Transaction>, fileName: String): Uri {
        val file = File(context.getExternalFilesDir(null), fileName)
        OutputStreamWriter(file.outputStream(), Charsets.UTF_8).use { writer ->
            // 写入 UTF-8 BOM（确保 Excel 直接打开不乱码）
            writer.write('\uFEFF'.code)
            writer.write(HEADER)
            writer.write("\r\n")

            for (tx in transactions) {
                writer.write(formatRow(tx))
                writer.write("\r\n")
            }
            writer.flush()
        }
        // 使用 FileProvider 生成 content URI（供分享 Intent）
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 格式化单行数据。
     */
    private fun formatRow(tx: Transaction): String {
        return listOf(
            escapeField(tx.flowType),
            escapeField(tx.categoryL1),
            escapeField(tx.categoryL2 ?: ""),
            formatAmount(tx.amount),
            escapeField(tx.note ?: ""),
            dateFormat.format(Date(tx.date))
        ).joinToString(",")
    }

    /**
     * 金额格式：纯数字两位小数，不含货币符号和千分位。
     */
    private fun formatAmount(amount: BigDecimal): String {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * CSV 字段转义：含逗号、双引号或换行时用双引号包裹，内部双引号转义为 ""。
     */
    internal fun escapeField(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
