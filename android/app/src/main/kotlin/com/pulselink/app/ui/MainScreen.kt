package com.pulselink.app.ui

import android.app.AlarmManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulselink.app.garmin.GarminManager
import com.pulselink.app.service.AlarmSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    service: AlarmSyncService?,
    onSyncClick: () -> Unit,
    onGrantExactAlarmClick: () -> Unit
) {
    val connectionState by (service?.garminManager?.connectionState
        ?: MutableStateFlow(GarminManager.ConnectionState.DISCONNECTED))
        .collectAsState()

    val nextAlarmTime by (service?.nextAlarmTime
        ?: MutableStateFlow(null))
        .collectAsState()

    val lastSyncTime by (service?.lastSyncTime
        ?: MutableStateFlow(null))
        .collectAsState()

    val context = LocalContext.current
    val canScheduleExact = canScheduleExactAlarms(context)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "PulseLink",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Android \u2192 Garmin Alarm Sync",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Garmin connection status
            StatusCard(
                title = "Garmin Watch",
                status = connectionState.displayName(),
                isOk = connectionState == GarminManager.ConnectionState.APP_READY
            )

            // Next alarm
            AlarmCard(nextAlarmTime = nextAlarmTime)

            // Exact alarm permission warning
            if (!canScheduleExact) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Exact alarm permission required",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onGrantExactAlarmClick) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Last sync
            if (lastSyncTime != null) {
                Text(
                    text = "Last sync: ${formatTimestamp(lastSyncTime!!)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sync button
            Button(
                onClick = onSyncClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = service != null
            ) {
                Text("Sync Now", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(title: String, status: String, isOk: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status,
                fontSize = 14.sp,
                color = if (isOk)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlarmCard(nextAlarmTime: Long?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Next Alarm",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (nextAlarmTime != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = nextAlarmTime }
                Text(
                    text = "%02d:%02d".format(
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                    ),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatDate(nextAlarmTime),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No alarm set",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun GarminManager.ConnectionState.displayName(): String = when (this) {
    GarminManager.ConnectionState.DISCONNECTED -> "Disconnected"
    GarminManager.ConnectionState.SDK_INITIALIZING -> "Initializing..."
    GarminManager.ConnectionState.SDK_READY -> "Searching..."
    GarminManager.ConnectionState.DEVICE_CONNECTED -> "Watch found"
    GarminManager.ConnectionState.APP_READY -> "Connected"
    GarminManager.ConnectionState.ERROR -> "Error"
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
        return true // exact alarms are always allowed before Android 12
    }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun formatTimestamp(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatDate(timeMillis: Long): String {
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timeMillis))
}
