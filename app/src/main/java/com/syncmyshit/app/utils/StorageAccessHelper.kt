package com.syncmyshit.app.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import java.io.File

object StorageAccessHelper {

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun getAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Resolves all possible storage roots across all Android variants:
     * - Standard Android internal storage (/storage/emulated/0)
     * - Removable MicroSD cards via getExternalFilesDirs()
     * - GammaOS Nano / Anbernic / de-Googled ROMs: /mnt/media_rw/, /sdcard1/, etc.
     * - Legacy /mnt/sdcard, /sdcard paths
     *
     * We probe every known path directly so we never miss saves on custom firmware.
     */
    fun getStorageRoots(context: Context): List<File> {
        val roots = LinkedHashSet<File>() // dedup while preserving insertion order

        // 1. Standard Android internal storage
        runCatching {
            val internal = Environment.getExternalStorageDirectory()
            if (internal != null && internal.exists() && internal.canRead()) {
                roots.add(internal)
            }
        }

        // 2. /sdcard symlink (resolves to same as above on most devices, but catches edge cases)
        runCatching {
            val sdcard = File("/sdcard")
            if (sdcard.exists() && sdcard.canRead()) {
                roots.add(sdcard.canonicalFile)
            }
        }

        // 3. getExternalFilesDirs — picks up removable MicroSD on standard Android
        runCatching {
            context.getExternalFilesDirs(null).forEach { dir ->
                if (dir != null) {
                    val path = dir.absolutePath
                    val markerIndex = path.indexOf("/Android/data/")
                    if (markerIndex > 0) {
                        val rootFile = File(path.substring(0, markerIndex)).canonicalFile
                        if (rootFile.exists() && rootFile.canRead()) {
                            roots.add(rootFile)
                        }
                    }
                }
            }
        }

        // 4. Enumerate /storage/ — catches MicroSD cards on GammaOS / LineageOS / Anbernic
        runCatching {
            val storageDir = File("/storage")
            storageDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.canRead()
                    && file.name != "emulated"
                    && file.name != "self"
                ) {
                    roots.add(file.canonicalFile)
                }
            }
        }

        // 5. /mnt/media_rw/ — used by GammaOS Nano on the Anbernic RG DS for SD card
        runCatching {
            val mntrw = File("/mnt/media_rw")
            mntrw.listFiles()?.forEach { file ->
                if (file.isDirectory && file.canRead()) {
                    roots.add(file.canonicalFile)
                }
            }
        }

        // 6. Legacy / manufacturer-specific paths seen on Anbernic, Retroid, AYN, etc.
        val legacyPaths = listOf(
            "/mnt/sdcard",
            "/mnt/sdcard1",
            "/mnt/extsdcard",
            "/mnt/external_sd",
            "/mnt/emmc",
            "/sdcard1",
            "/sdcard2",
            "/storage/sdcard1",
            "/storage/extSdCard",
            "/storage/external_SD",
            "/storage/emulated/0",
            "/data/media/0",            // some de-Googled ROMs symlink here
        )
        for (path in legacyPaths) {
            runCatching {
                val f = File(path)
                if (f.exists() && f.canRead() && f.isDirectory) {
                    roots.add(f.canonicalFile)
                }
            }
        }

        return roots.toList()
    }
}
