package com.wyd.mypurse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HSV 色相环取色器。
 *
 * 绘制逻辑：
 * 1. SweepGradient（0°→360°色相环）+ RadialGradient（中心白→边缘透明=饱和度 0→1）
 * 2. 叠加产生完整的 HSV 色盘（V=1 固定，V 由下方滑块单独控制）
 * 3. 手势：拖拽/点击色相环 → 更新 hue/saturation
 *
 * @param hue 当前色相 [0f, 360f)
 * @param saturation 当前饱和度 [0f, 1f]
 * @param brightness 当前明度 [0f, 1f]
 * @param onColorChanged 颜色变更回调，参数 (hue, saturation, brightness)
 * @param modifier 修饰符
 * @param wheelDiameter 色相环直径，默认 260dp
 */
@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChanged: (hue: Float, saturation: Float, brightness: Float) -> Unit,
    modifier: Modifier = Modifier,
    wheelDiameter: Int = 260
) {
    val wheelDiameterDp = wheelDiameter.dp
    val wheelRadius = wheelDiameter / 2f

    // 当前选中颜色预览
    val selectedColor = remember(hue, saturation, brightness) {
        Color.hsv(hue, saturation, brightness)
    }

    // 色相环渐变色（0→360°）
    val hueColors = remember {
        (0..360 step 30).map { angle ->
            Color.hsv(angle.toFloat(), 1f, 1f)
        }
    }
    val sweepBrush = remember {
        Brush.sweepGradient(hueColors)
    }

    // 从中心到边缘的饱和度渐变
    val saturationBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color.White, Color.Transparent),
            center = Offset(wheelRadius, wheelRadius),
            radius = wheelRadius
        )
    }

    // --- 指示器位置计算 ---
    val indicatorAngleRad = hue * PI.toFloat() / 180f
    val indicatorRadius = saturation * wheelRadius
    val indicatorX = wheelRadius + indicatorRadius * cos(indicatorAngleRad)
    val indicatorY = wheelRadius + indicatorRadius * sin(indicatorAngleRad)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 色相环
        Box(
            modifier = Modifier.size(wheelDiameterDp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(wheelDiameterDp)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val (h, s) = cartesianToHsv(offset, wheelRadius)
                            onColorChanged(h, s, brightness)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val (h, s) = cartesianToHsv(
                                change.position, wheelRadius
                            )
                            onColorChanged(h, s, brightness)
                        }
                    }
            ) {
                val canvasSize = Size(wheelDiameter.toFloat(), wheelDiameter.toFloat())
                // 1. 色相环底色
                drawOval(brush = sweepBrush, size = canvasSize)
                // 2. 饱和度蒙层
                drawOval(brush = saturationBrush, size = canvasSize)
                // 3. 指示器圆环
                drawCircle(
                    color = if (brightness > 0.5f) Color.Black else Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(indicatorX, indicatorY),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = selectedColor,
                    radius = 5.dp.toPx(),
                    center = Offset(indicatorX, indicatorY)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 明度滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "明度",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(36.dp)
            )
            Slider(
                value = brightness,
                onValueChange = { v ->
                    onColorChanged(hue, saturation, v)
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = selectedColor,
                    activeTrackColor = selectedColor
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 预览色块 + HEX
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(selectedColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = colorToHex(selectedColor),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 笛卡尔坐标 → (hue, saturation)。超出色相环区域（距离 > radius）则钳位到边缘。
 */
private fun cartesianToHsv(offset: Offset, radius: Float): Pair<Float, Float> {
    val dx = offset.x - radius
    val dy = offset.y - radius
    val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
    val sat = if (radius > 0f) dist / radius else 0f
    val hue = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
    return hue to sat
}

/**
 * Compose Color → "#RRGGBB" 大写十六进制字符串。
 */
fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
