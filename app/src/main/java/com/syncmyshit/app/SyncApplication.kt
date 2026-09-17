package com.syncmyshit.app

import android.app.Application
import com.syncmyshit.app.data.drive.GoogleDriveAuthManager
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.repository.ScannerRepository
import com.syncmyshit.app.data.repository.SyncRepository
import com.syncmyshit.app.utils.NotificationHelper

class SyncApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var scannerRepository: ScannerRepository
        private set

    lateinit var authManager: GoogleDriveAuthManager
        private set

    lateinit var syncRepository: SyncRepository
        private set

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannels(this)

        preferencesManager = PreferencesManager(this)
        authManager = GoogleDriveAuthManager(this, preferencesManager)
        scannerRepository = ScannerRepository(this, preferencesManager)
        syncRepository = SyncRepository(this, preferencesManager, scannerRepository, authManager)

        com.syncmyshit.app.utils.RootAccessHelper.setupEmulatorMounts()
    }
}
