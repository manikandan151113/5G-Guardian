package com.fivegguardian

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fivegguardian.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkMonitorManager.initPreferences(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentNetwork by viewModel.currentNetworkType.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsState()
    val logs by viewModel.switchLogs.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()

    val alertMode by viewModel.alertMode.collectAsState()
    val selectedAlarmToneId by viewModel.selectedAlarmToneId.collectAsState()
    val selectedNotificationToneId by viewModel.selectedNotificationToneId.collectAsState()
    val alarmTones by viewModel.alarmTonesList.collectAsState()
    val notificationTones by viewModel.notificationTonesList.collectAsState()

    // Permissions check state
    var permissionsGranted by remember {
        mutableStateOf(hasRequiredPermissions(context))
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    val alarmTonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to take persistable URI permission", e)
            }
            val fileName = getFileName(context, it) ?: "Custom Alarm File"
            NetworkMonitorManager.addCustomAlarmTone(context, it.toString(), fileName)
        }
    }

    val notificationTonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to take persistable URI permission", e)
            }
            val fileName = getFileName(context, it) ?: "Custom Notification File"
            NetworkMonitorManager.addCustomNotificationTone(context, it.toString(), fileName)
        }
    }

    LaunchedEffect(Unit) {
        permissionsGranted = hasRequiredPermissions(context)
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        AppHeader()

        // Permissions Card if missing
        if (!permissionsGranted) {
            PermissionCard(
                onRequestPermissions = {
                    val permissions = mutableListOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsLauncher.launch(permissions.toTypedArray())
                }
            )
        }

        // Live Dashboard Card
        DashboardSection(
            isMonitoring = isMonitoring,
            networkType = currentNetwork,
            callState = callState,
            isAlarmPlaying = isAlarmPlaying
        )

        // Control Panel Section
        ControlPanelSection(
            isMonitoring = isMonitoring,
            isAlarmPlaying = isAlarmPlaying,
            onStartMonitoring = { viewModel.startMonitoring(context) },
            onStopMonitoring = { viewModel.stopMonitoring(context) },
            onTestAlarm = { viewModel.testAlarm(context) },
            onStopAlarm = { viewModel.stopAlarm(context) },
            permissionsGranted = permissionsGranted
        )

        // Alert Settings Section
        AlertSettingsSection(
            alertMode = alertMode,
            selectedAlarmToneId = selectedAlarmToneId,
            selectedNotificationToneId = selectedNotificationToneId,
            alarmTones = alarmTones,
            notificationTones = notificationTones,
            onSetAlertMode = { mode -> viewModel.setAlertMode(context, mode) },
            onSetAlarmTone = { id -> viewModel.setAlarmTone(context, id) },
            onSetNotificationTone = { id -> viewModel.setNotificationTone(context, id) },
            onBrowseAlarmTone = { alarmTonePickerLauncher.launch(arrayOf("audio/*")) },
            onBrowseNotificationTone = { notificationTonePickerLauncher.launch(arrayOf("audio/*")) },
            context = context
        )

        // Simulation Center Section
        SimulationCenter(
            isSimulationMode = isSimulationMode,
            onToggleSimulation = { viewModel.toggleSimulationMode(context) },
            currentNetwork = currentNetwork,
            currentCall = callState,
            onSimulateNetwork = { type -> viewModel.simulateNetworkChange(type, context) },
            onSimulateCall = { state -> viewModel.simulateCallStateChange(state, context) }
        )

        // Historic Logs Section
        LogsSection(
            logs = logs,
            onClearLogs = { viewModel.clearLogs() }
        )
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "5G Lock Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column {
            Text(
                text = "5G Guardian",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Active fallback prevention & call awareness",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "To monitor mobile signal changes and detect active calls, this app needs Phone State, Location, and Notification permissions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_permissions_button")
            ) {
                Text("Grant Required Permissions")
            }
        }
    }
}

@Composable
fun DashboardSection(
    isMonitoring: Boolean,
    networkType: NetworkType,
    callState: CallState,
    isAlarmPlaying: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Live Telemetry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Guard Status Box
                StatusMetricBox(
                    title = "Guard Status",
                    value = if (isMonitoring) "ACTIVE" else "STOPPED",
                    icon = Icons.Filled.CheckCircle,
                    color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                    pulse = isMonitoring
                )

                // Network Signal Box
                val signalColor = when (networkType) {
                    NetworkType.TYPE_5G -> Color(0xFF2E7D32) // Bright Green
                    NetworkType.TYPE_4G -> MaterialTheme.colorScheme.error // Alert Red
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
                StatusMetricBox(
                    title = "Network Type",
                    value = networkType.toString(),
                    icon = Icons.Filled.Info,
                    color = signalColor,
                    modifier = Modifier.weight(1f),
                    pulse = isMonitoring && networkType == NetworkType.TYPE_5G
                )
            }

            // Call Status & Alarm Alert Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call Status Box
                val callColor = when (callState) {
                    CallState.IDLE -> MaterialTheme.colorScheme.outline
                    else -> Color(0xFFEF6C00) // Call Amber
                }
                StatusMetricBox(
                    title = "Phone Call State",
                    value = callState.toString(),
                    icon = Icons.Filled.Call,
                    color = callColor,
                    modifier = Modifier.weight(1f)
                )

                // Alarm Audio Status Box
                val alarmColor = if (isAlarmPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                StatusMetricBox(
                    title = "Alarm Player",
                    value = if (isAlarmPlaying) "PLAYING 🔊" else "SILENT 🔇",
                    icon = Icons.Filled.Notifications,
                    color = alarmColor,
                    modifier = Modifier.weight(1f),
                    pulse = isAlarmPlaying
                )
            }
        }
    }
}

