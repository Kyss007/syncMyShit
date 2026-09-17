package com.syncmyshit.app.data.model

sealed class SyncProgressState {
    data object Idle : SyncProgressState()
    data class Scanning(val message: String = "Scanning for save files…") : SyncProgressState()
    data class Syncing(
        val currentFile: String,
        val currentStep: Int,
        val totalSteps: Int,
        val isUpload: Boolean
    ) : SyncProgressState()
    data class Success(val filesSyncedCount: Int, val timestamp: Long = System.currentTimeMillis()) : SyncProgressState()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : SyncProgressState()
}
