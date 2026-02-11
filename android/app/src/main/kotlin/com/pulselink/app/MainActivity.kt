package com.pulselink.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pulselink.app.service.AlarmSyncService
import com.pulselink.app.ui.MainScreen
import com.pulselink.app.ui.theme.PulseLinkTheme

class MainActivity : ComponentActivity() {

    private var service: AlarmSyncService? = null
    private var serviceBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AlarmSyncService.LocalBinder).service
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            serviceBound = false
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless of result */ }

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-sync after returning from settings
        service?.syncAlarm()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        AlarmSyncService.start(this)

        setContent {
            PulseLinkTheme {
                MainScreen(
                    service = if (serviceBound) service else null,
                    onSyncClick = { service?.syncAlarm() },
                    onGrantExactAlarmClick = { openExactAlarmSettings() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, AlarmSyncService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
            service = null
        }
        super.onStop()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            exactAlarmPermissionLauncher.launch(intent)
        }
    }
}
