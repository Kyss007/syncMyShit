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
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class ScannerRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    suspend fun discoverEmulatorsAndGames(): List<EmulatorProfile> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager

        // getInstalledPackages with GET_META_DATA can hang indefinitely on de-Googled ROMs
        // (GammaOS Nano, LineageOS without GApps, etc.). We cap it at 3 seconds.
        // If it times out, path-based discovery still finds everything on the filesystem.
        val installedPackages: Set<String> = withTimeoutOrNull(3_000L) {
            runCatching {
                packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                    .map { it.packageName }.toSet()
            }.getOrElse {
                // Fallback: lighter flag (no metadata), works on more ROM variants
                runCatching {
                    packageManager.getInstalledPackages(0)
                        .map { it.packageName }.toSet()
                }.getOrDefault(emptySet())
            }
        } ?: emptySet() // PM timed out — scan by path only

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
            "roms", "Roms", "ROMs", "Games", "games", "Emulation/saves", "Emulation/roms",
            "RetroArch/saves", "Saves", "saves", "drastic", "DraStic", "DraStic/backup", "drastic/backup"
        )
        for (root in roots) {
            for (sub in searchSubdirs) {
                val base = File(root, sub)
                if (base.exists() && base.isDirectory && base.canRead()) {
                    val count = countSaveFiles(base, extensions)
                    if (count > 0) return Pair(base.absolutePath, count)

                    // Check immediate children (e.g. Roms/nds, Roms/gba)
                    val children = base.listFiles() ?: continue
                    for (child in children) {
                        if (child.isDirectory && !child.name.startsWith(".") && child.name != "Android") {
                            val childCount = countSaveFiles(child, extensions)
                            if (childCount > 0) {
                                return Pair(child.absolutePath, childCount)
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
        alreadyDiscoveredPaths: MutableSet<String>
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
                    val children = base.listFiles() ?: continue
                    for (child in children) {
                        if (child.isDirectory && !child.name.startsWith(".") && child.name != "Android") {
                            val abs = child.absolutePath
                            if (!alreadyDiscoveredPaths.contains(abs)) {
                                val folderLower = child.name.lowercase()
                                val systemInfo = knownSystems[folderLower]
                                if (systemInfo != null) {
                                    val count = countSaveFiles(child, systemInfo.second)
                                    if (count > 0) {
                                        alreadyDiscoveredPaths.add(abs)
                                        additional.add(
                                            EmulatorProfile(
                                                id = "auto_${child.name.lowercase()}_${abs.hashCode()}",
                                                name = "${systemInfo.first} (${child.name})",
                                                system = systemInfo.first,
                                                category = ProfileCategory.EMULATOR,
                                                packageNames = emptyList(),
                                                candidatePaths = listOf(abs),
                                                resolvedSavePath = abs,
                                                fileExtensions = systemInfo.second,
                                                driveSubfolder = child.name,
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
        if (!dir.exists() || !dir.isDirectory) return 0
        if (extensions.isEmpty()) return 0 // No known extensions = don't sync
        var count = 0
        try {
            val files = dir.listFiles() ?: return 0
            for (file in files) {
                if (file.isFile && matchesExtension(file.name, extensions)) {
                    count++
                } else if (file.isDirectory && !file.name.startsWith(".")
                    && file.name != "Android"
                    && file.name != ".syncmyshit_backups"
                ) {
                    val subFiles = file.listFiles() ?: continue
                    for (subFile in subFiles) {
                        if (subFile.isFile && matchesExtension(subFile.name, extensions)) {
                            count++
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return count
    }

    private fun collectFiles(dir: File, extensions: List<String>, outList: MutableList<File>) {
        if (!dir.exists() || !dir.isDirectory) return
        if (extensions.isEmpty()) return // No known extensions = don't sync
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isFile && matchesExtension(file.name, extensions)) {
                    outList.add(file)
                } else if (file.isDirectory && !file.name.startsWith(".")
                    && file.name != "Android"
                    && file.name != ".syncmyshit_backups"
                ) {
                    val subFiles = file.listFiles() ?: continue
                    for (subFile in subFiles) {
                        if (subFile.isFile && matchesExtension(subFile.name, extensions)) {
                            outList.add(subFile)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun matchesExtension(fileName: String, extensions: List<String>): Boolean {
        if (extensions.isEmpty()) return false // Never sync if no extensions defined
        val lower = fileName.lowercase()
        return extensions.any { ext ->
            when {
                ext == "*" -> false // Never match wildcards blindly
                ext.contains('?') -> {
                    // Pattern like ".s??" → match exact length suffix (e.g. .s00, .s01, .sav)
                    val lowerExt = ext.lowercase()
                    if (lower.length < lowerExt.length) return@any false
                    val suffix = lower.takeLast(lowerExt.length)
                    suffix.zip(lowerExt).all { (a, b) -> b == '?' || a == b }
                }
                else -> lower.endsWith(ext.lowercase())
            }
        }
    }
}
