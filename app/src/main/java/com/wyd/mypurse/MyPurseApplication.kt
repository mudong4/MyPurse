package com.wyd.mypurse

import android.app.Application
import android.os.StrictMode
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException

/**
 * 应用入口。标注 @HiltAndroidApp 触发 Hilt 依赖注入框架初始化。
 * 不在此处初始化数据库或执行任何耗时操作，遵循冷启动优化规范。
 */
@HiltAndroidApp
class MyPurseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 设置全局未捕获异常处理器：静默处理 CancellationException，
        // 避免 Android 系统弹出 "StandaloneCoroutine was cancelled" 等调试 Toast
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            when (throwable) {
                is CancellationException -> {
                    // 协程取消是正常的程序行为，静默忽略
                    Log.d("MyPurse", "Coroutine cancelled: ${throwable.message}")
                }
                else -> originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
