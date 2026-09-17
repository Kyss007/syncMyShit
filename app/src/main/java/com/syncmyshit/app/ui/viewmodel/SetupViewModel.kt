package com.syncmyshit.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.syncmyshit.app.SyncApplication
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.utils.StorageAccessHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SyncApplication
    private val preferencesManager = app.preferencesManager
    private val scannerRepository = app.scannerRepository
    private val authManager = app.authManager

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    val customOAuthClientId = preferencesManager.customOAuthClientId

    private val _discoveredEmulators = MutableStateFlow<List<EmulatorProfile>>(emptyList())
    val discoveredEmulators: StateFlow<List<EmulatorProfile>> = _discoveredEmulators.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        refreshPermissions()
        checkAuth()
    }

    val signedInEmail = preferencesManager.googleAccountEmail

    fun refreshPermissions() {
        _hasStoragePermission.value = StorageAccessHelper.hasAllFilesAccess()
        _hasUsagePermission.value = StorageAccessHelper.hasUsageStatsPermission(getApplication())
    }

    fun checkAuth() {
        _signedInAccount.value = authManager.currentAccount.value
    }

    fun onSignInSuccess(account: GoogleSignInAccount) {
        _signedInAccount.value = account
        _authErrorMessage.value = null
        viewModelScope.launch {
            preferencesManager.setGoogleAccount(account.email)
        }
    }

    fun onWebAuthSuccess(email: String?) {
        _authErrorMessage.value = null
        if (!email.isNullOrBlank()) {
            viewModelScope.launch {
                preferencesManager.setGoogleAccount(email)
            }
        }
    }

    fun startWebLogin(customClientId: String? = null, customClientSecret: String? = null) {
        viewModelScope.launch {
            val started = authManager.startWebLogin(customClientId, customClientSecret)
            if (!started) {
                _authErrorMessage.value = "Failed to launch browser for Web Login. Please check if a web browser is installed."
            }
        }
    }

    fun setAuthError(message: String?) {
        _authErrorMessage.value = message
    }

    fun updateCustomClientId(clientId: String) {
        viewModelScope.launch {
            preferencesManager.setCustomOAuthCredentials(clientId.trim(), "")
        }
    }

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
            if (_currentStep.value == 3) {
                scanEmulators()
            }
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun scanEmulators() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val list = scannerRepository.discoverEmulatorsAndGames()
                _discoveredEmulators.value = list
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun finishSetup(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setSetupCompleted(true)
            onDone()
        }
    }
}
