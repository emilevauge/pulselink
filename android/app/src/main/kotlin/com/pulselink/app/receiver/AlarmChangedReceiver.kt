package com.pulselink.app.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulselink.app.service.AlarmSyncService

/**
 * Receives ACTION_NEXT_ALARM_CLOCK_CHANGED broadcasts.
 * This fires whenever the system's next alarm clock changes:
 * - new alarm set, existing alarm deleted/modified, or an alarm just went off.
 *
 * This broadcast is exempt from Android 8+ implicit broadcast restrictions.
 */
class AlarmChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) {
            AlarmSyncService.start(context)
        }
    }
}
