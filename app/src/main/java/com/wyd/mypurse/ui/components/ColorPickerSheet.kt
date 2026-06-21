package com.wyd.mypurse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
/**
 * V1.1 颜色选择器 BottomSheet。
 *
 * 两种模式：
 * - 预设模式（默认）：12 个推荐色块网格 + "自定义"按钮
 * - 自定义模式：HSV 色相环取色
 *
 * 供 11d（分类颜色）和 11e（图表配色）复用。
 *
 * @param initialColor 初始颜色（用于预览和预设选中标记）
 * @param onColorSelected 用户确认后的回调，传递选中的 Color
 * @param onDismiss 关闭回调
 * @param onRestoreDefault 可选："恢复默认颜色"回调（仅 11d 内置分类显示）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    onRestoreDefault: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var selectedColor by rememberSaveable {
        mutableStateOf(
            colorToHex(initialColor)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCustom) {
                    IconButton(onClick = { showCustom = false }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "返回")
                    }
                }
                Text(
                    text = if (showCustom) "自定义取色" else "选择颜色",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showCustom) {
                CustomColorPicker(
                    initialHex = selectedColor,
                    onColorConfirmed = { newColor ->
                        selectedColor = colorToHex(newColor)
                        showCustom = false
                    }
                )
            } else {
                PresetPalette(
                    selectedHex = selectedColor,
                    onColorPicked = { hex ->
                        selectedColor = hex
                    },
                    onCustomClick = { showCustom = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：恢复默认（仅内置分类显示）
                if (onRestoreDefault != null) {
                    OutlinedButton(onClick = onRestoreDefault) {
                        Text("恢复默认颜色")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = {
                        val color = parseHexColor(selectedColor) ?: initialColor
                        onColorSelected(color)
                        onDismiss()
                    }) {
                        Text("确认")
                    }
                }
            }
        }
    }
}

/** 预设色盘：12 色块 + "自定义"按钮 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetPalette(
    selectedHex: String,
    onColorPicked: (String) -> Unit,
    onCustomClick: () -> Unit
) {
    val selectedColor = remember(selectedHex) {
        parseHexColor(selectedHex) ?: Color.Unspecified
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PresetColors.forEach { color ->
            val hex = colorToHex(color)
            val isSelected = hex == selectedHex
            PresetColorSwatch(
                color = color,
                isSelected = isSelected,
                onClick = { onColorPicked(hex) }
            )
        }

        // "自定义"按钮
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onCustomClick),
            contentAlignment = Alignment.Center
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 当前选中的预览
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(selectedColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedHex,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PresetColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .background(color)
            .clickable(onClick = onClick)
    )
}

/**
 * 自定义取色模式：HSV 色相环。
 * 内部管理 hue/saturation/brightness 状态。
 */
@Composable
private fun CustomColorPicker(
    initialHex: String,
    onColorConfirmed: (Color) -> Unit
) {
    val initialColor = remember {
        parseHexColor(initialHex) ?: Color.Red
    }
    val initialHsv = remember(initialColor) {
        getHsv(initialColor)
    }

    var hue by remember { mutableFloatStateOf(initialHsv.first) }
    var saturation by remember { mutableFloatStateOf(initialHsv.second) }
    var brightness by remember { mutableFloatStateOf(initialHsv.third) }
    val currentColor = remember(hue, saturation, brightness) {
        Color.hsv(hue, saturation, brightness)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ColorWheel(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onColorChanged = { h, s, v ->
                hue = h
                saturation = s
                brightness = v
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onColorConfirmed(currentColor) },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentColor,
                contentColor = if (brightness > 0.5f) Color.Black else Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("确定")
        }
    }
}

/** 12 个预设推荐色 */
val PresetColors = listOf(
    Color(0xFFE53935), // 红
    Color(0xFFE91E63), // 粉
    Color(0xFF9C27B0), // 紫
    Color(0xFF673AB7), // 深紫
    Color(0xFF3F51B5), // 靛蓝
    Color(0xFF2196F3), // 蓝
    Color(0xFF00BCD4), // 青
    Color(0xFF009688), // 墨绿
    Color(0xFF4CAF50), // 绿
    Color(0xFFFF9800), // 橙
    Color(0xFFFF5722), // 深橙
    Color(0xFF795548), // 棕
)

/** 解析 "#RRGGBB" → Color，失败返回 null */
private fun parseHexColor(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        val r = clean.substring(0, 2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        Color(r, g, b)
    } catch (_: Exception) {
        null
    }
}

/** Color → (hue, saturation, brightness) */
private fun getHsv(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return Triple(hsv[0], hsv[1], hsv[2])
}
