package com.syncmyshit.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.syncmyshit.app.data.local.EmulatorRegistry
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.ProfileCategory
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
        val resolvedPaths = mutableSetOf<String>()

        for (profile in allProfiles) {
            val isPackageInstalled = profile.packageNames.any { pkg ->
                installedPackages.contains(pkg) || installedPackages.any { it.startsWith(pkg) }
            }

            // 1. Look for existing candidate save directories across all storage roots
            var bestPath: String? = null
            var bestCount = 0

            for (root in storageRoots) {
                for (candidate in profile.candidatePaths) {
                    val candidateFile = if (candidate.startsWith("/")) File(candidate) else File(root, candidate)
                    if (candidateFile.exists() && candidateFile.canRead()) {
                        val count = countSaveFiles(candidateFile, profile.fileExtensions)
                        if (count > bestCount) {
                            bestCount = count
                            bestPath = candidateFile.absolutePath
                        } else if (bestPath == null) {
                            bestPath = candidateFile.absolutePath
                            bestCount = count
                        }
                    }
                }
            }

            // 2. Deep fallback: If no candidate path had save files, search for directories with matching extensions
            if (bestCount == 0 && profile.fileExtensions.isNotEmpty() && !profile.isCustom) {
                val deepMatch = findDirectoryWithExtensions(storageRoots, profile.fileExtensions)
                if (deepMatch != null) {
                    bestPath = deepMatch.first
                    bestCount = deepMatch.second
                }
            }

            val foundPath = bestPath
            val fileCount = bestCount

            // Include if either package is installed, or the save folder exists, or it's custom
            if (foundPath != null || isPackageInstalled || profile.isCustom) {
                if (foundPath != null) {
                    resolvedPaths.add(foundPath)
                }
                discoveredList.add(
                    profile.copy(
                        resolvedSavePath = foundPath,
                        fileCount = fileCount
                    )
                )
            }
        }

        // 3. Auto-discover any other emulator/system folders containing save files on internal/SD storage
        val autoDiscovered = autoDiscoverUnregisteredSaveFolders(storageRoots, resolvedPaths)
        discoveredList.addAll(autoDiscovered)

        discoveredList
    }

    private fun findDirectoryWithExtensions(roots: List<File>, extensions: List<String>): Pair<String, Int>? {
        val searchSubdirs = listOf(
            "", "roms", "Roms", "ROMs", "Games", "games", "Emulation", "emulation",
            "RetroArch", "retroarch", "DraStic", "drastic", "Saves", "saves",
            "ES-DE", "Daijisho", "RetroDeck"
        )
        for (root in roots) {
            for (sub in searchSubdirs) {
                val base = if (sub.isEmpty()) root else File(root, sub)
                if (base.exists() && base.isDirectory && base.canRead()) {
                    base.walkTopDown().maxDepth(3).forEach { dir ->
                        if (dir.isDirectory && dir.name != "Android" && !dir.name.startsWith(".")) {
                            val count = countSaveFiles(dir, extensions)
                            if (count > 0) {
                                return Pair(dir.absolutePath, count)
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun autoDiscoverUnregisteredSaveFolders(
        storageRoots: List<File>,
        alreadyDiscoveredPaths: Set<String>
    ): List<EmulatorProfile> {
        val additional = mutableListOf<EmulatorProfile>()
        val searchFolders = listOf(
            "roms", "Roms", "ROMs", "Games", "games", "Emulation/saves", "Emulation/roms",
            "RetroArch/saves", "Saves", "saves", "drastic", "DraStic"
        )

        val knownSystems = mapOf(
            "nds" to Pair("Nintendo DS", listOf(".dsv", ".dss", ".sav")),
            "ds" to Pair("Nintendo DS", listOf(".dsv", ".dss", ".sav")),
            "gba" to Pair("Game Boy Advance", listOf(".sav", ".srm", ".state")),
            "gbc" to Pair("Game Boy Color", listOf(".sav", ".srm")),
            "gb" to Pair("Game Boy", listOf(".sav", ".srm")),
            "snes" to Pair("Super Nintendo", listOf(".srm", ".state")),
            "sfc" to Pair("Super Famicom", listOf(".srm", ".state")),
            "nes" to Pair("NES / Famicom", listOf(".srm", ".state")),
            "n64" to Pair("Nintendo 64", listOf(".srm", ".mpk", ".fla", ".eep")),
            "psx" to Pair("Sony PlayStation", listOf(".mcd", ".mcr", ".srm", ".sav")),
            "ps1" to Pair("Sony PlayStation", listOf(".mcd", ".mcr", ".srm", ".sav")),
            "ps2" to Pair("Sony PlayStation 2", listOf(".ps2", ".mcd")),
            "psp" to Pair("Sony PSP", listOf(".bin", ".sfo", ".ppst")),
            "megadrive" to Pair("Sega Genesis", listOf(".srm", ".state")),
            "genesis" to Pair("Sega Genesis", listOf(".srm", ".state")),
            "dreamcast" to Pair("Sega Dreamcast", listOf(".bin", ".vmu", ".state")),
            "dc" to Pair("Sega Dreamcast", listOf(".bin", ".vmu", ".state"))
        )

        for (root in storageRoots) {
            for (searchBase in searchFolders) {
                val base = File(root, searchBase)
                if (base.exists() && base.isDirectory && base.canRead()) {
                    base.walkTopDown().maxDepth(3).forEach { dir ->
                        if (dir.isDirectory && !dir.name.startsWith(".") && dir.name != "Android") {
                            val abs = dir.absolutePath
                            if (!alreadyDiscoveredPaths.contains(abs)) {
                                val folderLower = dir.name.lowercase()
                                val systemInfo = knownSystems[folderLower]
                                if (systemInfo != null) {
                                    val count = countSaveFiles(dir, systemInfo.second)
                                    if (count > 0) {
                                        additional.add(
                                            EmulatorProfile(
                                                id = "auto_${dir.name.lowercase()}_${abs.hashCode()}",
                                                name = "${systemInfo.first} (${dir.name})",
                                                system = systemInfo.first,
                                                category = ProfileCategory.EMULATOR,
                                                packageNames = emptyList(),
                                                candidatePaths = listOf(abs),
                                                resolvedSavePath = abs,
                                                fileExtensions = systemInfo.second,
                                                driveSubfolder = dir.name,
                                                fileCount = count,
                                                isEnabled = true
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return additional
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
