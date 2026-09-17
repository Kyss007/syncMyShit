package com.syncmyshit.app.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object FileHashUtils {

    fun calculateSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        return runCatching {
            FileInputStream(file).use { calculateStreamHash(it, "SHA-256") }
        }.getOrDefault("")
    }

    fun calculateMd5(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        return runCatching {
            FileInputStream(file).use { calculateStreamHash(it, "MD5") }
        }.getOrDefault("")
    }

    private fun calculateStreamHash(inputStream: InputStream, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
