package com.syncmyshit.app.data.repository

import android.content.Context
import com.syncmyshit.app.data.drive.GoogleDriveAuthManager
import com.syncmyshit.app.data.drive.GoogleDriveService
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.FileSyncState
import com.syncmyshit.app.data.model.SaveFileItem
import com.syncmyshit.app.data.model.SyncAction
import com.syncmyshit.app.data.model.SyncLogEntry
import com.syncmyshit.app.data.model.SyncProgressState
import com.syncmyshit.app.utils.FileHashUtils
import com.syncmyshit.app.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SyncRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val scannerRepository: ScannerRepository,
    private val authManager: GoogleDriveAuthManager
) {

    private val _syncProgress = MutableStateFlow<SyncProgressState>(SyncProgressState.Idle)
    val syncProgress: StateFlow<SyncProgressState> = _syncProgress.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val syncLogs: StateFlow<List<SyncLogEntry>> = _syncLogs.asStateFlow()

    private fun addLog(
        emulatorName: String,
        fileName: String,
        action: SyncAction,
        message: String,
        isSuccess: Boolean = true
    ) {
        val entry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            emulatorName = emulatorName,
            fileName = fileName,
            action = action,
            message = message,
            isSuccess = isSuccess
        )
        _syncLogs.value = (listOf(entry) + _syncLogs.value).take(100)
    }

    private fun getDriveService(): GoogleDriveService? {
        if (!authManager.checkExistingSignIn()) return null
        return GoogleDriveService(context, authManager)
    }

    suspend fun syncAllProfiles(): Result<Int> = withContext(Dispatchers.IO) {
        val driveService = getDriveService()
            ?: return@withContext Result.failure(IllegalStateException("Google Drive not connected"))

        _syncProgress.value = SyncProgressState.Scanning("Scanning profiles & connecting to Drive…")

        var totalFilesSynced = 0
        try {
            val rootResult = driveService.getOrCreateRootFolder("syncMyShit")
            val rootFolder = rootResult.getOrThrow()
            preferencesManager.setDriveRootFolderId(rootFolder.id)

            // Ensure remote _backups folder
            val backupFolderResult = driveService.getOrCreateSubfolder("_backups", rootFolder.id)
            val backupFolderId = backupFolderResult.getOrNull()?.id

            val profiles = scannerRepository.discoverEmulatorsAndGames().filter { it.isEnabled }

            for ((index, profile) in profiles.withIndex()) {
                NotificationHelper.showSyncNotification(
                    context,
                    "Syncing Saves",
                    "Checking ${profile.name} (${index + 1}/${profiles.size})",
                    progress = index,
                    maxProgress = profiles.size
                )

                val count = syncProfileInternal(profile, driveService, rootFolder.id, backupFolderId)
                totalFilesSynced += count
            }

            preferencesManager.updateLastSyncTimestamp()
            _syncProgress.value = SyncProgressState.Success(totalFilesSynced)
            NotificationHelper.showSyncNotification(
                context,
                "Sync Complete",
                "Automagically synced $totalFilesSynced save files."
            )
            Result.success(totalFilesSynced)
        } catch (e: Exception) {
            _syncProgress.value = SyncProgressState.Error(e.message ?: "Unknown sync error")
            NotificationHelper.showSyncNotification(
                context,
                "Sync Failed",
                e.message ?: "Error connecting to Google Drive"
            )
            Result.failure(e)
        }
    }

    suspend fun syncSingleProfile(profile: EmulatorProfile): Result<Int> = withContext(Dispatchers.IO) {
        val driveService = getDriveService()
            ?: return@withContext Result.failure(IllegalStateException("Google Drive not connected"))

        _syncProgress.value = SyncProgressState.Scanning("Syncing ${profile.name}…")

        try {
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            val backupFolderId = driveService.getOrCreateSubfolder("_backups", rootFolder.id).getOrNull()?.id

            val count = syncProfileInternal(profile, driveService, rootFolder.id, backupFolderId)
            preferencesManager.updateLastSyncTimestamp()
            _syncProgress.value = SyncProgressState.Success(count)
            Result.success(count)
        } catch (e: Exception) {
            _syncProgress.value = SyncProgressState.Error(e.message ?: "Sync error")
            Result.failure(e)
        }
    }

    /**
     * Pre-play hook: Checks if Google Drive has newer save files before the user starts playing!
     */
    suspend fun checkAndPullUpdatesForPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService() ?: return@withContext false
        val profiles = scannerRepository.discoverEmulatorsAndGames()
        val matchingProfile = profiles.firstOrNull { it.packageNames.contains(packageName) } ?: return@withContext false

        try {
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            val backupFolderId = driveService.getOrCreateSubfolder("_backups", rootFolder.id).getOrNull()?.id
            val subfolder = driveService.getOrCreateSubfolder(matchingProfile.driveSubfolder, rootFolder.id).getOrThrow()

            val cloudFiles = driveService.listFilesInFolder(subfolder.id).getOrDefault(emptyList())
            val localFiles = scannerRepository.scanSaveFilesForProfile(matchingProfile)

            var pulledCount = 0
            val resolvedPath = matchingProfile.resolvedSavePath ?: return@withContext false
            val saveRootDir = File(resolvedPath)

            for (cloudFile in cloudFiles) {
                if (cloudFile.isDirectory) continue
                val localMatch = localFiles.firstOrNull { it.fileName == cloudFile.name }

                if (localMatch == null || cloudFile.modifiedTimeMillis > localMatch.localLastModified + 2000L) {
                    // Cloud has a newer version! Download before playing.
                    val destFile = if (localMatch != null) {
                        File(localMatch.localAbsolutePath)
                    } else {
                        File(saveRootDir, cloudFile.name)
                    }

                    // Create local backup first to never lose progress
                    if (destFile.exists()) {
                        createLocalBackup(destFile)
                    }

                    driveService.downloadFile(cloudFile.id, destFile)
                    pulledCount++
                    addLog(
                        matchingProfile.name,
                        cloudFile.name,
                        SyncAction.DOWNLOAD,
                        "Updated from cloud before game launch"
                    )
                }
            }

            if (pulledCount > 0) {
                NotificationHelper.showSyncNotification(
                    context,
                    "${matchingProfile.name} Updated",
                    "Downloaded $pulledCount newer save files from Drive."
                )
            }
            true
        } catch (e: Exception) {
            addLog(matchingProfile.name, "pre-play", SyncAction.ERROR, e.message ?: "Failed pre-play check", false)
            false
        }
    }

    /**
     * Post-play hook: Automatically uploads any modified saves after exiting the game!
     */
    suspend fun syncAfterExitForPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService() ?: return@withContext false
        val profiles = scannerRepository.discoverEmulatorsAndGames()
        val matchingProfile = profiles.firstOrNull { it.packageNames.contains(packageName) } ?: return@withContext false

        try {
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            val backupFolderId = driveService.getOrCreateSubfolder("_backups", rootFolder.id).getOrNull()?.id
            val count = syncProfileInternal(matchingProfile, driveService, rootFolder.id, backupFolderId)
            
            if (count > 0) {
                NotificationHelper.showSyncNotification(
                    context,
                    "${matchingProfile.name} Synced",
                    "Automagically backed up $count save files to Google Drive."
                )
            }
            true
        } catch (e: Exception) {
            addLog(matchingProfile.name, "post-play", SyncAction.ERROR, e.message ?: "Failed post-play sync", false)
            false
        }
    }

    private suspend fun syncProfileInternal(
        profile: EmulatorProfile,
        driveService: GoogleDriveService,
        rootFolderId: String,
        backupFolderId: String?
    ): Int {
        val savePath = profile.resolvedSavePath ?: return 0
        val rootDir = File(savePath)
        if (!rootDir.exists()) return 0

        val localFiles = scannerRepository.scanSaveFilesForProfile(profile)
        if (localFiles.isEmpty()) return 0

        val subfolderResult = driveService.getOrCreateSubfolder(profile.driveSubfolder, rootFolderId)
        val subfolder = subfolderResult.getOrNull() ?: return 0

        val remoteFiles = driveService.listFilesInFolder(subfolder.id).getOrDefault(emptyList())
        val remoteMap = remoteFiles.filter { !it.isDirectory }.associateBy { it.name }

        var syncCount = 0

        for ((index, item) in localFiles.withIndex()) {
            _syncProgress.value = SyncProgressState.Syncing(
                currentFile = "${profile.name}: ${item.fileName}",
                currentStep = index + 1,
                totalSteps = localFiles.size,
                isUpload = true
            )

            val remoteMatch = remoteMap[item.fileName]
            val localFile = File(item.localAbsolutePath)

            if (remoteMatch == null) {
                // Cloud doesn't have it -> Upload
                val uploaded = driveService.uploadFile(localFile, subfolder.id, item.fileName)
                if (uploaded.isSuccess) {
                    syncCount++
                    addLog(profile.name, item.fileName, SyncAction.UPLOAD, "Uploaded new save to Drive")
                }
            } else {
                // Compare modified times & size
                val timeDiff = item.localLastModified - remoteMatch.modifiedTimeMillis
                val isDifferent = Math.abs(timeDiff) > 2000L || item.localSizeBytes != remoteMatch.sizeBytes

                if (isDifferent) {
                    if (timeDiff > 2000L) {
                        // Local is newer -> Upload (Backup remote copy if configured)
                        if (backupFolderId != null) {
                            driveService.createCloudBackup(remoteMatch.id, backupFolderId, remoteMatch.name)
                            addLog(profile.name, remoteMatch.name, SyncAction.BACKUP_CREATED, "Created cloud snapshot before update")
                        }
                        val uploaded = driveService.uploadFile(localFile, subfolder.id, item.fileName)
                        if (uploaded.isSuccess) {
                            syncCount++
                            addLog(profile.name, item.fileName, SyncAction.UPLOAD, "Updated save in Google Drive")
                        }
                    } else if (timeDiff < -2000L) {
                        // Remote is newer -> Download to local (Create local backup first!)
                        createLocalBackup(localFile)
                        addLog(profile.name, item.fileName, SyncAction.BACKUP_CREATED, "Saved local rollback copy")
                        
                        val downloaded = driveService.downloadFile(remoteMatch.id, localFile)
                        if (downloaded.isSuccess) {
                            syncCount++
                            addLog(profile.name, item.fileName, SyncAction.DOWNLOAD, "Downloaded newer save from Drive")
                        }
                    }
                }
            }
        }

        return syncCount
    }

    private fun createLocalBackup(file: File) {
        runCatching {
            val backupDir = File(file.parentFile, ".syncmyshit_backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "${file.name}.$dateStamp.bak")
            file.copyTo(backupFile, overwrite = true)
        }
    }
}
