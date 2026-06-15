package com.wyd.mypurse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。标注 @HiltAndroidApp 触发 Hilt 依赖注入框架初始化。
 * 不在此处初始化数据库或执行任何耗时操作，遵循冷启动优化规范。
 */
@HiltAndroidApp
class MyPurseApplication : Application()
