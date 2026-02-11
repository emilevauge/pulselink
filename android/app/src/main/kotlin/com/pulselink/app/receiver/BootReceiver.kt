package com.pulselink.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulselink.app.service.AlarmSyncService

/**
 * Re-starts alarm monitoring after device reboot.
 * Android clears all scheduled AlarmManager alarms on reboot,
 * so we need to re-read the next alarm and re-schedule our trigger.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmSyncService.start(context)
        }
    }
}
