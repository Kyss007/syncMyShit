package com.syncmyshit.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.syncmyshit.app.SyncApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? SyncApplication ?: return
            val prefs = app.preferencesManager

            CoroutineScope(Dispatchers.IO).launch {
                val autoSync = prefs.autoSyncEnabled.first()
                val detectApp = prefs.detectAppSwitch.first()
                val interval = prefs.syncIntervalMinutes.first()
                val wifiOnly = prefs.wifiOnly.first()

                if (autoSync && detectApp) {
                    val serviceIntent = Intent(context, EmulatorWatcherService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }

                if (autoSync) {
                    SyncWorkManagerHelper.schedulePeriodicSync(context, interval.toLong(), wifiOnly)
                }
            }
        }
    }
}