@Composable
fun StatusMetricBox(
    title: String,
    value: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon ?: imageVector ?: Icons.Filled.Info,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (pulse) color.copy(alpha = alphaAnim) else color
            )
        }
    }
}

@Composable
fun ControlPanelSection(
    isMonitoring: Boolean,
    isAlarmPlaying: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onTestAlarm: () -> Unit,
    onStopAlarm: () -> Unit,
    permissionsGranted: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Guarding Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isMonitoring) {
                    Button(
                        onClick = onStartMonitoring,
                        enabled = permissionsGranted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("start_monitoring_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                        Spacer(Modifier.width(8.dp))
                        Text("Start Guard")
                    }
                } else {
                    Button(
                        onClick = onStopMonitoring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("stop_monitoring_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Stop")
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Guard")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTestAlarm,
                    enabled = isMonitoring && !isAlarmPlaying,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .testTag("test_alarm_button")
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Test")
                    Spacer(Modifier.width(6.dp))
                    Text("Test Alarm")
                }

                OutlinedButton(
                    onClick = onStopAlarm,
                    enabled = isAlarmPlaying,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .testTag("stop_alarm_button")
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Silence")
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Alarm")
                }
            }
        }
    }
}

@Composable
fun SimulationCenter(
    isSimulationMode: Boolean,
    onToggleSimulation: () -> Unit,
    currentNetwork: NetworkType,
    currentCall: CallState,
    onSimulateNetwork: (NetworkType) -> Unit,
    onSimulateCall: (CallState) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = "Simulate icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Simulation Console",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Switch(
                    checked = isSimulationMode,
                    onCheckedChange = { onToggleSimulation() },
                    modifier = Modifier.testTag("simulation_switch")
                )
            }

            if (isSimulationMode) {
                Text(
                    text = "Forces state overrides. Use this to verify how the guard behaves during cell switches and active calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )

                // Network Simulation Controls
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Simulate Network Drop / Reconnect:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val is5G = currentNetwork == NetworkType.TYPE_5G
                        val is4G = currentNetwork == NetworkType.TYPE_4G

                        FilledTonalButton(
                            onClick = { onSimulateNetwork(NetworkType.TYPE_5G) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (is5G) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surface,
                                contentColor = if (is5G) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_5g_button")
                        ) {
                            Text("5G Signal", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { onSimulateNetwork(NetworkType.TYPE_4G) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (is4G) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surface,
                                contentColor = if (is4G) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_4g_button")
                        ) {
                            Text("4G LTE", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { onSimulateNetwork(NetworkType.TYPE_OTHER) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Other/3G", fontSize = 11.sp)
                        }
                    }
                }

                // Call Simulation Controls
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Simulate Phone Call Interception:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isIdle = currentCall == CallState.IDLE
                        val isRinging = currentCall == CallState.RINGING
                        val isOffhook = currentCall == CallState.OFFHOOK

                        FilledTonalButton(
                            onClick = { onSimulateCall(CallState.IDLE) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isIdle) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sim_idle_button")
                        ) {
                            Text("No Call", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { onSimulateCall(CallState.RINGING) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isRinging) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surface,
                                contentColor = if (isRinging) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(40.dp)
                        ) {
                            Text("Ringing", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { onSimulateCall(CallState.OFFHOOK) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isOffhook) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surface,
                                contentColor = if (isOffhook) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(40.dp)
                                .testTag("sim_incall_button")
                        ) {
                            Text("In Call", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "Enable to override live carrier data with mock connection states.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun LogsSection(
    logs: List<String>,
    onClearLogs: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Logs",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Historical Event Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear Logs",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (log.contains("⚠️") || log.contains("Drop")) {
                                    MaterialTheme.colorScheme.error
                                } else if (log.contains("✅") || log.contains("Reconnected")) {
                                    Color(0xFF2E7D32)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hasRequiredPermissions(context: Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val phoneState = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
    val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return fineLocation && phoneState && postNotifications
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    result = cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Log.e("MainScreen", "Error query filename", e)
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun AlertSettingsSection(
    alertMode: AlertMode,
    selectedAlarmToneId: String,
    selectedNotificationToneId: String,
    alarmTones: List<SoundOption>,
    notificationTones: List<SoundOption>,
    onSetAlertMode: (AlertMode) -> Unit,
    onSetAlarmTone: (String) -> Unit,
    onSetNotificationTone: (String) -> Unit,
    onBrowseAlarmTone: () -> Unit,
    onBrowseNotificationTone: () -> Unit,
    context: Context
) {
    var showAlarmToneDialog by remember { mutableStateOf(false) }
    var showNotificationToneDialog by remember { mutableStateOf(false) }

    val currentAlarmToneTitle = alarmTones.find { it.id == selectedAlarmToneId }?.title ?: "Default System Alarm"
    val currentNotificationToneTitle = notificationTones.find { it.id == selectedNotificationToneId }?.title ?: "Default System Notification"

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Alert Settings Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Alert Customization",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Choose how you want to be alerted and personalize the sounds used when the signal falls back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Alert Mode Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Alert Mode",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Continuous Alarm Option
                    FilledTonalButton(
                        onClick = { onSetAlertMode(AlertMode.ALARM) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (alertMode == AlertMode.ALARM) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = if (alertMode == AlertMode.ALARM) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        ),
                        border = if (alertMode != AlertMode.ALARM) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("alert_mode_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Alarm Active"
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Loud Alarm",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Notification-Only Option
                    FilledTonalButton(
                        onClick = { onSetAlertMode(AlertMode.NOTIFICATION_ONLY) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (alertMode == AlertMode.NOTIFICATION_ONLY) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = if (alertMode == AlertMode.NOTIFICATION_ONLY) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        ),
                        border = if (alertMode != AlertMode.NOTIFICATION_ONLY) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("alert_mode_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notification Only"
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quiet Notify",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Text(
                    text = if (alertMode == AlertMode.ALARM) {
                        "Plays a continuous looping sound and shows notifications until dismissed."
                    } else {
                        "Plays a quiet one-shot sound once and shows notification."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Alarm Tone Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarm Sound Tone",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentAlarmToneTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = { showAlarmToneDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("change_alarm_tone_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Edit")
                    Spacer(Modifier.width(4.dp))
                    Text("Change")
                }
            }

            // Notification Tone Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Sound Tone",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentNotificationToneTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = { showNotificationToneDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("change_notification_tone_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Edit")
                    Spacer(Modifier.width(4.dp))
                    Text("Change")
                }
            }
        }
    }

    // Sound Selection Dialogs
    if (showAlarmToneDialog) {
        SoundSelectionDialog(
            title = "Choose Alarm Tone",
            options = alarmTones,
            selectedId = selectedAlarmToneId,
            onSelect = onSetAlarmTone,
            onBrowseDeviceFile = {
                showAlarmToneDialog = false
                onBrowseAlarmTone()
            },
            onDismiss = { showAlarmToneDialog = false },
            context = context
        )
    }

    if (showNotificationToneDialog) {
        SoundSelectionDialog(
            title = "Choose Notification Tone",
            options = notificationTones,
            selectedId = selectedNotificationToneId,
            onSelect = onSetNotificationTone,
            onBrowseDeviceFile = {
                showNotificationToneDialog = false
                onBrowseNotificationTone()
            },
            onDismiss = { showNotificationToneDialog = false },
            context = context
        )
    }
}

@Composable
fun SoundSelectionDialog(
    title: String,
    options: List<SoundOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onBrowseDeviceFile: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    context: Context
) {
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewToneGen by remember { mutableStateOf<ToneGenerator?>(null) }

    fun stopPreview() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            previewToneGen?.let {
                it.stopTone()
                it.release()
            }
        } catch (e: Exception) {}
        previewPlayer = null
        previewToneGen = null
    }

    fun playPreview(soundOption: SoundOption) {
        stopPreview()
        try {
            if (soundOption.isTone) {
                val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                previewToneGen = tg
                tg.startTone(soundOption.toneType, 1200) // play for 1.2 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    stopPreview()
                }, 1500)
            } else {
                val uri = soundOption.uriString?.let { Uri.parse(it) }
                if (uri != null) {
                    val mp = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        prepare()
                        start()
                    }
                    previewPlayer = mp
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        stopPreview()
                    }, 2000)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundSelectionDialog", "Error playing sound preview", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    AlertDialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onBrowseDeviceFile != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .clickable {
                                        stopPreview()
                                        onBrowseDeviceFile()
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Browse Folder Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Browse from device...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(options) { option ->
                        val isSelected = option.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelect(option.id)
                                    playPreview(option)
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(option.id)
                                    playPreview(option)
                                }
                            )
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    stopPreview()
                    onDismiss()
                }
            ) {
                Text("Done")
            }
        }
    )
}
