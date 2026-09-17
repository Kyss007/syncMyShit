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
     * Resolves all possible storage roots (Internal Storage + Removable MicroSD Cards)
     */
    fun getStorageRoots(context: Context): List<File> {
        val roots = mutableListOf<File>()
        
        // Internal storage root
        val internal = Environment.getExternalStorageDirectory()
        if (internal != null && internal.exists()) {
            roots.add(internal)
        }

        // External removable SD cards
        context.getExternalFilesDirs(null).forEach { dir ->
            if (dir != null) {
                // dir is usually: /storage/XXXX-XXXX/Android/data/com.syncmyshit.app/files
                val path = dir.absolutePath
                val markerIndex = path.indexOf("/Android/data/")
                if (markerIndex > 0) {
                    val rootPath = path.substring(0, markerIndex)
                    val rootFile = File(rootPath)
                    if (rootFile.exists() && rootFile.canRead() && !roots.contains(rootFile)) {
                        roots.add(rootFile)
                    }
                }
            }
        }

        // Secondary check in /storage/
        val storageRoot = File("/storage")
        if (storageRoot.exists() && storageRoot.isDirectory) {
            storageRoot.listFiles()?.forEach { file ->
                if (file.isDirectory && file.canRead() &&
                    file.name != "emulated" && file.name != "self" &&
                    !roots.contains(file)
                ) {
                    roots.add(file)
                }
            }
        }

        return roots
    }
}
