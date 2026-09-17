package com.syncmyshit.app.utils

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object RootAccessHelper {

    private const val TAG = "syncMyShit"
    private var isRootAvailableCache: Boolean? = null

    fun getSuBinary(): String {
        val paths = listOf("/sbin/su", "/system/xbin/su", "/system/bin/su", "/data/adb/magisk/su", "su")
        return paths.firstOrNull { runCatching { File(it).exists() }.getOrDefault(false) } ?: "su"
    }

    fun isRootAvailable(): Boolean {
        isRootAvailableCache?.let { return it }
        val suBin = getSuBinary()
        val available = try {
            val process = Runtime.getRuntime().exec(arrayOf(suBin, "-c", "id"))
            val finished = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(3000, TimeUnit.MILLISECONDS)
            } else {
                process.waitFor() == 0
            }
            if (finished && process.exitValue() == 0) true else {
                process.destroyForcibly()
                false
            }
        } catch (_: Exception) {
            false
        }
        isRootAvailableCache = available
        Log.d(TAG, "Root ($suBin) available: $available")
        return available
    }

    suspend fun ensurePathAccessible(path: String): Boolean = withContext(Dispatchers.IO) {
        if (!path.startsWith("/data/data/") && !path.startsWith("/data/user/0/")) {
            return@withContext true
        }

        val targetFile = File(path)
        if (targetFile.exists() && targetFile.canRead()) {
            return@withContext true
        }

        if (!isRootAvailable()) return@withContext false

        try {
            val pkgDir = if (path.startsWith("/data/data/")) {
                val parts = path.split("/")
                if (parts.size >= 4) "/data/data/${parts[3]}" else null
            } else null

            val cmd = buildString {
                if (pkgDir != null) {
                    append("chmod 755 $pkgDir; chmod 755 $pkgDir/files 2>/dev/null; ")
                }
                append("chmod -R a+rwX '$path' 2>/dev/null")
            }

            val suBin = getSuBinary()
            val process = Runtime.getRuntime().exec(arrayOf(suBin, "-c", cmd))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(3000, TimeUnit.MILLISECONDS)
            } else {
                process.waitFor()
            }

            val accessible = targetFile.exists() && targetFile.canRead()
            Log.d(TAG, "Ensured path accessibility via $suBin for '$path': accessible=$accessible")
            accessible
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unlock path via su: $path", e)
            false
        }
    }

    fun setupEmulatorMounts() {
        if (!isRootAvailable()) return
        val suBin = getSuBinary()
        try {
            val cmd = "mkdir -p /storage/emulated/0/DraStic && (mountpoint -q /storage/emulated/0/DraStic || mount -o bind /data/data/com.dsemu.drastic/files/DraStic /storage/emulated/0/DraStic) && chmod 755 /data/data/com.dsemu.drastic /data/data/com.dsemu.drastic/files && chmod -R a+rwX /data/data/com.dsemu.drastic/files/DraStic"
            Runtime.getRuntime().exec(arrayOf(suBin, "-c", cmd))
            Log.d(TAG, "DraStic bind mount ensured at /storage/emulated/0/DraStic")
        } catch (_: Exception) {}
    }
}
