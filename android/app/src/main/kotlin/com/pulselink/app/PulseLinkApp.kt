package com.pulselink.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class PulseLinkApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pulselink_sync"

        // Must match the id in garmin/manifest.xml
        const val WATCH_APP_ID = "d3fc4b7e-831a-4e9b-b5f2-7c4e3a9d1b0f"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Alarm Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps alarm sync with Garmin watch active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
