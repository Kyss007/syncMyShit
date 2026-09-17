package com.syncmyshit.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.syncmyshit.app.data.local.EmulatorRegistry
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.SaveFileItem
import com.syncmyshit.app.utils.FileHashUtils
import com.syncmyshit.app.utils.StorageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class ScannerRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    suspend fun discoverEmulatorsAndGames(): List<EmulatorProfile> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedPackages = runCatching {
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA).map { it.packageName }.toSet()
        }.getOrDefault(emptySet())

        val storageRoots = StorageAccessHelper.getStorageRoots(context)
        val customProfiles = preferencesManager.customProfiles.first()
        val allProfiles = EmulatorRegistry.BUILT_IN_PROFILES + customProfiles

        val discoveredList = mutableListOf<EmulatorProfile>()

        for (profile in allProfiles) {
            val isPackageInstalled = profile.packageNames.any { pkg ->
                installedPackages.contains(pkg) || installedPackages.any { it.startsWith(pkg) }
            }

            // Look for existing candidate save directories across all storage roots
            var foundPath: String? = null
            var fileCount = 0

            for (root in storageRoots) {
                for (candidate in profile.candidatePaths) {
                    val candidateFile = File(root, candidate)
                    if (candidateFile.exists() && candidateFile.canRead()) {
                        foundPath = candidateFile.absolutePath
                        fileCount = countSaveFiles(candidateFile, profile.fileExtensions)
                        break
                    }
                }
                if (foundPath != null) break
            }

            // Include if either package is installed, or the save folder exists, or it's custom
            if (foundPath != null || isPackageInstalled || profile.isCustom) {
                discoveredList.add(
                    profile.copy(
                        resolvedSavePath = foundPath,
                        fileCount = fileCount
                    )
                )
            }
        }

        discoveredList
    }

    suspend fun scanSaveFilesForProfile(profile: EmulatorProfile): List<SaveFileItem> = withContext(Dispatchers.IO) {
        val path = profile.resolvedSavePath ?: return@withContext emptyList()
        val rootDir = File(path)
        if (!rootDir.exists()) return@withContext emptyList()

        val saveFiles = mutableListOf<File>()
        collectFiles(rootDir, profile.fileExtensions, saveFiles)

        saveFiles.map { file ->
            val relative = file.relativeTo(rootDir).path
            SaveFileItem(
                id = "${profile.id}_$relative",
                emulatorId = profile.id,
                localAbsolutePath = file.absolutePath,
                relativePath = relative,
                fileName = file.name,
                localSizeBytes = file.length(),
                localLastModified = file.lastModified(),
                localSha256 = FileHashUtils.calculateSha256(file)
            )
        }
    }

    private fun countSaveFiles(dir: File, extensions: List<String>): Int {
        if (!dir.exists()) return 0
        var count = 0
        dir.walkTopDown().maxDepth(6).forEach { file ->
            if (file.isFile && matchesExtension(file.name, extensions)) {
                count++
            }
        }
        return count
    }

    private fun collectFiles(dir: File, extensions: List<String>, outList: MutableList<File>) {
        if (!dir.exists()) return
        dir.walkTopDown().maxDepth(6).forEach { file ->
            if (file.isFile && matchesExtension(file.name, extensions)) {
                outList.add(file)
            }
        }
    }

    private fun matchesExtension(fileName: String, extensions: List<String>): Boolean {
        if (extensions.isEmpty() || extensions.contains("*")) return true
        val lower = fileName.lowercase()
        return extensions.any { ext ->
            if (ext.endsWith("*")) {
                lower.contains(ext.removeSuffix("*").lowercase())
            } else {
                lower.endsWith(ext.lowercase())
            }
        }
    }
}
