package com.syncmyshit.app.data.drive

data class DriveFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTimeMillis: Long,
    val sizeBytes: Long,
    val md5Checksum: String?,
    val parentId: String?
) {
    val isDirectory: Boolean
        get() = mimeType == "application/vnd.google-apps.folder"
}
