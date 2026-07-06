package com.fivegguardian

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fivegguardian.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel()
        
        // Reset singleton manager before each test
        NetworkMonitorManager.updateNetworkType(NetworkType.TYPE_5G)
        NetworkMonitorManager.updateCallState(CallState.IDLE)
        NetworkMonitorManager.updateAlarmPlaying(false)
        NetworkMonitorManager.updateMonitoringStatus(false)
        NetworkMonitorManager.updateSimulationMode(false)
        NetworkMonitorManager.clearLogs()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("5G Guardian", appName)
    }

    @Test
    fun `test network type state flow updates`() {
        NetworkMonitorManager.updateNetworkType(NetworkType.TYPE_4G)
        assertEquals(NetworkType.TYPE_4G, viewModel.currentNetworkType.value)

        NetworkMonitorManager.updateNetworkType(NetworkType.TYPE_5G)
        assertEquals(NetworkType.TYPE_5G, viewModel.currentNetworkType.value)
    }

    @Test
    fun `test call state flow updates`() {
        NetworkMonitorManager.updateCallState(CallState.RINGING)
        assertEquals(CallState.RINGING, viewModel.callState.value)

        NetworkMonitorManager.updateCallState(CallState.OFFHOOK)
        assertEquals(CallState.OFFHOOK, viewModel.callState.value)

        NetworkMonitorManager.updateCallState(CallState.IDLE)
        assertEquals(CallState.IDLE, viewModel.callState.value)
    }

    @Test
    fun `test monitoring status flow updates`() {
        NetworkMonitorManager.updateMonitoringStatus(true)
        assertTrue(viewModel.isMonitoring.value)

        NetworkMonitorManager.updateMonitoringStatus(false)
        assertFalse(viewModel.isMonitoring.value)
    }

    @Test
    fun `test simulation mode flow activation`() {
        assertFalse(viewModel.isSimulationMode.value)
        viewModel.toggleSimulationMode(context)
        assertTrue(viewModel.isSimulationMode.value)
        
        // Initial simulation setup sets Network to 5G and Call to IDLE
        assertEquals(NetworkType.TYPE_5G, viewModel.currentNetworkType.value)
        assertEquals(CallState.IDLE, viewModel.callState.value)
    }

    @Test
    fun `test fallback simulation without active call triggers alarm`() {
        viewModel.toggleSimulationMode(context) // Enable simulation
        
        // Setup initial 5G and IDLE call status
        viewModel.simulateNetworkChange(NetworkType.TYPE_5G, context)
        viewModel.simulateCallStateChange(CallState.IDLE, context)
        
        // Fallback to 4G
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        
        // Verify that the alarm is requested/playing
        assertTrue(viewModel.isAlarmPlaying.value)
        
        // Verify fallback log is added
        val logs = viewModel.switchLogs.value
        assertTrue(logs.any { it.contains("Fallback") })
    }

    @Test
    fun `test fallback simulation during active call suppresses alarm`() {
        viewModel.toggleSimulationMode(context) // Enable simulation
        
        // Setup initial 5G and active call status
        viewModel.simulateNetworkChange(NetworkType.TYPE_5G, context)
        viewModel.simulateCallStateChange(CallState.OFFHOOK, context)
        
        // Fallback to 4G
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        
        // Alarm should be suppressed due to call state
        assertFalse(viewModel.isAlarmPlaying.value)
    }

    @Test
    fun `test transition to active call silences existing alarm`() {
        viewModel.toggleSimulationMode(context)
        
        // Setup 4G fallback first with NO active call (alarm starts)
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        assertTrue(viewModel.isAlarmPlaying.value)
        
        // Now simulate a phone call state change
        viewModel.simulateCallStateChange(CallState.RINGING, context)
        
        // Alarm should be silenced automatically
        assertFalse(viewModel.isAlarmPlaying.value)
    }

    @Test
    fun `test reconnecting to 5G silences existing alarm`() {
        viewModel.toggleSimulationMode(context)
        
        // Setup 4G fallback first with NO active call (alarm starts)
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        assertTrue(viewModel.isAlarmPlaying.value)
        
        // Reconnect to 5G
        viewModel.simulateNetworkChange(NetworkType.TYPE_5G, context)
        
        // Alarm should be silenced automatically
        assertFalse(viewModel.isAlarmPlaying.value)
    }

    @Test
    fun `test manual alarm stop request silences alarm`() {
        viewModel.toggleSimulationMode(context)
        
        // Trigger alarm
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        assertTrue(viewModel.isAlarmPlaying.value)
        
        // Silence manually
        viewModel.stopAlarm(context)
        
        // Alarm is stopped
        assertFalse(viewModel.isAlarmPlaying.value)
    }

    @Test
    fun `test fallback simulation to OFFLINE does not trigger alarm`() {
        viewModel.toggleSimulationMode(context) // Enable simulation
        
        // Setup initial 5G and IDLE call status
        viewModel.simulateNetworkChange(NetworkType.TYPE_5G, context)
        viewModel.simulateCallStateChange(CallState.IDLE, context)
        
        // Mobile data is turned off (becomes OFFLINE)
        viewModel.simulateNetworkChange(NetworkType.OFFLINE, context)
        
        // Alarm should NOT be playing because mobile data is off
        assertFalse(viewModel.isAlarmPlaying.value)
    }

    @Test
    fun `test transition to OFFLINE silences existing alarm`() {
        viewModel.toggleSimulationMode(context)
        
        // Setup 4G fallback first with NO active call (alarm starts)
        viewModel.simulateNetworkChange(NetworkType.TYPE_4G, context)
        assertTrue(viewModel.isAlarmPlaying.value)
        
        // Now mobile data is turned off (becomes OFFLINE)
        viewModel.simulateNetworkChange(NetworkType.OFFLINE, context)
        
        // Alarm should be silenced automatically
        assertFalse(viewModel.isAlarmPlaying.value)
    }
}
