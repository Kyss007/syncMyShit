package com.syncmyshit.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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

    private val tag = "syncMyShit"

    suspend fun discoverEmulatorsAndGames(): List<EmulatorProfile> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val packageManager = context.packageManager

        // getInstalledPackages with GET_META_DATA can hang on de-Googled ROMs (GammaOS Nano, etc.).
        // We cap it at 3 seconds and fallback to lighter flags if needed.
        val installedPackages: Set<String> = withTimeoutOrNull(3_000L) {
            runCatching {
                packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
            }.getOrElse {
                emptySet()
            }
        } ?: emptySet()

        Log.d(tag, "Installed packages queried (${installedPackages.size} found)")
        com.syncmyshit.app.utils.RootAccessHelper.setupEmulatorMounts()

        val storageRoots = StorageAccessHelper.getStorageRoots(context)
        val customProfiles = preferencesManager.customProfiles.first()
        val allProfiles = EmulatorRegistry.BUILT_IN_PROFILES + customProfiles

        val discoveredList = mutableListOf<EmulatorProfile>()
        val resolvedPaths = mutableSetOf<String>()

        for (profile in allProfiles) {
            val isPackageInstalled = profile.packageNames.any { pkg ->
                installedPackages.contains(pkg) || installedPackages.any { it.startsWith(pkg) }
            }

            // Look for existing candidate save directories across all storage roots
            var bestPath: String? = null
            var bestCount = 0
            var firstExistingCandidate: String? = null

            for (candidate in profile.candidatePaths) {
                if (candidate.startsWith("/")) {
                    com.syncmyshit.app.utils.RootAccessHelper.ensurePathAccessible(candidate)
                    val candidateFile = File(candidate)
                    val exists = candidateFile.exists()
                    val canRead = candidateFile.canRead()
                    Log.d(tag, "[${profile.id}] Candidate '$candidate' -> exists=$exists, canRead=$canRead")
                    if (exists && canRead) {
                        if (firstExistingCandidate == null) {
                            firstExistingCandidate = candidateFile.absolutePath
                        }
                        val count = countSaveFiles(candidateFile, profile.fileExtensions)
                        if (count > bestCount) {
                            bestCount = count
                            bestPath = candidateFile.absolutePath
                        } else if (bestPath == null) {
                            bestPath = candidateFile.absolutePath
                            bestCount = count
                        }
                    }
                    continue
                }

                for (root in storageRoots) {
                    val candidateFile = File(root, candidate)
                    if (candidateFile.exists() && candidateFile.canRead()) {
                        if (firstExistingCandidate == null) {
                            firstExistingCandidate = candidateFile.absolutePath
                        }
                        val count = countSaveFiles(candidateFile, profile.fileExtensions)
                        Log.d(tag, "[${profile.id}] Candidate '${candidateFile.absolutePath}' -> exists=true, saves=$count")
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

            val defaultPath = if (storageRoots.isNotEmpty() && profile.candidatePaths.isNotEmpty()) {
                val firstCandidate = profile.candidatePaths.first()
                if (firstCandidate.startsWith("/")) firstCandidate else File(storageRoots.first(), firstCandidate).absolutePath
            } else null
            val rawFinalPath = bestPath ?: firstExistingCandidate ?: if (isPackageInstalled) defaultPath else null

            var finalPath = rawFinalPath
            var fileCount = bestCount

            if (profile.id == "drastic" && finalPath != null) {
                // If path pointed to DraStic/backup or DraStic/savestates, normalize to root DraStic directory
                val candidateDir = File(finalPath)
                if (candidateDir.name.equals("backup", ignoreCase = true) || candidateDir.name.equals("savestates", ignoreCase = true)) {
                    val parent = candidateDir.parentFile
                    if (parent != null && (parent.name.equals("drastic", ignoreCase = true) || parent.name.equals("DraStic", ignoreCase = true))) {
                        finalPath = parent.absolutePath
                    }
                }
                finalPath?.let { p ->
                    try {
                        val rootF = File(p)
                        if (rootF.exists() || isPackageInstalled) {
                            File(rootF, "backup").mkdirs()
                            File(rootF, "savestates").mkdirs()
                        }
                        if (rootF.exists()) {
                            fileCount = countSaveFiles(rootF, profile.fileExtensions)
                        }
                    } catch (_: Exception) {}
                }
            } else if (profile.id == "duckstation" && finalPath != null) {
                val candidateDir = File(finalPath)
                if (candidateDir.name.equals("memcards", ignoreCase = true) || candidateDir.name.equals("savestates", ignoreCase = true)) {
                    val parent = candidateDir.parentFile
                    if (parent != null && parent.name.equals("duckstation", ignoreCase = true)) {
                        finalPath = parent.absolutePath
                        fileCount = countSaveFiles(parent, profile.fileExtensions)
                    }
                }
            }

            // Include if either package is installed, or the save folder exists, or it's custom
            if (finalPath != null || isPackageInstalled || profile.isCustom) {
                if (finalPath != null) {
                    resolvedPaths.add(finalPath)
                }
                discoveredList.add(
                    profile.copy(
                        resolvedSavePath = finalPath,
                        fileCount = fileCount
                    )
                )
                Log.d(tag, "Discovered profile '${profile.name}': pkgInstalled=$isPackageInstalled, path=$finalPath, saves=$fileCount")
            }
        }

        // Auto-discover any unregistered system folders containing save files (direct O(1) checks)
        val coveredSystems = discoveredList.map { it.system.lowercase() }.toSet()
        val autoDiscovered = autoDiscoverUnregisteredSaveFolders(storageRoots, resolvedPaths, coveredSystems)
        discoveredList.addAll(autoDiscovered)

        val duration = System.currentTimeMillis() - startTime
        Log.i(tag, "Save file discovery finished in ${duration}ms! Found ${discoveredList.size} systems.")

        discoveredList
    }

    /**
     * Fast, direct-lookup auto-discovery for systems not covered by registered apps.
     * Uses direct path checks rather than recursive directory tree walking.
     */
    private fun autoDiscoverUnregisteredSaveFolders(
        storageRoots: List<File>,
        alreadyDiscoveredPaths: MutableSet<String>,
        coveredSystems: Set<String>
    ): List<EmulatorProfile> {
        val additional = mutableListOf<EmulatorProfile>()
        val baseFolders = listOf("roms", "Roms", "RetroArch/saves", "Saves", "saves", "Emulation/saves", "EmuData")
        val checkedDirs = mutableSetOf<String>()

        val knownSystems = listOf(
            Triple("nds", "Nintendo DS", listOf(".dsv", ".dss", ".sav")),
            Triple("gba", "Game Boy Advance", listOf(".sav", ".srm", ".state")),
            Triple("gbc", "Game Boy Color", listOf(".sav", ".srm")),
            Triple("gb", "Game Boy", listOf(".sav", ".srm")),
            Triple("snes", "Super Nintendo", listOf(".srm", ".state")),
            Triple("sfc", "Super Famicom", listOf(".srm", ".state")),
            Triple("nes", "NES / Famicom", listOf(".srm", ".state")),
            Triple("n64", "Nintendo 64", listOf(".srm", ".mpk", ".fla", ".eep")),
            Triple("psx", "Sony PlayStation", listOf(".mcd", ".mcr", ".srm", ".sav")),
            Triple("ps1", "Sony PlayStation", listOf(".mcd", ".mcr", ".srm", ".sav")),
            Triple("ps2", "Sony PlayStation 2", listOf(".ps2", ".mcd")),
            Triple("psp", "Sony PSP", listOf(".bin", ".sfo", ".ppst", ".dat")),
            Triple("megadrive", "Sega Genesis", listOf(".srm", ".state")),
            Triple("genesis", "Sega Genesis", listOf(".srm", ".state")),
            Triple("dreamcast", "Sega Dreamcast", listOf(".bin", ".vmu", ".state"))
        )

        for (root in storageRoots) {
            for (baseName in baseFolders) {
                val base = File(root, baseName)
                if (!base.exists() || !base.isDirectory) continue
                val baseCanonical = runCatching { base.canonicalPath }.getOrDefault(base.absolutePath)
                if (checkedDirs.contains(baseCanonical)) continue
                checkedDirs.add(baseCanonical)

                for ((sysDir, sysName, extensions) in knownSystems) {
                    if (coveredSystems.contains(sysName.lowercase())) continue

                    val candidate = File(base, sysDir)
                    if (!candidate.exists() || !candidate.isDirectory || !candidate.canRead()) continue
                    val abs = runCatching { candidate.canonicalPath }.getOrDefault(candidate.absolutePath)
                    if (alreadyDiscoveredPaths.contains(abs)) continue

                    val count = countSaveFiles(candidate, extensions)
                    if (count > 0) {
                        alreadyDiscoveredPaths.add(abs)
                        additional.add(
                            EmulatorProfile(
                                id = "auto_${candidate.name.lowercase()}_${abs.hashCode()}",
                                name = "$sysName (${candidate.name})",
                                system = sysName,
                                category = ProfileCategory.EMULATOR,
                                packageNames = emptyList(),
                                candidatePaths = listOf(abs),
                                resolvedSavePath = abs,
                                fileExtensions = extensions,
                                driveSubfolder = candidate.name,
                                fileCount = count,
                                isEnabled = true
                            )
                        )
                        Log.d(tag, "Auto-discovered save folder: $abs with $count saves")
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

    companion object {
        private val SKIPPED_DIR_NAMES = setOf(
            "android", "cache", "dump", "shaders", "resourcepacks", "logs", "log",
            "gpu", "vk_graphics_pipeline_cache", "graphics_pipeline_cache",
            "crash_dumps", "crashdumps", "screenshots", "themes", "theme", "cheats",
            "amiibo", "dcim", "pictures", "music", "movies", "ringtones", "alarms",
            "notifications", "podcasts", "audiobooks", "download", "documents",
            "system", ".syncmyshit_backups"
        )
    }

    private fun shouldTraverseDirectory(name: String): Boolean {
        if (name.startsWith(".")) return false
        return name.lowercase() !in SKIPPED_DIR_NAMES
    }

    /**
     * Counts matching save files in [dir] without creating unnecessary File objects.
     * Fast and bounded up to depth 7 — will never hang regardless of directory size.
     */
    private fun countSaveFiles(
        dir: File,
        extensions: List<String>,
        currentDepth: Int = 0,
        maxDepth: Int = 7
    ): Int {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return 0
        if (extensions.isEmpty()) return 0

        var count = 0
        try {
            val files = dir.listFiles() ?: return 0
            for (file in files) {
                if (file.isFile) {
                    if (matchesExtension(file.name, extensions)) {
                        count++
                    }
                } else if (file.isDirectory) {
                    if (shouldTraverseDirectory(file.name)) {
                        count += countSaveFiles(file, extensions, currentDepth + 1, maxDepth)
                    }
                }
            }
        } catch (_: Exception) {}
        return count
    }

    private fun collectFiles(
        dir: File,
        extensions: List<String>,
        outList: MutableList<File>,
        currentDepth: Int = 0,
        maxDepth: Int = 7
    ) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return
        if (extensions.isEmpty()) return
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isFile) {
                    if (matchesExtension(file.name, extensions)) {
                        outList.add(file)
                    }
                } else if (file.isDirectory) {
                    if (shouldTraverseDirectory(file.name)) {
                        collectFiles(file, extensions, outList, currentDepth + 1, maxDepth)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun matchesExtension(fileName: String, extensions: List<String>): Boolean {
        if (extensions.isEmpty()) return false
        for (i in 0 until extensions.size) {
            val ext = extensions[i]
            if (ext == "*") continue
            if (ext.contains('?')) {
                if (fileName.length >= ext.length) {
                    val start = fileName.length - ext.length
                    var match = true
                    for (j in 0 until ext.length) {
                        val ec = ext[j]
                        if (ec != '?' && !ec.equals(fileName[start + j], ignoreCase = true)) {
                            match = false
                            break
                        }
                    }
                    if (match) return true
                }
            } else {
                if (fileName.endsWith(ext, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
