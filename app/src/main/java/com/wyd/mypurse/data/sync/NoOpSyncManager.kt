package com.wyd.mypurse.data.sync

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步管理器空实现。V1.0 不执行任何同步操作。
 */
@Singleton
class NoOpSyncManager @Inject constructor() : SyncManager {
    override suspend fun sync(): Result<Unit> = Result.success(Unit)
}
