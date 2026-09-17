package com.syncmyshit.app.data.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val tokenType: String
)

class WebOAuthManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder().build()
    private val secureRandom = SecureRandom()

    companion object {
        const val DEFAULT_CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"
        const val REDIRECT_URI = "com.syncmyshit.app:/oauth2redirect"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
    }

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateState(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun buildAuthorizationUrl(
        clientId: String = DEFAULT_CLIENT_ID,
        codeChallenge: String,
        state: String,
        redirectUri: String = REDIRECT_URI
    ): String {
        val effectiveClientId = clientId.trim().ifBlank { DEFAULT_CLIENT_ID }
        val scopes = "$SCOPE_DRIVE_FILE $SCOPE_USER_EMAIL"
        return Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", effectiveClientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("prompt", "select_account")
            .appendQueryParameter("access_type", "offline")
            .build()
            .toString()
    }

    fun launchAuthorizationInBrowser(context: Context, authUrl: String): Boolean {
        return runCatching {
            val uri = Uri.parse(authUrl)
            try {
                val customTabs = CustomTabsIntent.Builder().build()
                customTabs.launchUrl(context, uri)
                true
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                true
            }
        }.getOrDefault(false)
    }

    suspend fun exchangeCodeForTokens(
        code: String,
        codeVerifier: String,
        clientId: String = DEFAULT_CLIENT_ID,
        clientSecret: String? = null,
        redirectUri: String = REDIRECT_URI
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val effectiveClientId = clientId.trim().ifBlank { DEFAULT_CLIENT_ID }
            val formBuilder = FormBody.Builder()
                .add("client_id", effectiveClientId)
                .add("code", code.trim())
                .add("code_verifier", codeVerifier.trim())
                .add("grant_type", "authorization_code")
                .add("redirect_uri", redirectUri)

            if (!clientSecret.isNullOrBlank()) {
                formBuilder.add("client_secret", clientSecret.trim())
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = runCatching {
                    val json = JSONObject(body)
                    json.optString("error_description", json.optString("error", "Token exchange failed ($body)"))
                }.getOrDefault("HTTP ${response.code}: $body")
                throw RuntimeException("Google Token Error: $errorMsg")
            }

            val json = JSONObject(body)
            OAuthTokenResponse(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresInSeconds = json.optLong("expires_in", 3600L),
                tokenType = json.optString("token_type", "Bearer")
            )
        }
    }

    suspend fun refreshAccessToken(
        refreshToken: String,
        clientId: String = DEFAULT_CLIENT_ID,
        clientSecret: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val effectiveClientId = clientId.trim().ifBlank { DEFAULT_CLIENT_ID }
            val formBuilder = FormBody.Builder()
                .add("client_id", effectiveClientId)
                .add("refresh_token", refreshToken.trim())
                .add("grant_type", "refresh_token")

            if (!clientSecret.isNullOrBlank()) {
                formBuilder.add("client_secret", clientSecret.trim())
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw RuntimeException("Token refresh failed: HTTP ${response.code} $body")
            }

            val json = JSONObject(body)
            json.getString("access_token")
        }
    }

    suspend fun fetchUserEmail(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(USERINFO_ENDPOINT)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw RuntimeException("User info failed: HTTP ${response.code}")
            }

            val json = JSONObject(body)
            json.getString("email")
        }
    }
}
