package com.syncmyshit.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncAction {
    UPLOAD,
    DOWNLOAD,
    BACKUP_CREATED,
    CONFLICT_RESOLVED,
    SCAN_COMPLETED,
    ERROR
}

@Serializable
data class SyncLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emulatorName: String,
    val fileName: String,
    val action: SyncAction,
    val message: String,
    val isSuccess: Boolean = true
)
