package com.syncmyshit.app.data.repository

import android.content.Context
import com.syncmyshit.app.data.drive.DriveFileInfo
import com.syncmyshit.app.data.drive.GoogleDriveAuthManager
import com.syncmyshit.app.data.drive.GoogleDriveService
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.SyncAction
import com.syncmyshit.app.data.model.SyncLogEntry
import com.syncmyshit.app.data.model.SyncProgressState
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

/**
 * Handles all Google Drive sync operations.
 *
 * ## Multi-Device Sync Strategy
 *
 * Drive folder layout per emulator:
 * ```
 * syncMyShit/
 *   <emulator>/
 *     <save.sav>            ← canonical "newest across all devices" copy (used for download)
 *     _devices/
 *       <device-id>/
 *         <save.sav>        ← this device's latest upload
 *       <other-device-id>/
 *         <save.sav>        ← other device's latest upload
 *     _history/
 *       <save.sav>_<device-id>_<timestamp>.bak  ← every version ever uploaded, never deleted
 * ```
 *
 * ### Sync flow per save file (per device):
 * 1. Upload local file → `_devices/<device-id>/<save.sav>` (always, tracks this device's latest)
 * 2. Archive previous canonical to `_history/` before overwriting it
 * 3. Scan ALL `_devices/*/` subfolders → find the one with the newest `<save.sav>`
 * 4. If this device is newest → overwrite the canonical file with our version
 * 5. If another device is newest → download that device's version as the canonical file AND
 *    download it locally (with a local backup first), so this device is now up to date
 *
 * Result: **All saves are preserved** in `_history/`. **Newest always wins** and gets
 * pushed to every device on their next sync.
 */
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
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            preferencesManager.setDriveRootFolderId(rootFolder.id)

            val deviceId = preferencesManager.getOrCreateDeviceId()
            val profiles = scannerRepository.discoverEmulatorsAndGames().filter { it.isEnabled }

            for ((index, profile) in profiles.withIndex()) {
                NotificationHelper.showSyncNotification(
                    context,
                    "Syncing Saves",
                    "Checking ${profile.name} (${index + 1}/${profiles.size})",
                    progress = index,
                    maxProgress = profiles.size
                )

                val count = syncProfileInternal(profile, driveService, rootFolder.id, deviceId)
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
            val deviceId = preferencesManager.getOrCreateDeviceId()
            val count = syncProfileInternal(profile, driveService, rootFolder.id, deviceId)
            preferencesManager.updateLastSyncTimestamp()
            _syncProgress.value = SyncProgressState.Success(count)
            Result.success(count)
        } catch (e: Exception) {
            _syncProgress.value = SyncProgressState.Error(e.message ?: "Sync error")
            Result.failure(e)
        }
    }

    /**
     * Pre-play hook: pulls the newest save from Drive before the user starts playing.
     */
    suspend fun checkAndPullUpdatesForPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService() ?: return@withContext false
        val profiles = scannerRepository.discoverEmulatorsAndGames()
        val matchingProfile = profiles.firstOrNull { it.packageNames.contains(packageName) }
            ?: return@withContext false

        try {
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            val deviceId = preferencesManager.getOrCreateDeviceId()
            val count = syncProfileInternal(matchingProfile, driveService, rootFolder.id, deviceId)
            if (count > 0) {
                NotificationHelper.showSyncNotification(
                    context,
                    "${matchingProfile.name} Updated",
                    "Downloaded $count newer save files from Drive."
                )
            }
            true
        } catch (e: Exception) {
            addLog(matchingProfile.name, "pre-play", SyncAction.ERROR, e.message ?: "Failed pre-play check", false)
            false
        }
    }

    /**
     * Post-play hook: uploads any modified saves after the game exits.
     */
    suspend fun syncAfterExitForPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService() ?: return@withContext false
        val profiles = scannerRepository.discoverEmulatorsAndGames()
        val matchingProfile = profiles.firstOrNull { it.packageNames.contains(packageName) }
            ?: return@withContext false

        try {
            val rootFolder = driveService.getOrCreateRootFolder("syncMyShit").getOrThrow()
            val deviceId = preferencesManager.getOrCreateDeviceId()
            val count = syncProfileInternal(matchingProfile, driveService, rootFolder.id, deviceId)
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

    // ─────────────────────────────────────────────────────────────────────────
    // Core multi-device sync
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun syncProfileInternal(
        profile: EmulatorProfile,
        driveService: GoogleDriveService,
        rootFolderId: String,
        deviceId: String
    ): Int {
        val savePath = profile.resolvedSavePath ?: return 0
        val rootDir = File(savePath)
        if (!rootDir.exists()) return 0

        val localFiles = scannerRepository.scanSaveFilesForProfile(profile)
        if (localFiles.isEmpty()) return 0

        // Emulator root folder: syncMyShit/<emulator>/
        val emulatorFolder = driveService.getOrCreateSubfolder(profile.driveSubfolder, rootFolderId)
            .getOrNull() ?: return 0

        // Per-device uploads folder: syncMyShit/<emulator>/_devices/
        val devicesFolder = driveService.getOrCreateSubfolder("_devices", emulatorFolder.id)
            .getOrNull() ?: return 0

        // This device's subfolder: syncMyShit/<emulator>/_devices/<device-id>/
        val myDeviceFolder = driveService.getOrCreateSubfolder(deviceId, devicesFolder.id)
            .getOrNull() ?: return 0

        // History archive folder: syncMyShit/<emulator>/_history/
        val historyFolder = driveService.getOrCreateSubfolder("_history", emulatorFolder.id)
            .getOrNull()

        // Canonical files (newest-wins) at emulator root level
        val canonicalFiles = driveService.listFilesInFolder(emulatorFolder.id)
            .getOrDefault(emptyList())
            .filter { !it.isDirectory }
            .associateBy { it.name }

        var syncCount = 0

        for ((index, item) in localFiles.withIndex()) {
            _syncProgress.value = SyncProgressState.Syncing(
                currentFile = "${profile.name}: ${item.fileName}",
                currentStep = index + 1,
                totalSteps = localFiles.size,
                isUpload = true
            )

            val localFile = File(item.localAbsolutePath)
            if (!localFile.exists()) continue

            // ── Step 1: Upload this device's copy to _devices/<device-id>/<file> ──
            driveService.uploadFile(localFile, myDeviceFolder.id, item.fileName)

            // ── Step 2: Archive the current canonical to _history/ before changing it ──
            val currentCanonical = canonicalFiles[item.fileName]
            if (currentCanonical != null && historyFolder != null) {
                val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                archiveToHistory(driveService, currentCanonical, historyFolder.id, deviceId, dateStamp)
            }

            // ── Step 3: Find newest version across ALL device subfolders ──
            val (newestDeviceId, newestFile, newestTime) = findNewestAcrossDevices(
                driveService, devicesFolder.id, item.fileName, localFile, deviceId
            )

            // ── Step 4: Act on who has the newest ──
            val localTime = item.localLastModified
            val canonicalTime = currentCanonical?.modifiedTimeMillis ?: 0L

            if (newestDeviceId == deviceId || newestFile == null) {
                // This device is newest (or first) → push to canonical
                if (newestFile == null || localTime > canonicalTime + 2000L) {
                    driveService.uploadFile(localFile, emulatorFolder.id, item.fileName)
                    syncCount++
                    addLog(
                        profile.name, item.fileName, SyncAction.UPLOAD,
                        "Uploaded newest save from device $deviceId to canonical"
                    )
                }
            } else {
                // Another device has a newer save → download it locally
                if ((newestTime - localTime) > 2000L) {
                    // Back up local first so we never lose local progress
                    createLocalBackup(localFile, deviceId)
                    addLog(profile.name, item.fileName, SyncAction.BACKUP_CREATED, "Saved local rollback copy")

                    // Download the newest version from the winning device subfolder
                    val result = driveService.downloadFile(newestFile.id, localFile)
                    if (result.isSuccess) {
                        syncCount++
                        addLog(
                            profile.name, item.fileName, SyncAction.DOWNLOAD,
                            "Downloaded newer save from device $newestDeviceId"
                        )
                    }

                    // Also promote that device's version to canonical
                    driveService.uploadFile(localFile, emulatorFolder.id, item.fileName)
                    addLog(
                        profile.name, item.fileName, SyncAction.CONFLICT_RESOLVED,
                        "Promoted device $newestDeviceId save as canonical newest"
                    )
                } else {
                    // Timestamps are within 2s — upload local as canonical (tie → local wins)
                    driveService.uploadFile(localFile, emulatorFolder.id, item.fileName)
                    syncCount++
                    addLog(
                        profile.name, item.fileName, SyncAction.UPLOAD,
                        "Uploaded save (tie resolved, local to canonical)"
                    )
                }
            }
        }

        return syncCount
    }

    /**
     * Scans all `_devices/*/` subfolders for [fileName] and returns the device whose
     * copy has the latest modifiedTime. Includes this device's just-uploaded version
     * using [localFile] as the reference.
     *
     * Returns a Triple of (deviceId, DriveFileInfo?, modifiedTimeMillis).
     */
    private suspend fun findNewestAcrossDevices(
        driveService: GoogleDriveService,
        devicesFolderId: String,
        fileName: String,
        localFile: File,
        myDeviceId: String
    ): Triple<String, DriveFileInfo?, Long> {
        var newestDeviceId = myDeviceId
        var newestFile: DriveFileInfo? = null
        var newestTime = localFile.lastModified()

        val deviceSubfolders = driveService.listSubfolders(devicesFolderId).getOrDefault(emptyList())

        for (deviceFolder in deviceSubfolders) {
            if (deviceFolder.name == myDeviceId) continue // Already counted as newestTime

            val filesInFolder = driveService.listFilesInFolder(deviceFolder.id)
                .getOrDefault(emptyList())
            val remoteFile = filesInFolder.firstOrNull { it.name == fileName } ?: continue

            if (remoteFile.modifiedTimeMillis > newestTime + 2000L) {
                newestTime = remoteFile.modifiedTimeMillis
                newestDeviceId = deviceFolder.name
                newestFile = remoteFile
            }
        }

        return Triple(newestDeviceId, newestFile, newestTime)
    }

    /**
     * Copies a canonical Drive file into the `_history/` folder with a timestamped name.
     * This ensures every version of every save is archived and never deleted.
     */
    private suspend fun archiveToHistory(
        driveService: GoogleDriveService,
        file: DriveFileInfo,
        historyFolderId: String,
        deviceId: String,
        dateStamp: String
    ) {
        runCatching {
            driveService.createCloudBackup(
                fileId = file.id,
                backupFolderId = historyFolderId,
                originalName = "${file.name}_${deviceId}_$dateStamp"
            )
        }
    }

    /**
     * Creates a local timestamped backup of [file] before overwriting it with a newer
     * cloud version. Stored in `.syncmyshit_backups/` next to the save file.
     */
    private fun createLocalBackup(file: File, deviceId: String) {
        runCatching {
            val backupDir = File(file.parentFile, ".syncmyshit_backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "${file.name}.${deviceId}.$dateStamp.bak")
            file.copyTo(backupFile, overwrite = true)
        }
    }
}
