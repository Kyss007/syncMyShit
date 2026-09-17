package com.syncmyshit.app.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleDriveAuthManager(private val context: Context) {

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    init {
        checkExistingSignIn()
    }

    fun checkExistingSignIn(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            _currentAccount.value = account
            return account
        }
        return null
    }

    fun getSignInClient(customClientId: String? = null): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )

        if (!customClientId.isNullOrBlank()) {
            gsoBuilder.requestIdToken(customClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getSignInIntent(customClientId: String? = null): Intent {
        return getSignInClient(customClientId).signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        _currentAccount.value = account
        return account != null
    }

    fun signOut(onComplete: () -> Unit) {
        val client = getSignInClient()
        client.signOut().addOnCompleteListener {
            _currentAccount.value = null
            onComplete()
        }
    }
}
