package com.wyd.mypurse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.wyd.mypurse.data.local.UserPreferencesRepository
import com.wyd.mypurse.ui.navigation.MyPurseNavHost
import com.wyd.mypurse.ui.theme.MyPurseTheme
import com.wyd.mypurse.ui.theme.findPresetByName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * 应用唯一 Activity。
 *
 * 启动页方案：在 Compose 层先渲染全屏启动图（用户提供的加载界面.png），
 * 短暂展示后切换到主页面。绕过了 Android 系统主题的 windowBackground 限制。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 冷启动 starting window 使用 Splash 主题（浅绿色背景，无图标闪现），
        // Activity 创建后立即切回主主题，由 Compose 层接管渲染。
        setTheme(R.style.Theme_MyPurse)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val presetName by userPreferencesRepository.themePresetName
                .collectAsState(initial = "默认紫")
            val preset = findPresetByName(presetName)

            MyPurseTheme(preset = preset) {
                AppEntry()
            }
        }
    }
}

/**
 * 应用入口：先显示启动图，短暂延迟后切换到主界面。
 */
@Composable
fun AppEntry() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        // 全屏启动图：直接显示用户提供的加载界面.png
        // 注意：windowBackground 已在 Splash 主题中设为 #E3F4EB（与图片顶部一致），
        // 图片未加载完成前不会出现白色闪烁。
        Image(
            painter = painterResource(id = R.drawable.splash_full),
            contentDescription = "启动页",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 短暂展示后切换到主界面
        LaunchedEffect(Unit) {
            delay(800L)
            showSplash = false
        }
    } else {
        MyPurseNavHost()
    }
}
