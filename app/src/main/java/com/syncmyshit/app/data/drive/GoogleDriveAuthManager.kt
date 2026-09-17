package com.syncmyshit.app.data.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.syncmyshit.app.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleDriveAuthManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    val webOAuth = WebOAuthManager(context)

    private val _currentAccountEmail = MutableStateFlow<String?>(null)
    val currentAccountEmail: StateFlow<String?> = _currentAccountEmail.asStateFlow()

    // For backwards-compatibility with existing screens observing currentAccount
    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            // Check stored email in preferences first
            val email = preferencesManager.googleAccountEmail.first()
            if (!email.isNullOrBlank()) {
                _currentAccountEmail.value = email
            } else {
                // Fallback to Google Play Services check
                checkExistingGmsSignIn()
            }
        }
    }

    private fun checkExistingGmsSignIn(): GoogleSignInAccount? {
        return runCatching {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
                _currentAccount.value = account
                _currentAccountEmail.value = account.email
                account
            } else null
        }.getOrNull()
    }

    fun checkExistingSignIn(): Boolean {
        return _currentAccountEmail.value != null || checkExistingGmsSignIn() != null
    }

    suspend fun startWebLogin(customClientId: String? = null, customClientSecret: String? = null): Result<Boolean> {
        val clientId = when {
            !customClientId.isNullOrBlank() -> customClientId.trim()
            else -> preferencesManager.customOAuthClientId.first().trim().ifBlank {
                WebOAuthManager.DEFAULT_CLIENT_ID
            }
        }

        val verifier = webOAuth.generateCodeVerifier()
        val challenge = webOAuth.generateCodeChallenge(verifier)
        val state = webOAuth.generateState()

        preferencesManager.saveOAuthPkceSession(verifier, state)
        if (!customClientId.isNullOrBlank()) {
            preferencesManager.setCustomOAuthCredentials(customClientId, customClientSecret ?: "")
        }

        val authUrl = webOAuth.buildAuthorizationUrl(clientId, challenge, state)
        val launched = webOAuth.launchAuthorizationInBrowser(context, authUrl)
        return if (launched) Result.success(true) else Result.failure(IllegalStateException("Failed to launch web browser. Please check if a browser is installed."))
    }

    suspend fun handleOAuthCallback(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val code = uri.getQueryParameter("code")
                ?: throw IllegalArgumentException("OAuth response missing authorization code: $uri")
            val state = uri.getQueryParameter("state")
            val savedState = preferencesManager.oauthState.first()

            if (savedState != null && state != savedState) {
                throw SecurityException("OAuth state mismatch (CSRF protection)")
            }

            val verifier = preferencesManager.oauthCodeVerifier.first()
                ?: throw IllegalStateException("OAuth verifier missing from session")

            val clientId = preferencesManager.customOAuthClientId.first().trim().ifBlank {
                WebOAuthManager.DEFAULT_CLIENT_ID
            }
            val clientSecret = preferencesManager.customOAuthClientSecret.first().ifBlank { null }

            val redirectUri = if (uri.scheme == "com.syncmyshit.app") {
                WebOAuthManager.REDIRECT_URI
            } else {
                "${uri.scheme}:${uri.path ?: "/oauth2redirect"}"
            }

            val tokens = webOAuth.exchangeCodeForTokens(code, verifier, clientId, clientSecret, redirectUri).getOrThrow()
            preferencesManager.saveOAuthTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresInSeconds)
            preferencesManager.clearOAuthPkceSession()

            // Fetch user's email
            val email = webOAuth.fetchUserEmail(tokens.accessToken).getOrDefault("Google Drive User")
            preferencesManager.setGoogleAccount(email)
            _currentAccountEmail.value = email

            email
        }
    }

    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        // 1. Check if we have an OAuth access token from Web Login
        val token = preferencesManager.oauthAccessToken.first()
        val refreshToken = preferencesManager.oauthRefreshToken.first()

        if (!token.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            // Attempt a refresh if close to expiry or return existing
            val clientId = preferencesManager.customOAuthClientId.first().trim().ifBlank {
                WebOAuthManager.DEFAULT_CLIENT_ID
            }
            val clientSecret = preferencesManager.customOAuthClientSecret.first().ifBlank { null }
            val refreshed = webOAuth.refreshAccessToken(refreshToken, clientId, clientSecret).getOrNull()
            if (refreshed != null) {
                preferencesManager.saveOAuthTokens(refreshed, refreshToken, 3600L)
                return@withContext refreshed
            }
            return@withContext token
        }

        // 2. Fallback to Google Play Services token if available
        val gmsAccount = _currentAccount.value ?: checkExistingGmsSignIn()
        if (gmsAccount != null) {
            // Handled via GoogleAccountCredential in GMS mode
            return@withContext "GMS_CREDENTIAL"
        }

        null
    }

    // Google Play Services client fallback
    fun getSignInClient(customClientId: String? = null): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )

        val activeClientId = customClientId?.ifBlank { null }
            ?: WebOAuthManager.DEFAULT_CLIENT_ID

        gsoBuilder.requestIdToken(activeClientId)

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getSignInIntent(customClientId: String? = null): Intent {
        return getSignInClient(customClientId).signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        _currentAccount.value = account
        if (account != null) {
            _currentAccountEmail.value = account.email
            scope.launch {
                preferencesManager.setGoogleAccount(account.email)
            }
            return true
        }
        return false
    }

    fun signOut(onComplete: () -> Unit = {}) {
        scope.launch {
            preferencesManager.clearOAuthTokens()
            _currentAccountEmail.value = null
            _currentAccount.value = null

            withContext(Dispatchers.Main) {
                runCatching {
                    val client = getSignInClient()
                    client.signOut().addOnCompleteListener {
                        onComplete()
                    }
                }.onFailure {
                    onComplete()
                }
            }
        }
    }
}
