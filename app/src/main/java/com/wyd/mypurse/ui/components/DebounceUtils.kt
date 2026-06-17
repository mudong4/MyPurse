package com.wyd.mypurse.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 快速点击防抖工具。
 *
 * 防抖窗口默认 500ms，在此窗口内的重复点击将被忽略。
 * 适用于导航、保存、删除等可能被用户快速双击的操作。
 *
 * 使用方式：
 * ```
 * val debounce = rememberDebounce()
 * Button(onClick = { debounce { doSomething() } }) { ... }
 * ```
 */
@Composable
fun rememberDebounce(delayMs: Long = 500): (() -> Unit) -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return { action ->
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= delayMs) {
            lastClickTime = now
            action()
        }
    }
}
