package com.syncmyshit.app.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.syncmyshit.app.SyncApplication
import com.syncmyshit.app.data.local.PreferencesManager
import com.syncmyshit.app.data.repository.ScannerRepository
import com.syncmyshit.app.data.repository.SyncRepository
import com.syncmyshit.app.utils.NotificationHelper
import com.syncmyshit.app.utils.StorageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EmulatorWatcherService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var syncRepository: SyncRepository
    private lateinit var scannerRepository: ScannerRepository
    private lateinit var preferencesManager: PreferencesManager

    private var lastForegroundPackage: String? = null
    private var isEmulatorActive = false

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val app = application as SyncApplication
        syncRepository = app.syncRepository
        scannerRepository = app.scannerRepository
        preferencesManager = app.preferencesManager

        NotificationHelper.createNotificationChannels(this)
        startForeground(
            NotificationHelper.NOTIFICATION_ID_SERVICE,
            NotificationHelper.buildForegroundServiceNotification(this, "Monitoring emulator activity")
        )

        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        scope.launch {
            while (isActive) {
                if (!preferencesManager.detectAppSwitch.first()) {
                    delay(5000)
                    continue
                }

                if (!StorageAccessHelper.hasUsageStatsPermission(this@EmulatorWatcherService)) {
                    delay(10000)
                    continue
                }

                val currentPkg = getForegroundPackageName()
                if (currentPkg != null && currentPkg != lastForegroundPackage) {
                    handlePackageTransition(previousPkg = lastForegroundPackage, currentPkg = currentPkg)
                    lastForegroundPackage = currentPkg
                }

                delay(2500) // Poll every 2.5s (very low battery footprint)
            }
        }
    }

    private suspend fun handlePackageTransition(previousPkg: String?, currentPkg: String) {
        val profiles = scannerRepository.discoverEmulatorsAndGames()
        val allEmulatorPackages = profiles.flatMap { it.packageNames }.toSet()

        // 1. Check if user just opened an emulator
        if (allEmulatorPackages.contains(currentPkg)) {
            isEmulatorActive = true
            // Pre-play check: download newer saves before game loads
            syncRepository.checkAndPullUpdatesForPackage(currentPkg)
        }
        // 2. Check if user just exited an emulator
        else if (previousPkg != null && allEmulatorPackages.contains(previousPkg)) {
            isEmulatorActive = false
            // Post-play check: sync saves to Google Drive automagically
            syncRepository.syncAfterExitForPackage(previousPkg)
        }
    }

    private fun getForegroundPackageName(): String? {
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 10000, time)
        val event = UsageEvents.Event()
        var lastPkg: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
