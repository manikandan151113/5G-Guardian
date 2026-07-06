package com.fivegguardian

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    // Expose flows from the singleton manager
    val currentNetworkType: StateFlow<NetworkType> = NetworkMonitorManager.currentNetworkType
    val callState: StateFlow<CallState> = NetworkMonitorManager.callState
    val isMonitoring: StateFlow<Boolean> = NetworkMonitorManager.isMonitoring
    val isAlarmPlaying: StateFlow<Boolean> = NetworkMonitorManager.isAlarmPlaying
    val switchLogs: StateFlow<List<String>> = NetworkMonitorManager.switchLogs

    // Alert customizations flows
    val alertMode: StateFlow<AlertMode> = NetworkMonitorManager.alertMode
    val selectedAlarmToneId: StateFlow<String> = NetworkMonitorManager.selectedAlarmToneId
    val selectedNotificationToneId: StateFlow<String> = NetworkMonitorManager.selectedNotificationToneId
    val alarmTonesList: StateFlow<List<SoundOption>> = NetworkMonitorManager.alarmTonesList
    val notificationTonesList: StateFlow<List<SoundOption>> = NetworkMonitorManager.notificationTonesList

    fun setAlertMode(context: Context, mode: AlertMode) {
        NetworkMonitorManager.setAlertMode(context, mode)
    }

    fun setAlarmTone(context: Context, toneId: String) {
        NetworkMonitorManager.setAlarmTone(context, toneId)
    }

    fun setNotificationTone(context: Context, toneId: String) {
        NetworkMonitorManager.setNotificationTone(context, toneId)
    }

    // Simulation states delegated to NetworkMonitorManager singleton for service access
    val isSimulationMode: StateFlow<Boolean> = NetworkMonitorManager.isSimulationMode
    private var simAlarmSilencedByCall = false

    fun toggleSimulationMode(context: Context) {
        val newState = !isSimulationMode.value
        NetworkMonitorManager.updateSimulationMode(newState)
        simAlarmSilencedByCall = false
        
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (newState) {
            NetworkMonitorManager.addLog("[$timestamp] 🧪 Simulation Mode ENABLED.")
            // Initialize simulation with realistic starting state
            NetworkMonitorManager.updateNetworkType(NetworkType.TYPE_5G)
            NetworkMonitorManager.updateCallState(CallState.IDLE)
            // Restart service if running to unbind real telephony listeners
            if (isMonitoring.value) {
                stopMonitoring(context)
                startMonitoring(context)
            }
        } else {
            NetworkMonitorManager.addLog("[$timestamp] 🧪 Simulation Mode DISABLED.")
            // If service is running, restart it to re-bind real listeners, else clear state
            if (isMonitoring.value) {
                stopMonitoring(context)
                startMonitoring(context)
            } else {
                NetworkMonitorManager.updateNetworkType(NetworkType.OFFLINE)
                NetworkMonitorManager.updateCallState(CallState.IDLE)
            }
        }
    }

    fun startMonitoring(context: Context) {
        val intent = Intent(context, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_START_MONITORING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopMonitoring(context: Context) {
        val intent = Intent(context, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_STOP_MONITORING
        }
        context.startService(intent)
    }

    fun stopAlarm(context: Context) {
        if (isSimulationMode.value) {
            NetworkMonitorManager.updateAlarmPlaying(false)
            simAlarmSilencedByCall = false
        }
        val intent = Intent(context, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_STOP_ALARM
        }
        context.startService(intent)
    }

    fun testAlarm(context: Context) {
        if (isSimulationMode.value) {
            NetworkMonitorManager.updateAlarmPlaying(true)
        }
        val intent = Intent(context, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_TRIGGER_TEST_ALARM
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun clearLogs() {
        NetworkMonitorManager.clearLogs()
    }

    // SIMULATION METHODS
    fun simulateNetworkChange(type: NetworkType, context: Context) {
        if (!isSimulationMode.value) return
        
        val oldType = currentNetworkType.value
        if (oldType == type) return

        NetworkMonitorManager.updateNetworkType(type)
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        if (type == NetworkType.OFFLINE) {
            NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Mobile data is OFF ($type)!")
            simAlarmSilencedByCall = false
            if (isAlarmPlaying.value) {
                stopAlarm(context)
            }
        } else if (type == NetworkType.TYPE_5G) {
            NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Reconnected to 5G!")
            simAlarmSilencedByCall = false
            if (isAlarmPlaying.value) {
                stopAlarm(context)
            }
        } else { // TYPE_4G or TYPE_OTHER
            NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Network Fallback to non-5G ($type)!")
            val currentCall = callState.value
            if (currentCall == CallState.IDLE) {
                testAlarm(context)
            } else {
                NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Suppressed alarm (Call State: $currentCall)")
            }
        }
    }

    fun simulateCallStateChange(state: CallState, context: Context) {
        if (!isSimulationMode.value) return

        val oldState = callState.value
        if (oldState == state) return

        NetworkMonitorManager.updateCallState(state)
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Call state changed to: $state")

        if (state != CallState.IDLE) {
            if (isAlarmPlaying.value) {
                stopAlarm(context)
                simAlarmSilencedByCall = true
                NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Alarm silenced due to Call ($state)")
            }
        } else {
            val currentNet = currentNetworkType.value
            val isDangerState = currentNet == NetworkType.TYPE_4G || currentNet == NetworkType.TYPE_OTHER
            if (isDangerState) {
                simAlarmSilencedByCall = false
                NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Call ended. Resuming alarm since network is still $currentNet.")
                testAlarm(context)
            } else {
                simAlarmSilencedByCall = false
                NetworkMonitorManager.addLog("[$timestamp] 🧪 [SIM] Call ended. Network is $currentNet.")
                if (isAlarmPlaying.value) {
                    stopAlarm(context)
                }
            }
        }
    }
}
