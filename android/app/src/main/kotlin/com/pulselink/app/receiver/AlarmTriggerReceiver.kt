package com.pulselink.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulselink.app.service.AlarmSyncService

/**
 * Fires at the exact time of the user's phone alarm.
 * Tells the service to send the vibrate command to the Garmin watch.
 */
class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmSyncService::class.java).apply {
            action = AlarmSyncService.ACTION_TRIGGER
        }
        context.startForegroundService(serviceIntent)
    }
}
