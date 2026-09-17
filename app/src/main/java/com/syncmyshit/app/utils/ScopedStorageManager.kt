package com.syncmyshit.app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import rikka.shizuku.Shizuku
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object ScopedStorageManager {

    /**
     * Checks if Shizuku is installed and running on the handheld
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if syncMyShit has been granted Shizuku permission
     */
    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            try {
                if (Shizuku.isPreV11()) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } catch (e: Throwable) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Requests Shizuku permission if available
     */
    fun requestShizukuPermission(requestCode: Int = 1001) {
        if (isShizukuAvailable() && !hasShizukuPermission()) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Checks if a direct file path is accessible or blocked by Android 11+ scoped storage
     */
    fun isPathDirectlyAccessible(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.canRead()
            } else {
                file.parentFile?.canRead() == true
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Creates an Intent to prompt the user to grant access to an Android/data subdirectory via SAF
     */
    fun createSafFolderIntent(packageName: String): Intent {
        val path = "Android/data/$packageName"
        val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val authority = "com.android.externalstorage.documents"
            val documentId = "primary:$path"
            DocumentsContract.buildDocumentUri(authority, documentId)
        } else {
            null
        }

        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            if (initialUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
    }

    /**
     * Reads file bytes either directly, via DocumentFile, or via Shizuku rootless shell
     */
    fun readFileBytes(context: Context, targetFile: File, treeUri: Uri? = null): ByteArray? {
        // 1. Try standard File IO first
        if (targetFile.exists() && targetFile.canRead()) {
            return runCatching { targetFile.readBytes() }.getOrNull()
        }

        // 2. Try SAF DocumentFile if URI permission is available
        if (treeUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            val docFile = findChildDocument(rootDoc, targetFile.name)
            if (docFile != null && docFile.canRead()) {
                return runCatching {
                    context.contentResolver.openInputStream(docFile.uri)?.use { it.readBytes() }
                }.getOrNull()
            }
        }

        // 3. Try Shizuku rootless shell access if available
        if (hasShizukuPermission()) {
            return readViaShizuku(targetFile.absolutePath)
        }

        return null
    }

    /**
     * Writes file bytes either directly, via DocumentFile, or via Shizuku rootless shell
     */
    fun writeFileBytes(context: Context, targetFile: File, bytes: ByteArray, treeUri: Uri? = null): Boolean {
        // 1. Try standard File IO
        val directSuccess = runCatching {
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(bytes)
            true
        }.getOrDefault(false)

        if (directSuccess) return true

        // 2. Try SAF DocumentFile
        if (treeUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.canWrite()) {
                var docFile = findChildDocument(rootDoc, targetFile.name)
                if (docFile == null) {
                    docFile = rootDoc.createFile("application/octet-stream", targetFile.name)
                }
                if (docFile != null) {
                    return runCatching {
                        context.contentResolver.openOutputStream(docFile.uri)?.use { it.write(bytes) }
                        true
                    }.getOrDefault(false)
                }
            }
        }

        // 3. Try Shizuku rootless shell
        if (hasShizukuPermission()) {
            return writeViaShizuku(targetFile.absolutePath, bytes)
        }

        return false
    }

    private fun findChildDocument(parent: DocumentFile?, targetName: String): DocumentFile? {
        if (parent == null) return null
        return parent.listFiles().firstOrNull { it.name == targetName }
    }

    private fun invokeShizukuProcess(cmd: Array<String>): java.lang.Process? {
        return runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, cmd, null, null) as? java.lang.Process
        }.getOrNull()
    }

    private fun readViaShizuku(absolutePath: String): ByteArray? {
        return runCatching {
            val process = invokeShizukuProcess(arrayOf("sh", "-c", "cat \"$absolutePath\"")) ?: return null
            val bytes = process.inputStream.use { it.readBytes() }
            process.waitFor()
            if (process.exitValue() == 0) bytes else null
        }.getOrNull()
    }

    private fun writeViaShizuku(absolutePath: String, bytes: ByteArray): Boolean {
        return runCatching {
            val parentDir = File(absolutePath).parent ?: ""
            val mkdirProcess = invokeShizukuProcess(arrayOf("sh", "-c", "mkdir -p \"$parentDir\""))
            mkdirProcess?.waitFor()

            val process = invokeShizukuProcess(arrayOf("sh", "-c", "cat > \"$absolutePath\"")) ?: return false
            process.outputStream.use { it.write(bytes); it.flush() }
            process.waitFor()
            process.exitValue() == 0
        }.getOrDefault(false)
    }
}
