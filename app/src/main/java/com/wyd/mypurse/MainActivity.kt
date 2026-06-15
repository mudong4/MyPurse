package com.wyd.mypurse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wyd.mypurse.ui.navigation.MyPurseNavHost
import com.wyd.mypurse.ui.theme.MyPurseTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用唯一 Activity。使用 @AndroidEntryPoint 启用 Hilt 依赖注入。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyPurseTheme {
                MyPurseNavHost()
            }
        }
    }
}
