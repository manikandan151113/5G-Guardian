package com.fivegguardian

import android.content.Context
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkType {
    TYPE_5G,
    TYPE_4G,
    TYPE_OTHER,
    OFFLINE;

    override fun toString(): String {
        return when (this) {
            TYPE_5G -> "5G"
            TYPE_4G -> "4G (LTE)"
            TYPE_OTHER -> "Other (3G/2G)"
            OFFLINE -> "Offline"
        }
    }
}

enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK;

    override fun toString(): String {
        return when (this) {
            IDLE -> "Idle"
            RINGING -> "Ringing (Incoming)"
            OFFHOOK -> "In Call / Active"
        }
    }
}

enum class AlertMode {
    ALARM,
    NOTIFICATION_ONLY;

    override fun toString(): String {
        return when (this) {
            ALARM -> "Continuous Alarm Sound"
            NOTIFICATION_ONLY -> "Quiet Push Notification Only"
        }
    }
}

data class SoundOption(
    val id: String,
    val title: String,
    val uriString: String? = null,
    val isTone: Boolean = false,
    val toneType: Int = 0
)

object NetworkMonitorManager {
    private val _currentNetworkType = MutableStateFlow(NetworkType.OFFLINE)
    val currentNetworkType: StateFlow<NetworkType> = _currentNetworkType.asStateFlow()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _isAlarmPlaying = MutableStateFlow(false)
    val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()

    private val _switchLogs = MutableStateFlow<List<String>>(emptyList())
    val switchLogs: StateFlow<List<String>> = _switchLogs.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    // Preferences & Alerts customization states
    private val _alertMode = MutableStateFlow(AlertMode.ALARM)
    val alertMode: StateFlow<AlertMode> = _alertMode.asStateFlow()

    private val _selectedAlarmToneId = MutableStateFlow("sys_alarm")
    val selectedAlarmToneId: StateFlow<String> = _selectedAlarmToneId.asStateFlow()

    private val _selectedNotificationToneId = MutableStateFlow("sys_notification")
    val selectedNotificationToneId: StateFlow<String> = _selectedNotificationToneId.asStateFlow()

    // Available alarms & notifications lists
    private val _alarmTonesList = MutableStateFlow<List<SoundOption>>(emptyList())
    val alarmTonesList: StateFlow<List<SoundOption>> = _alarmTonesList.asStateFlow()

    private val _notificationTonesList = MutableStateFlow<List<SoundOption>>(emptyList())
    val notificationTonesList: StateFlow<List<SoundOption>> = _notificationTonesList.asStateFlow()

    fun initPreferences(context: Context) {
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        
        val modeStr = prefs.getString("alert_mode", AlertMode.ALARM.name) ?: AlertMode.ALARM.name
        _alertMode.value = try { AlertMode.valueOf(modeStr) } catch(e: Exception) { AlertMode.ALARM }

        _selectedAlarmToneId.value = prefs.getString("selected_alarm_tone", "sys_alarm") ?: "sys_alarm"
        _selectedNotificationToneId.value = prefs.getString("selected_notification_tone", "sys_notification") ?: "sys_notification"

        // Load lists of sounds
        loadSoundsLists(context)
    }

