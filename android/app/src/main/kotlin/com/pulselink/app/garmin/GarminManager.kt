package com.pulselink.app.garmin

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import com.pulselink.app.PulseLinkApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GarminManager(private val context: Context) {

    companion object {
        private const val TAG = "GarminManager"
    }

    enum class ConnectionState {
        DISCONNECTED,
        SDK_INITIALIZING,
        SDK_READY,
        DEVICE_CONNECTED,
        APP_READY,
        ERROR
    }

    private var connectIQ: ConnectIQ? = null
    private var device: IQDevice? = null
    private var watchApp: IQApp? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    fun initialize() {
        _connectionState.value = ConnectionState.SDK_INITIALIZING
        try {
            connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS)
            connectIQ?.initialize(context, true, object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    Log.d(TAG, "ConnectIQ SDK ready")
                    _connectionState.value = ConnectionState.SDK_READY
                    discoverDevices()
                }

                override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                    Log.e(TAG, "ConnectIQ init error: $status")
                    _connectionState.value = ConnectionState.ERROR
                }

                override fun onSdkShutDown() {
                    Log.d(TAG, "ConnectIQ SDK shut down")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ConnectIQ", e)
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private fun discoverDevices() {
        val iq = connectIQ ?: return
        try {
            val devices = iq.connectedDevices ?: iq.knownDevices ?: emptyList()
            if (devices.isEmpty()) {
                Log.w(TAG, "No Garmin devices found")
                return
            }

            device = devices[0]
            Log.d(TAG, "Found device: ${device?.friendlyName}")

            iq.registerForDeviceEvents(device) { dev, status ->
                Log.d(TAG, "Device ${dev.friendlyName} status: $status")
                if (status == IQDevice.IQDeviceStatus.CONNECTED) {
                    _connectionState.value = ConnectionState.DEVICE_CONNECTED
                    resolveApp(dev)
                } else {
                    _connectionState.value = ConnectionState.SDK_READY
                    watchApp = null
                }
            }

            // Use ConnectIQ.getDeviceStatus() instead of device.status (more reliable)
            val status = iq.getDeviceStatus(device)
            if (status == IQDevice.IQDeviceStatus.CONNECTED) {
                _connectionState.value = ConnectionState.DEVICE_CONNECTED
                resolveApp(device!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering devices", e)
        }
    }

    private fun resolveApp(device: IQDevice) {
        val iq = connectIQ ?: return
        try {
            iq.getApplicationInfo(
                PulseLinkApp.WATCH_APP_ID,
                device,
                object : ConnectIQ.IQApplicationInfoListener {
                    override fun onApplicationInfoReceived(app: IQApp) {
                        Log.d(TAG, "PulseLink watch app found")
                        watchApp = app
                        _connectionState.value = ConnectionState.APP_READY
                    }

                    override fun onApplicationNotInstalled(appId: String) {
                        Log.w(TAG, "PulseLink watch app not installed on device")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving watch app", e)
        }
    }

    fun sendAlarmSync(hour: Int, minute: Int) {
        val payload = mapOf(
            "type" to "alarm_sync",
            "hour" to hour,
            "minute" to minute
        )
        sendMessage(payload)
    }

    fun sendAlarmTrigger() {
        // Ensure the watch app is running before sending the trigger
        openWatchApp()
        sendMessage(mapOf("type" to "alarm_trigger"))
    }

    fun sendAlarmClear() {
        sendMessage(mapOf("type" to "alarm_clear"))
    }

    private fun sendMessage(payload: Map<String, Any>) {
        val iq = connectIQ ?: return
        val dev = device ?: return
        val app = watchApp ?: return

        try {
            iq.sendMessage(dev, app, payload, object : ConnectIQ.IQSendMessageListener {
                override fun onMessageStatus(
                    device: IQDevice,
                    app: IQApp,
                    status: ConnectIQ.IQMessageStatus
                ) {
                    Log.d(TAG, "sendMessage status: $status")
                }
            })
        } catch (e: InvalidStateException) {
            Log.e(TAG, "ConnectIQ SDK not initialized", e)
        } catch (e: ServiceUnavailableException) {
            Log.e(TAG, "Garmin Connect Mobile not available", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
        }
    }

    private fun openWatchApp() {
        val iq = connectIQ ?: return
        val dev = device ?: return
        val app = watchApp ?: return
        try {
            iq.openApplication(dev, app) { _, _, status ->
                Log.d(TAG, "openApplication status: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening watch app", e)
        }
    }

    fun shutdown() {
        try {
            connectIQ?.shutdown(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down ConnectIQ", e)
        }
        connectIQ = null
        device = null
        watchApp = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
