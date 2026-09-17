package com.syncmyshit.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncmyshit.app.SyncApplication
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.SyncLogEntry
import com.syncmyshit.app.data.model.SyncProgressState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SyncApplication
    private val syncRepository = app.syncRepository
    private val scannerRepository = app.scannerRepository
    private val preferencesManager = app.preferencesManager
    private val authManager = app.authManager

    val syncProgress: StateFlow<SyncProgressState> = syncRepository.syncProgress
    val syncLogs: StateFlow<List<SyncLogEntry>> = syncRepository.syncLogs

    val googleAccountEmail = preferencesManager.googleAccountEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSyncTimestamp = preferencesManager.lastSyncTimestamp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val autoSyncEnabled = preferencesManager.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val detectAppSwitch = preferencesManager.detectAppSwitch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiOnly = preferencesManager.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncIntervalMinutes = preferencesManager.syncIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val keepBackupsCount = preferencesManager.keepBackupsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val customOAuthClientId = preferencesManager.customOAuthClientId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isSetupCompleted = preferencesManager.isSetupCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _emulators = MutableStateFlow<List<EmulatorProfile>>(emptyList())
    val emulators: StateFlow<List<EmulatorProfile>> = _emulators.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        refreshEmulators()
    }

    fun refreshEmulators() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val list = scannerRepository.discoverEmulatorsAndGames()
                _emulators.value = list
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun toggleProfile(profileId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = _emulators.value.map {
                if (it.id == profileId) it.copy(isEnabled = isEnabled) else it
            }
            _emulators.value = updated

            val customMatch = updated.firstOrNull { it.id == profileId && it.isCustom }
            if (customMatch != null) {
                preferencesManager.addCustomProfile(customMatch)
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            syncRepository.syncAllProfiles()
            refreshEmulators()
        }
    }

    fun syncProfile(profile: EmulatorProfile) {
        viewModelScope.launch {
            syncRepository.syncSingleProfile(profile)
            refreshEmulators()
        }
    }

    fun addCustomProfile(profile: EmulatorProfile) {
        viewModelScope.launch {
            preferencesManager.addCustomProfile(profile)
            refreshEmulators()
        }
    }

    fun removeCustomProfile(profileId: String) {
        viewModelScope.launch {
            preferencesManager.removeCustomProfile(profileId)
            refreshEmulators()
        }
    }

    fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoSyncEnabled(enabled) }
    }

    fun updateDetectAppSwitch(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDetectAppSwitch(enabled) }
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { preferencesManager.setWifiOnly(wifiOnly) }
    }

    fun updateSyncInterval(minutes: Int) {
        viewModelScope.launch { preferencesManager.setSyncIntervalMinutes(minutes) }
    }

    fun updateKeepBackupsCount(count: Int) {
        viewModelScope.launch { preferencesManager.setKeepBackupsCount(count) }
    }

    fun saveCustomOAuthCredentials(clientId: String, secret: String) {
        viewModelScope.launch {
            preferencesManager.setCustomOAuthCredentials(clientId, secret)
        }
    }

    fun signOut(onComplete: () -> Unit) {
        authManager.signOut {
            viewModelScope.launch {
                preferencesManager.setGoogleAccount(null)
                onComplete()
            }
        }
    }
}
