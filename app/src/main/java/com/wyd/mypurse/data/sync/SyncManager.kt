package com.wyd.mypurse.data.sync

/**
 * 同步管理器接口。V1.0 为空实现，为未来云同步预留。
 */
interface SyncManager {
    suspend fun sync(): Result<Unit>
}
