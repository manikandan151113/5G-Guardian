package com.fivegguardian

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkMonitorService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_MONITOR = "fiveg_guardian_monitoring"
        const val CHANNEL_ALERT = "fiveg_guardian_alert"

        const val ACTION_START_MONITORING = "com.fivegguardian.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.fivegguardian.ACTION_STOP_MONITORING"
        const val ACTION_STOP_ALARM = "com.fivegguardian.ACTION_STOP_ALARM"
        const val ACTION_TRIGGER_TEST_ALARM = "com.fivegguardian.ACTION_TRIGGER_TEST_ALARM"
    }

    private lateinit var alarmPlayer: AlarmPlayer
    private var telephonyCallback: Any? = null
    private var legacyPhoneStateListener: LegacyPhoneStateListener? = null

    private var previousNetworkType: NetworkType? = null
    private var alarmTriggeredForCurrentFallback = false
    private var isTestingAlarm = false
    private var alarmSilencedByCall = false

    override fun onCreate() {
        super.onCreate()
        Log.d("NetworkMonitorService", "onCreate")
        NetworkMonitorManager.initPreferences(this)
        alarmPlayer = AlarmPlayer(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("NetworkMonitorService", "onStartCommand: action=$action")

        if (action != ACTION_STOP_MONITORING) {
            val initialText = when (action) {
                ACTION_TRIGGER_TEST_ALARM -> "Loud testing alarm is triggered."
                else -> "Monitoring active. Waiting for signal updates..."
            }
            val notification = buildNormalNotification(initialText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        when (action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            ACTION_STOP_ALARM -> {
                stopAlarmOnly()
            }
            ACTION_TRIGGER_TEST_ALARM -> {
                triggerTestAlarm()
            }
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        Log.d("NetworkMonitorService", "startMonitoring")
        NetworkMonitorManager.updateMonitoringStatus(true)
        
        val notification = buildNormalNotification("Monitoring active. Waiting for signal updates...")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        NetworkMonitorManager.addLog("[$timestamp] ▶️ Monitoring started.")

        registerTelephonyListeners()
        evaluateAlarmState()
    }

    private fun stopMonitoring() {
        Log.d("NetworkMonitorService", "stopMonitoring")
        unregisterTelephonyListeners()
        alarmPlayer.stop()
        isTestingAlarm = false
        alarmTriggeredForCurrentFallback = false
        alarmSilencedByCall = false
        NetworkMonitorManager.updateMonitoringStatus(false)
        NetworkMonitorManager.updateAlarmPlaying(false)
        
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        NetworkMonitorManager.addLog("[$timestamp] ⏹️ Monitoring stopped.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun stopAlarmOnly() {
        Log.d("NetworkMonitorService", "stopAlarmOnly")
        alarmPlayer.stop()
        isTestingAlarm = false
        alarmSilencedByCall = false
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        NetworkMonitorManager.addLog("[$timestamp] 🔇 Alarm stopped by user.")
        
        val currentNet = NetworkMonitorManager.currentNetworkType.value
        showNormalNotification("Monitoring active. Network: $currentNet (Alarm Silenced)")
    }

    private fun triggerTestAlarm() {
        Log.d("NetworkMonitorService", "triggerTestAlarm")
        val callState = NetworkMonitorManager.callState.value
        if (callState != CallState.IDLE) {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            NetworkMonitorManager.addLog("[$timestamp] ℹ️ Test alarm suppressed: in call.")
            return
        }

        val alertMode = NetworkMonitorManager.alertMode.value
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        if (alertMode == AlertMode.ALARM) {
            isTestingAlarm = true
            alarmPlayer.start()
            NetworkMonitorManager.addLog("[$timestamp] 🔊 Test continuous alarm triggered manually.")
            showAlarmNotification("Test Alarm Active!", "Loud continuous alarm triggered for verification.")
        } else {
            alarmPlayer.playOneShotNotification()
            NetworkMonitorManager.addLog("[$timestamp] 🔔 Test notification tone played manually.")
            showNormalNotification("Test notification tone played.")
        }
    }

    private fun registerTelephonyListeners() {
        if (NetworkMonitorManager.isSimulationMode.value) {
            Log.d("NetworkMonitorService", "Simulation Mode is active. Skipping real telephony listeners registration.")
            return
        }

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        val initialCallState = when (telephonyManager.callState) {
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
            else -> CallState.IDLE
        }
        NetworkMonitorManager.updateCallState(initialCallState)
        updateInitialNetworkType(telephonyManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val callback = @RequiresApi(Build.VERSION_CODES.S) object : TelephonyCallback(),
                    TelephonyCallback.DisplayInfoListener,
                    TelephonyCallback.CallStateListener,
                    TelephonyCallback.DataConnectionStateListener {

                    override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                        val overrideType = telephonyDisplayInfo.overrideNetworkType
                        Log.d("NetworkMonitorService", "onDisplayInfoChanged: overrideType=$overrideType")
                        
                        val is5G = overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
                        
                        if (is5G) {
                            handleNetworkChanged(NetworkType.TYPE_5G)
                        } else {
                            val baseType = try {
                                telephonyManager.dataNetworkType
                            } catch (e: SecurityException) {
                                TelephonyManager.NETWORK_TYPE_UNKNOWN
                            }
                            
                            val resolvedType = when (baseType) {
                                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.TYPE_5G
                                TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.TYPE_4G
                                TelephonyManager.NETWORK_TYPE_UNKNOWN -> NetworkType.OFFLINE
                                else -> NetworkType.TYPE_OTHER
                            }
                            handleNetworkChanged(resolvedType)
                        }
                    }

                    override fun onCallStateChanged(state: Int) {
                        val callState = when (state) {
                            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
                            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
                            else -> CallState.IDLE
                        }
                        handleCallStateChanged(callState)
                    }

                    override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                        Log.d("NetworkMonitorService", "onDataConnectionStateChanged: networkType=$networkType")
                        val type = when (networkType) {
                            TelephonyManager.NETWORK_TYPE_NR -> NetworkType.TYPE_5G
                            TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.TYPE_4G
                            TelephonyManager.NETWORK_TYPE_UNKNOWN -> NetworkType.OFFLINE
                            else -> NetworkType.TYPE_OTHER
                        }
                        handleNetworkChanged(type)
                    }
                }
                
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
                Log.d("NetworkMonitorService", "Registered TelephonyCallback successfully.")
            } catch (e: Exception) {
                Log.e("NetworkMonitorService", "Failed to register TelephonyCallback", e)
            }
        } else {
            try {
                val listener = LegacyPhoneStateListener(
                    onNetworkChanged = { handleNetworkChanged(it) },
                    onCallStateChanged = { handleCallStateChanged(it) }
                )
                telephonyManager.listen(
                    listener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE or PhoneStateListener.LISTEN_CALL_STATE
                )
                legacyPhoneStateListener = listener
                Log.d("NetworkMonitorService", "Registered Legacy PhoneStateListener successfully.")
            } catch (e: Exception) {
                Log.e("NetworkMonitorService", "Failed to register Legacy PhoneStateListener", e)
            }
        }
    }

    private fun unregisterTelephonyListeners() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = telephonyCallback
            if (callback is TelephonyCallback) {
                try {
                    telephonyManager.unregisterTelephonyCallback(callback)
                    Log.d("NetworkMonitorService", "Unregistered TelephonyCallback.")
                } catch (e: Exception) {
                    Log.e("NetworkMonitorService", "Error unregistering TelephonyCallback", e)
                }
            }
            telephonyCallback = null
        } else {
            val listener = legacyPhoneStateListener
            if (listener != null) {
                try {
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                    Log.d("NetworkMonitorService", "Unregistered Legacy PhoneStateListener.")
                } catch (e: Exception) {
                    Log.e("NetworkMonitorService", "Error unregistering PhoneStateListener", e)
                }
            }
            legacyPhoneStateListener = null
        }
    }

    private fun updateInitialNetworkType(telephonyManager: TelephonyManager) {
        try {
            val networkType = telephonyManager.dataNetworkType
            val type = when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.TYPE_5G
                TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.TYPE_4G
                else -> NetworkType.TYPE_OTHER
            }
            NetworkMonitorManager.updateNetworkType(type)
            previousNetworkType = type
            Log.d("NetworkMonitorService", "Initial network type detected: $type")
        } catch (e: SecurityException) {
            Log.e("NetworkMonitorService", "Permissions missing for initial network type check", e)
        } catch (e: Exception) {
            Log.e("NetworkMonitorService", "Error checking initial network type", e)
        }
    }

    private fun isMobileDataEnabled(): Boolean {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.isDataEnabled
            } else {
                true
            }
            val settingEnabled = android.provider.Settings.Global.getInt(
                contentResolver,
                "mobile_data",
                1
            ) == 1
            isEnabled && settingEnabled
        } catch (e: Exception) {
            true
        }
    }

    private fun evaluateAlarmState() {
        val currentNet = NetworkMonitorManager.currentNetworkType.value
        val currentCall = NetworkMonitorManager.callState.value
        val isSim = NetworkMonitorManager.isSimulationMode.value
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val alertMode = NetworkMonitorManager.alertMode.value

        Log.d("NetworkMonitorService", "evaluateAlarmState: network=$currentNet, call=$currentCall, isSim=$isSim, mode=$alertMode")

        val isMobileDataOff = if (isSim) {
            currentNet == NetworkType.OFFLINE
        } else {
            currentNet == NetworkType.OFFLINE || !isMobileDataEnabled()
        }

        if (isMobileDataOff) {
            if (NetworkMonitorManager.isAlarmPlaying.value && !isTestingAlarm) {
                alarmPlayer.stop()
                Log.d("NetworkMonitorService", "Alarm stopped because mobile data is off.")
                NetworkMonitorManager.addLog("[$timestamp] 🔇 Alarm stopped: mobile data is off.")
            }
            showNormalNotification("Guarding active. Mobile data is off.")
            return
        }

        if (currentNet == NetworkType.TYPE_5G) {
            if (NetworkMonitorManager.isAlarmPlaying.value && !isTestingAlarm) {
                alarmPlayer.stop()
                Log.d("NetworkMonitorService", "Alarm stopped because reconnected to 5G.")
                NetworkMonitorManager.addLog("[$timestamp] ✅ Reconnected to 5G!")
            }
            alarmTriggeredForCurrentFallback = false
            alarmSilencedByCall = false
            showNormalNotification("Connected to 5G. Active guarding...")
            return
        }

        val isDangerState = currentNet == NetworkType.TYPE_4G || currentNet == NetworkType.TYPE_OTHER

        if (isDangerState) {
            if (currentCall != CallState.IDLE) {
                if (NetworkMonitorManager.isAlarmPlaying.value && !isTestingAlarm) {
                    alarmPlayer.stop()
                    alarmSilencedByCall = true
                    Log.d("NetworkMonitorService", "Alarm silenced due to active call: $currentCall")
                    NetworkMonitorManager.addLog("[$timestamp] 🔇 Alarm silenced due to call: $currentCall")
                }
                showNormalNotification("Alarm silenced (In Call)")
            } else {
                if (alertMode == AlertMode.ALARM) {
                    if (!NetworkMonitorManager.isAlarmPlaying.value) {
                        alarmPlayer.start()
                        Log.d("NetworkMonitorService", "Alarm triggered: Network is $currentNet and call is Idle.")
                        NetworkMonitorManager.addLog("[$timestamp] ⚠️ Alarm triggered: network is $currentNet")
                        showAlarmNotification("Network is $currentNet!", "Continuous warning alarm is playing.")
                    } else {
                        showAlarmNotification("Network is $currentNet!", "Continuous warning alarm is playing.")
                    }
                } else {
                    if (NetworkMonitorManager.isAlarmPlaying.value && !isTestingAlarm) {
                        alarmPlayer.stop()
                    }
                    if (!alarmTriggeredForCurrentFallback) {
                        alarmTriggeredForCurrentFallback = true
                        alarmPlayer.playOneShotNotification()
                        Log.d("NetworkMonitorService", "Notification-only triggered: Network is $currentNet")
                        NetworkMonitorManager.addLog("[$timestamp] 🔔 Alert notification sent: network is $currentNet")
                    }
                    showAlarmNotification("Network is $currentNet!", "Guarding alert: Network is non-5G.")
                }
            }
        }
    }

    private fun handleNetworkChanged(newType: NetworkType) {
        if (NetworkMonitorManager.isSimulationMode.value) {
            Log.d("NetworkMonitorService", "Simulation Mode is active. Ignoring real network change to $newType")
            return
        }
        val prev = previousNetworkType
        if (prev == newType) return

        NetworkMonitorManager.updateNetworkType(newType)
        previousNetworkType = newType

        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d("NetworkMonitorService", "Network changed from $prev to $newType")

        evaluateAlarmState()
    }

    private fun handleCallStateChanged(newCallState: CallState) {
        if (NetworkMonitorManager.isSimulationMode.value) {
            Log.d("NetworkMonitorService", "Simulation Mode is active. Ignoring real call state change to $newCallState")
            return
        }
        val oldCallState = NetworkMonitorManager.callState.value
        if (oldCallState == newCallState) return

        NetworkMonitorManager.updateCallState(newCallState)
        Log.d("NetworkMonitorService", "Call state changed: $newCallState")

        evaluateAlarmState()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "5G Guardian Active Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows service background monitoring status"
                setShowBadge(false)
            }
            manager.createNotificationChannel(monitorChannel)

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "5G Guardian Network Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when network drops to 4G"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildNormalNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("5G Guardian Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(getMainActivityPendingIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                getServiceActionPendingIntent(ACTION_STOP_MONITORING, 201)
            )
            .build()
    }

    private fun showNormalNotification(text: String) {
        val notification = buildNormalNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showAlarmNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setContentTitle("⚠️ $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(getMainActivityPendingIntent())
            .addAction(
                android.R.drawable.ic_media_play,
                "Stop Alarm",
                getServiceActionPendingIntent(ACTION_STOP_ALARM, 202)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                getServiceActionPendingIntent(ACTION_STOP_MONITORING, 201)
            )
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getServiceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, NetworkMonitorService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("NetworkMonitorService", "onDestroy")
        super.onDestroy()
    }

    private class LegacyPhoneStateListener(
        private val onNetworkChanged: (NetworkType) -> Unit,
        private val onCallStateChanged: (CallState) -> Unit
    ) : PhoneStateListener() {

        @Deprecated("Deprecated in Java")
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            val type = when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.TYPE_5G
                TelephonyManager.NETWORK_TYPE_LTE -> NetworkType.TYPE_4G
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> NetworkType.OFFLINE
                else -> NetworkType.TYPE_OTHER
            }
            onNetworkChanged(type)
        }

        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            val callState = when (state) {
                TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
                TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
                else -> CallState.IDLE
            }
            onCallStateChanged(callState)
        }
    }
}
