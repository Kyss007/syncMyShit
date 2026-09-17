package com.syncmyshit.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object SigningUtils {

    const val FIXED_SHA1 = "03:16:B2:C3:98:4E:4C:C4:8C:29:3C:49:B6:07:2E:54:F0:D0:A2:82"

    fun getAppSha1(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            val packageName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return FIXED_SHA1
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        }.getOrDefault(FIXED_SHA1)
    }
}
