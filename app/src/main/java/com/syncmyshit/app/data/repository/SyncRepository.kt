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
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "SyncRepository"

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
 * 3. Scan ALL `_devices/<dev>/` subfolders → find the one with the newest `<save.sav>`
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

    private fun encodeRemoteFileName(relativePath: String): String {
        return relativePath.replace(File.separatorChar, '/').replace("/", "___")
    }

    private fun decodeRemoteFileName(remoteFileName: String): String {
        return remoteFileName.replace("___", File.separator)
    }

    private fun resolveTargetLocalFile(rootDir: File, profileId: String, decodedRelPath: String): File {
        if (profileId == "drastic") {
            var clean = decodedRelPath.replace('\\', '/')
            while (clean.startsWith("backup/backup/")) {
                clean = clean.removePrefix("backup/")
            }
            while (clean.startsWith("savestates/savestates/")) {
                clean = clean.removePrefix("savestates/")
            }

            val ext = "." + clean.substringAfterLast('.', "").lowercase()
            val isBatterySave = ext in listOf(".dsv", ".sav")
            val isSaveState = ext in listOf(".dss", ".dst", ".state")

            if (!clean.contains('/')) {
                return if (isBatterySave) {
                    File(File(rootDir, "backup"), clean)
                } else if (isSaveState) {
                    File(File(rootDir, "savestates"), clean)
                } else {
                    File(rootDir, clean)
                }
            } else if (clean.startsWith("backup/") && isSaveState) {
                return File(File(rootDir, "savestates"), clean.removePrefix("backup/"))
            } else if (clean.startsWith("savestates/") && isBatterySave) {
                return File(File(rootDir, "backup"), clean.removePrefix("savestates/"))
            }
            return File(rootDir, clean)
        }
        return File(rootDir, decodedRelPath)
    }

    private fun migrateDrasticFolderIfNeeded(rootDir: File) {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        try {
            val backupDir = File(rootDir, "backup")
            val savestatesDir = File(rootDir, "savestates")
            backupDir.mkdirs()
            savestatesDir.mkdirs()

            // 1. Fix nested backup/backup created by earlier builds
            val nestedBackup = File(backupDir, "backup")
            if (nestedBackup.exists() && nestedBackup.isDirectory) {
                nestedBackup.listFiles()?.forEach { f ->
                    if (f.isFile) {
                        val target = File(backupDir, f.name)
                        if (!target.exists() || f.lastModified() > target.lastModified()) {
                            f.renameTo(target)
                        } else {
                            f.delete()
                        }
                    }
                }
                nestedBackup.delete()
            }

            // 2. Fix nested backup/savestates created by earlier builds
            val nestedSavestates = File(backupDir, "savestates")
            if (nestedSavestates.exists() && nestedSavestates.isDirectory) {
                nestedSavestates.listFiles()?.forEach { f ->
                    if (f.isFile) {
                        val target = File(savestatesDir, f.name)
                        if (!target.exists() || f.lastModified() > target.lastModified()) {
                            f.renameTo(target)
                        } else {
                            f.delete()
                        }
                    }
                }
                nestedSavestates.delete()
            }

            val stateExts = listOf(".dss", ".dst", ".state")
            val saveExts = listOf(".dsv", ".sav")

            // 3. Move misplaced save states (.dss, .dst, .state) from backup/ to savestates/
            backupDir.listFiles()?.forEach { f ->
                if (f.isFile && stateExts.any { f.name.endsWith(it, ignoreCase = true) }) {
                    val target = File(savestatesDir, f.name)
                    if (!target.exists() || f.lastModified() > target.lastModified()) {
                        f.renameTo(target)
                    } else {
                        f.delete()
                    }
                }
            }

            // 4. Move flat saves (.sav, .dsv) sitting directly in rootDir to backup/
            rootDir.listFiles()?.forEach { f ->
                if (f.isFile && saveExts.any { f.name.endsWith(it, ignoreCase = true) }) {
                    val target = File(backupDir, f.name)
                    if (!target.exists() || f.lastModified() > target.lastModified()) {
                        f.renameTo(target)
                    } else {
                        f.delete()
                    }
                }
            }

            // 5. Move flat states (.dss, .dst, .state) sitting directly in rootDir to savestates/
            rootDir.listFiles()?.forEach { f ->
                if (f.isFile && stateExts.any { f.name.endsWith(it, ignoreCase = true) }) {
                    val target = File(savestatesDir, f.name)
                    if (!target.exists() || f.lastModified() > target.lastModified()) {
                        f.renameTo(target)
                    } else {
                        f.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DraStic migration check error", e)
        }
    }

    private suspend fun syncProfileInternal(
        profile: EmulatorProfile,
        driveService: GoogleDriveService,
        rootFolderId: String,
        deviceId: String
    ): Int {
        val savePath = profile.resolvedSavePath ?: return 0
        val rootDir = File(savePath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        if (!rootDir.exists()) return 0

        if (profile.id == "drastic") {
            migrateDrasticFolderIfNeeded(rootDir)
        }

        val localFiles = scannerRepository.scanSaveFilesForProfile(profile)

        // Emulator root folder: syncMyShit/<emulator>/
        val emulatorFolder = driveService.getOrCreateSubfolder(profile.driveSubfolder, rootFolderId)
            .getOrNull() ?: return 0

        // Per-device uploads folder: syncMyShit/<emulator>/_devices/
        val devicesFolder = driveService.getOrCreateSubfolder("_devices", emulatorFolder.id)
            .getOrNull() ?: return 0

        // This device's subfolder: syncMyShit/<emulator>/_devices/<device-id>/
        val myDeviceFolder = driveService.getOrCreateSubfolder(deviceId, devicesFolder.id)
            .getOrNull() ?: return 0

        // Files already uploaded by this device
        val myDeviceFiles = driveService.listFilesInFolder(myDeviceFolder.id)
            .getOrDefault(emptyList())
            .filter { !it.isDirectory }
            .associateBy { it.name }

        // History archive folder: syncMyShit/<emulator>/_history/
        val historyFolder = driveService.getOrCreateSubfolder("_history", emulatorFolder.id)
            .getOrNull()

        // Canonical files (newest-wins) at emulator root level
        val canonicalFiles = driveService.listFilesInFolder(emulatorFolder.id)
            .getOrDefault(emptyList())
            .filter { !it.isDirectory }
            .associateBy { it.name }

        var syncCount = 0
        val processedRemoteNames = mutableSetOf<String>()
        val processedLocalPaths = mutableSetOf<String>()

        for ((index, item) in localFiles.withIndex()) {
            val remoteFileName = encodeRemoteFileName(item.relativePath)
            processedRemoteNames.add(remoteFileName)

            val localFile = File(item.localAbsolutePath)
            if (!localFile.exists()) continue
            processedLocalPaths.add(localFile.absolutePath)

            val localLength = localFile.length()
            val currentCanonical = canonicalFiles[remoteFileName]
            val myDeviceFile = myDeviceFiles[remoteFileName]

            // Calculate MD5 only when needed (lazy evaluation)
            var localMd5Cached: String? = null
            fun getLocalMd5(): String {
                if (localMd5Cached == null) {
                    localMd5Cached = com.syncmyshit.app.utils.FileHashUtils.calculateMd5(localFile)
                }
                return localMd5Cached!!
            }

            val matchesCanonical = currentCanonical != null &&
                currentCanonical.sizeBytes == localLength &&
                !currentCanonical.md5Checksum.isNullOrBlank() &&
                currentCanonical.md5Checksum.equals(getLocalMd5(), ignoreCase = true)

            val matchesMyDevice = myDeviceFile != null &&
                myDeviceFile.sizeBytes == localLength &&
                !myDeviceFile.md5Checksum.isNullOrBlank() &&
                myDeviceFile.md5Checksum.equals(getLocalMd5(), ignoreCase = true)

            // ── Change Detection ──
            // If the local file is already identical to canonical AND this device's copy is up-to-date:
            // NOTHING has changed! Skip completely!
            if (matchesCanonical && matchesMyDevice) {
                continue
            }

            _syncProgress.value = SyncProgressState.Syncing(
                currentFile = "${profile.name}: ${item.relativePath}",
                currentStep = index + 1,
                totalSteps = localFiles.size,
                isUpload = true
            )

            // If canonical already has this exact file, but our device subfolder is missing/outdated,
            // just update our device subfolder without touching canonical or history
            if (matchesCanonical && !matchesMyDevice) {
                driveService.uploadFile(localFile, myDeviceFolder.id, remoteFileName)
                continue
            }

            // If canonical doesn't have this file at all (brand new save)
            if (currentCanonical == null) {
                driveService.uploadFile(localFile, myDeviceFolder.id, remoteFileName)
                driveService.uploadFile(localFile, emulatorFolder.id, remoteFileName)
                syncCount++
                addLog(
                    profile.name, item.relativePath, SyncAction.UPLOAD,
                    "Uploaded new save to cloud"
                )
                continue
            }

            // Canonical exists and differs from local file!
            // Check across device subfolders to determine whether local or cloud is newer
            val (newestDeviceId, newestFile, _) = findNewestAcrossDevices(
                driveService, devicesFolder.id, remoteFileName, localFile, deviceId
            )

            if (newestDeviceId == deviceId || newestFile == null) {
                // Local device is newer → push to myDeviceFolder and canonical
                if (historyFolder != null) {
                    val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    archiveToHistory(driveService, currentCanonical, historyFolder.id, deviceId, dateStamp)
                }
                driveService.uploadFile(localFile, myDeviceFolder.id, remoteFileName)
                driveService.uploadFile(localFile, emulatorFolder.id, remoteFileName)
                syncCount++
                addLog(
                    profile.name, item.relativePath, SyncAction.UPLOAD,
                    "Uploaded newest save from device $deviceId to cloud"
                )
            } else {
                // Another device has a newer version!
                // Back up local first so we never lose local progress
                createLocalBackup(localFile, deviceId)
                addLog(profile.name, item.relativePath, SyncAction.BACKUP_CREATED, "Saved local rollback copy")

                // Download newest version
                val result = driveService.downloadFile(newestFile.id, localFile, newestFile.modifiedTimeMillis)
                if (result.isSuccess) {
                    syncCount++
                    addLog(
                        profile.name, item.relativePath, SyncAction.DOWNLOAD,
                        "Downloaded newer save from device $newestDeviceId"
                    )
                }

                // Update myDeviceFolder to have this latest version too
                driveService.uploadFile(localFile, myDeviceFolder.id, remoteFileName)

                // If canonical isn't already this newest file, promote it
                if (currentCanonical.id != newestFile.id) {
                    driveService.uploadFile(localFile, emulatorFolder.id, remoteFileName)
                    addLog(
                        profile.name, item.relativePath, SyncAction.CONFLICT_RESOLVED,
                        "Promoted device $newestDeviceId save as canonical newest"
                    )
                }
            }
        }

        // ── Step 5: Download any canonical files that don't exist locally yet ──
        for ((remoteName, canonicalFile) in canonicalFiles) {
            val relPath = decodeRemoteFileName(remoteName)
            val targetLocalFile = resolveTargetLocalFile(rootDir, profile.id, relPath)
            if (targetLocalFile.absolutePath in processedLocalPaths) continue
            if (remoteName in processedRemoteNames && targetLocalFile.exists()) continue

            if (!targetLocalFile.exists() || targetLocalFile.length() != canonicalFile.sizeBytes) {
                targetLocalFile.parentFile?.mkdirs()
                val result = driveService.downloadFile(canonicalFile.id, targetLocalFile, canonicalFile.modifiedTimeMillis)
                if (result.isSuccess) {
                    syncCount++
                    processedLocalPaths.add(targetLocalFile.absolutePath)
                    val loggedPath = runCatching { targetLocalFile.relativeTo(rootDir).path }.getOrDefault(targetLocalFile.name)
                    addLog(
                        profile.name, loggedPath, SyncAction.DOWNLOAD,
                        "Downloaded new save from cloud"
                    )
                }
            }
        }

        return syncCount
    }

    /**
     * Scans all `_devices/<dev>/` subfolders for [fileName] and returns the device whose
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
