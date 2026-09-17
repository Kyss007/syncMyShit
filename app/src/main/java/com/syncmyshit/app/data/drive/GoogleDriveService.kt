package com.syncmyshit.app.data.drive

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as GoogleDriveFile
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class GoogleDriveService(
    private val context: Context,
    private val authManager: GoogleDriveAuthManager
) {

    private suspend fun getDrive(): Drive {
        val token = authManager.getValidAccessToken()
            ?: throw IllegalStateException("Google Drive is not authenticated")

        if (token == "GMS_CREDENTIAL") {
            val account = authManager.currentAccount.value
                ?: throw IllegalStateException("GMS Account is missing")
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }
            return Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("syncMyShit").build()
        }

        // Universal Web OAuth token (works on all Android devices, GammaOS, de-Googled)
        val requestInitializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $token"
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer
        ).setApplicationName("syncMyShit").build()
    }

    suspend fun getOrCreateRootFolder(folderName: String = "syncMyShit"): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val q = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false and 'root' in parents"
            val result: FileList = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime)")
                .execute()

            val existing = result.files?.firstOrNull()
            if (existing != null) {
                DriveFileInfo(
                    id = existing.id,
                    name = existing.name,
                    mimeType = existing.mimeType,
                    modifiedTimeMillis = existing.modifiedTime?.value ?: 0L,
                    sizeBytes = 0L,
                    md5Checksum = null,
                    parentId = "root"
                )
            } else {
                val metadata = GoogleDriveFile().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                }
                val created = drive.files().create(metadata)
                    .setFields("id, name, mimeType, modifiedTime")
                    .execute()

                DriveFileInfo(
                    id = created.id,
                    name = created.name,
                    mimeType = created.mimeType,
                    modifiedTimeMillis = created.modifiedTime?.value ?: 0L,
                    sizeBytes = 0L,
                    md5Checksum = null,
                    parentId = "root"
                )
            }
        }
    }

    suspend fun getOrCreateSubfolder(folderName: String, parentFolderId: String): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val q = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false and '$parentFolderId' in parents"
            val result = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime)")
                .execute()

            val existing = result.files?.firstOrNull()
            if (existing != null) {
                DriveFileInfo(
                    id = existing.id,
                    name = existing.name,
                    mimeType = existing.mimeType,
                    modifiedTimeMillis = existing.modifiedTime?.value ?: 0L,
                    sizeBytes = 0L,
                    md5Checksum = null,
                    parentId = parentFolderId
                )
            } else {
                val metadata = GoogleDriveFile().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf(parentFolderId)
                }
                val created = drive.files().create(metadata)
                    .setFields("id, name, mimeType, modifiedTime")
                    .execute()

                DriveFileInfo(
                    id = created.id,
                    name = created.name,
                    mimeType = created.mimeType,
                    modifiedTimeMillis = created.modifiedTime?.value ?: 0L,
                    sizeBytes = 0L,
                    md5Checksum = null,
                    parentId = parentFolderId
                )
            }
        }
    }

    suspend fun listFilesInFolder(parentFolderId: String): Result<List<DriveFileInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val q = "'$parentFolderId' in parents and trashed = false"
            val result = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime, size, md5Checksum, parents)")
                .setPageSize(1000)
                .execute()

            result.files?.map { file ->
                DriveFileInfo(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    modifiedTimeMillis = file.modifiedTime?.value ?: 0L,
                    sizeBytes = file.getSize() ?: 0L,
                    md5Checksum = file.md5Checksum,
                    parentId = parentFolderId
                )
            } ?: emptyList()
        }
    }

    /** Lists only subfolders (not files) within a given Drive folder. */
    suspend fun listSubfolders(parentFolderId: String): Result<List<DriveFileInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val q = "'$parentFolderId' in parents and trashed = false and mimeType = 'application/vnd.google-apps.folder'"
            val result = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime)")
                .setPageSize(200)
                .execute()

            result.files?.map { file ->
                DriveFileInfo(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    modifiedTimeMillis = file.modifiedTime?.value ?: 0L,
                    sizeBytes = 0L,
                    md5Checksum = null,
                    parentId = parentFolderId
                )
            } ?: emptyList()
        }
    }

    suspend fun uploadFile(
        localFile: File,
        parentFolderId: String,
        targetFileName: String = localFile.name
    ): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val existingFiles = listFilesInFolder(parentFolderId).getOrDefault(emptyList())
            val existing = existingFiles.firstOrNull { it.name == targetFileName }

            val mediaContent = FileContent("application/octet-stream", localFile)

            val uploadedFile: GoogleDriveFile = if (existing != null) {
                val updateMetadata = GoogleDriveFile().apply {
                    name = targetFileName
                }
                drive.files().update(existing.id, updateMetadata, mediaContent)
                    .setFields("id, name, mimeType, modifiedTime, size, md5Checksum, parents")
                    .execute()
            } else {
                val createMetadata = GoogleDriveFile().apply {
                    name = targetFileName
                    parents = listOf(parentFolderId)
                }
                drive.files().create(createMetadata, mediaContent)
                    .setFields("id, name, mimeType, modifiedTime, size, md5Checksum, parents")
                    .execute()
            }

            DriveFileInfo(
                id = uploadedFile.id,
                name = uploadedFile.name,
                mimeType = uploadedFile.mimeType,
                modifiedTimeMillis = uploadedFile.modifiedTime?.value ?: System.currentTimeMillis(),
                sizeBytes = uploadedFile.getSize() ?: localFile.length(),
                md5Checksum = uploadedFile.md5Checksum,
                parentId = parentFolderId
            )
        }
    }

    suspend fun downloadFile(
        driveFileId: String,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.downloading")
            if (tempFile.exists()) tempFile.delete()
            
            destinationFile.parentFile?.mkdirs()

            FileOutputStream(tempFile).use { outputStream ->
                drive.files().get(driveFileId).executeMediaAndDownloadTo(outputStream)
            }

            if (destinationFile.exists()) destinationFile.delete()
            if (!tempFile.renameTo(destinationFile)) {
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }

            destinationFile
        }
    }

    suspend fun createCloudBackup(
        fileId: String,
        backupFolderId: String,
        originalName: String
    ): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = getDrive()
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupName = "${originalName}_$dateStamp.bak"

            val copiedMetadata = GoogleDriveFile().apply {
                name = backupName
                parents = listOf(backupFolderId)
            }

            val result = drive.files().copy(fileId, copiedMetadata)
                .setFields("id, name, mimeType, modifiedTime, size, md5Checksum, parents")
                .execute()

            DriveFileInfo(
                id = result.id,
                name = result.name,
                mimeType = result.mimeType,
                modifiedTimeMillis = result.modifiedTime?.value ?: System.currentTimeMillis(),
                sizeBytes = result.getSize() ?: 0L,
                md5Checksum = result.md5Checksum,
                parentId = backupFolderId
            )
        }
    }
}
