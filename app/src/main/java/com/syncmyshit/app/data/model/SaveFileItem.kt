package com.syncmyshit.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class FileSyncState {
    SYNCED,
    LOCAL_NEWER,
    CLOUD_NEWER,
    CONFLICT,
    LOCAL_ONLY,
    CLOUD_ONLY,
    ERROR
}

@Serializable
data class SaveFileItem(
    val id: String,
    val emulatorId: String,
    val localAbsolutePath: String,
    val relativePath: String,
    val fileName: String,
    val localSizeBytes: Long = 0L,
    val localLastModified: Long = 0L,
    val localSha256: String = "",
    val cloudFileId: String? = null,
    val cloudLastModified: Long? = null,
    val cloudSizeBytes: Long? = null,
    val cloudMd5Checksum: String? = null,
    val syncState: FileSyncState = FileSyncState.LOCAL_ONLY
)
