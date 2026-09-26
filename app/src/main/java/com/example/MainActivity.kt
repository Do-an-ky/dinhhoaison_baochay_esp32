package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AlarmState
import com.example.ui.components.EmergencyStrobeOverlay
import com.example.ui.screens.AlertDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusEmergency
import com.example.viewmodel.FireAlarmViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FireAlarmApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireAlarmApp(viewModel: FireAlarmViewModel = viewModel()) {
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val devices = viewModel.devices
    val historyList by viewModel.alertHistory.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showAlertDetail by viewModel.showAlertDetail.collectAsStateWithLifecycle()
    val tempThreshold by viewModel.tempThreshold.collectAsStateWithLifecycle()
    val gasThreshold by viewModel.gasThreshold.collectAsStateWithLifecycle()
    val enableVibration by viewModel.enableVibration.collectAsStateWithLifecycle()
    val enableSound by viewModel.enableSound.collectAsStateWithLifecycle()
    val firebaseDbUrl by viewModel.firebaseDbUrl.collectAsStateWithLifecycle()

    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val esp32IpAddress by viewModel.esp32IpAddress.collectAsStateWithLifecycle()
    val isFetching by viewModel.isFetching.collectAsStateWithLifecycle()
    val isHardwareConnected by viewModel.isHardwareConnected.collectAsStateWithLifecycle()
    val lastLatencyMs by viewModel.lastLatencyMs.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val connectionStatusMessage by viewModel.connectionStatusMessage.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (showAlertDetail) {
            AlertDetailScreen(
                sensorData = sensorData,
                selectedDevice = selectedDevice,
                onBack = { viewModel.closeAlertDetail() },
                onAcknowledge = { note -> viewModel.acknowledgeAlert(note) },
                onMuteBuzzer = { viewModel.muteBuzzer() }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cảnh báo cháy nổ",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        },
                        actions = {
                            // Alarm indicator button
                            IconButton(
                                onClick = { viewModel.openAlertDetail() },
                                modifier = Modifier.testTag("app_bar_alert_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (sensorData.alarmState != AlarmState.NORMAL) {
                                            Badge(
                                                containerColor = Color.White,
                                                contentColor = sensorData.alarmState.color
                                            ) {
                                                Text(sensorData.alarmState.symbol, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Cảnh báo",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = com.example.ui.theme.TopBarCrimson,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_navigation_bar")
                    ) {
                        // Tab 0: Trang chủ (Dashboard)
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Trang chủ"
                                )
                            },
                            label = { Text("Trang chủ", fontSize = 12.sp) }
                        )

                        // Tab 1: Lịch sử (History)
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 1) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "Lịch sử"
                                )
                            },
                            label = { Text("Lịch sử", fontSize = 12.sp) }
                        )

                        // Tab 2: Thiết bị (Devices)
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 2) Icons.Filled.Router else Icons.Outlined.Router,
                                    contentDescription = "Thiết bị"
                                )
                            },
                            label = { Text("Thiết bị", fontSize = 12.sp) }
                        )

                        // Tab 3: Cài đặt (Settings)
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Cài đặt"
                                )
                            },
                            label = { Text("Cài đặt", fontSize = 12.sp) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            sensorData = sensorData,
                            selectedDevice = selectedDevice,
                            devices = devices,
                            connectionMode = connectionMode,
                            esp32IpAddress = esp32IpAddress,
                            firebaseDbUrl = firebaseDbUrl,
                            isFetching = isFetching,
                            isHardwareConnected = isHardwareConnected,
                            lastLatencyMs = lastLatencyMs,
                            lastSyncTimestamp = lastSyncTimestamp,
                            connectionStatusMessage = connectionStatusMessage,
                            onSelectDevice = { viewModel.selectDevice(it) },
                            onOpenAlertDetail = { viewModel.openAlertDetail() },
                            onMuteBuzzer = { viewModel.muteBuzzer() },
                            onFetchLiveData = { viewModel.fetchLiveDataNow() },
                            onUpdateConnectionMode = { viewModel.setConnectionMode(it) },
                            onUpdateEsp32Ip = { viewModel.updateEsp32Ip(it) },
                            onUpdateFirebaseUrl = { viewModel.updateFirebaseUrl(it) }
                        )
                        1 -> HistoryScreen(
                            historyList = historyList
                        )
                        2 -> DevicesScreen(
                            devices = devices,
                            selectedDevice = selectedDevice,
                            onSelectDevice = { viewModel.selectDevice(it) }
                        )
                        3 -> SettingsScreen(
                            tempThreshold = tempThreshold,
                            gasThreshold = gasThreshold,
                            enableVibration = enableVibration,
                            enableSound = enableSound,
                            firebaseDbUrl = firebaseDbUrl,
                            onThresholdChange = { t, g ->
                                viewModel.tempThreshold.value = t
                                viewModel.gasThreshold.value = g
                            },
                            onVibrationToggle = { viewModel.enableVibration.value = it },
                            onSoundToggle = { viewModel.enableSound.value = it }
                        )
                    }
                }
            }
        }

        // Strobe emergency overlay when fire is detected
        EmergencyStrobeOverlay(
            alarmState = sensorData.alarmState,
            enableVibration = enableVibration
        )
    }
}
