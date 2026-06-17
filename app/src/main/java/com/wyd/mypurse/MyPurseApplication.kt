package com.wyd.mypurse

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.wyd.mypurse.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException

/**
 * 应用入口。标注 @HiltAndroidApp 触发 Hilt 依赖注入框架初始化。
 * 不在此处初始化数据库或执行任何耗时操作，遵循冷启动优化规范。
 */
@HiltAndroidApp
class MyPurseApplication : Application() {
    override fun onCreate() {
        // StrictMode 仅在 debug 构建中启用，用于检测主线程违规操作
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

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
