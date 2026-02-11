package com.pulselink.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pulselink.app.MainActivity
import com.pulselink.app.PulseLinkApp
import com.pulselink.app.R
import com.pulselink.app.garmin.GarminManager
import com.pulselink.app.receiver.AlarmTriggerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class AlarmSyncService : Service() {

    companion object {
        private const val TAG = "AlarmSyncService"
        private const val NOTIFICATION_ID = 1
        private const val ALARM_REQUEST_CODE = 1001

        const val ACTION_SYNC = "com.pulselink.ACTION_SYNC"
        const val ACTION_TRIGGER = "com.pulselink.ACTION_TRIGGER"

        fun start(context: Context) {
            val intent = Intent(context, AlarmSyncService::class.java).apply {
                action = ACTION_SYNC
            }
            context.startForegroundService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        val service: AlarmSyncService get() = this@AlarmSyncService
    }

    private val binder = LocalBinder()

    lateinit var garminManager: GarminManager
        private set

    private val _nextAlarmTime = MutableStateFlow<Long?>(null)
    val nextAlarmTime: StateFlow<Long?> = _nextAlarmTime

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime

    override fun onCreate() {
        super.onCreate()
        garminManager = GarminManager(applicationContext)
        garminManager.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_TRIGGER -> triggerAlarm()
            else -> syncAlarm()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        garminManager.shutdown()
        super.onDestroy()
    }

    fun syncAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock

        if (nextAlarm != null) {
            val triggerTime = nextAlarm.triggerTime
            _nextAlarmTime.value = triggerTime
            _lastSyncTime.value = System.currentTimeMillis()

            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            Log.d(TAG, "Next alarm: %02d:%02d (epoch: $triggerTime)".format(hour, minute))

            // Send alarm time to Garmin watch
            garminManager.sendAlarmSync(hour, minute)

            // Schedule our own trigger at the same time
            scheduleTrigger(triggerTime)
        } else {
            Log.d(TAG, "No alarm scheduled on phone")
            _nextAlarmTime.value = null
            garminManager.sendAlarmClear()
            cancelTrigger()
        }
    }

    private fun triggerAlarm() {
        Log.d(TAG, "Alarm firing! Sending vibrate command to watch")
        garminManager.sendAlarmTrigger()
        _nextAlarmTime.value = null

        // Re-sync to pick up the next alarm (the one after the one that just fired)
        syncAlarm()
    }

    private fun scheduleTrigger(triggerTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmTriggerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            Log.d(TAG, "Trigger scheduled for $triggerTimeMillis")
        } else {
            Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
        }
    }

    private fun cancelTrigger() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmTriggerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PulseLinkApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("PulseLink")
            .setContentText("Syncing alarms with Garmin watch")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }
}