    private fun loadSoundsLists(context: Context) {
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        
        // Build alarm tones list
        val alarms = mutableListOf<SoundOption>()
        
        // Add custom alarm tone at the very top if set
        val customAlarmUri = prefs.getString("custom_alarm_uri", null)
        val customAlarmTitle = prefs.getString("custom_alarm_title", null)
        if (customAlarmUri != null && customAlarmTitle != null) {
            alarms.add(SoundOption("custom_alarm", "📁 Picked: $customAlarmTitle", customAlarmUri))
        }

        alarms.add(SoundOption("sys_alarm", "Default System Alarm", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()))
        alarms.add(SoundOption("sys_ringtone", "Default System Ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString()))
        alarms.add(SoundOption("sys_notification", "Default System Notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()))
        alarms.add(SoundOption("tone_cdma", "CDMA Emergency Ringback (Synth)", isTone = true, toneType = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK))
        alarms.add(SoundOption("tone_beep", "High-Pitch Warning Beep (Synth)", isTone = true, toneType = ToneGenerator.TONE_CDMA_HIGH_L))
        alarms.add(SoundOption("tone_siren", "Emergency Siren Sweep (Synth)", isTone = true, toneType = ToneGenerator.TONE_SUP_ERROR))

        try {
            val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            val cursor = manager.cursor
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(cursor.position)
                    if (uri != null) {
                        alarms.add(SoundOption("uri_" + uri.toString(), title, uri.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitorManager", "Error querying ringtones", e)
        }
        _alarmTonesList.value = alarms

        // Build notification tones list
        val notifications = mutableListOf<SoundOption>()

        // Add custom notification tone at the very top if set
        val customNotificationUri = prefs.getString("custom_notification_uri", null)
        val customNotificationTitle = prefs.getString("custom_notification_title", null)
        if (customNotificationUri != null && customNotificationTitle != null) {
            notifications.add(SoundOption("custom_notification", "📁 Picked: $customNotificationTitle", customNotificationUri))
        }

        notifications.add(SoundOption("sys_notification", "Default System Notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()))
        notifications.add(SoundOption("sys_alarm", "Default System Alarm", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()))
        notifications.add(SoundOption("sys_ringtone", "Default System Ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString()))
        notifications.add(SoundOption("tone_beep_double", "Quick Double Beep (Synth)", isTone = true, toneType = ToneGenerator.TONE_PROP_BEEP))
        notifications.add(SoundOption("tone_tick", "Discrete Tick Sound (Synth)", isTone = true, toneType = ToneGenerator.TONE_PROP_PROMPT))

        try {
            val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_NOTIFICATION) }
            val cursor = manager.cursor
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(cursor.position)
                    if (uri != null) {
                        notifications.add(SoundOption("uri_" + uri.toString(), title, uri.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitorManager", "Error querying notification sounds", e)
        }
        _notificationTonesList.value = notifications
    }

    fun addCustomAlarmTone(context: Context, uriStr: String, title: String) {
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("custom_alarm_uri", uriStr)
            putString("custom_alarm_title", title)
        }.apply()
        loadSoundsLists(context)
        setAlarmTone(context, "custom_alarm")
    }

    fun addCustomNotificationTone(context: Context, uriStr: String, title: String) {
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("custom_notification_uri", uriStr)
            putString("custom_notification_title", title)
        }.apply()
        loadSoundsLists(context)
        setNotificationTone(context, "custom_notification")
    }

    fun setAlertMode(context: Context, mode: AlertMode) {
        _alertMode.value = mode
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        prefs.edit().putString("alert_mode", mode.name).apply()
    }

    fun setAlarmTone(context: Context, toneId: String) {
        _selectedAlarmToneId.value = toneId
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_alarm_tone", toneId).apply()
    }

    fun setNotificationTone(context: Context, toneId: String) {
        _selectedNotificationToneId.value = toneId
        val prefs = context.getSharedPreferences("com.fivegguardian.settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_notification_tone", toneId).apply()
    }

    fun updateNetworkType(type: NetworkType) {
        _currentNetworkType.value = type
    }

    fun updateCallState(state: CallState) {
        _callState.value = state
    }

    fun updateMonitoringStatus(monitoring: Boolean) {
        _isMonitoring.value = monitoring
    }

    fun updateAlarmPlaying(playing: Boolean) {
        _isAlarmPlaying.value = playing
    }

    fun updateSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
    }

    fun addLog(log: String) {
        val current = _switchLogs.value.toMutableList()
        current.add(0, log) // Add at top (newest first)
        if (current.size > 50) {
            current.removeLast()
        }
        _switchLogs.value = current
    }

    fun clearLogs() {
        _switchLogs.value = emptyList()
    }
}
