package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceNode
import com.example.model.SensorData
import com.example.network.Esp32ConnectionMode
import com.example.ui.components.GaugeWidget
import com.example.ui.components.StatusBanner
import com.example.ui.theme.GaugeBlue
import com.example.ui.theme.GaugeBlueTrack
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeGreenTrack
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusEmergency
import com.example.ui.theme.StatusNormal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sensorData: SensorData,
    selectedDevice: DeviceNode,
    devices: List<DeviceNode>,
    connectionMode: Esp32ConnectionMode,
    esp32IpAddress: String,
    firebaseDbUrl: String,
    isFetching: Boolean,
    isHardwareConnected: Boolean,
    lastLatencyMs: Long?,
    lastSyncTimestamp: Long,
    connectionStatusMessage: String,
    onSelectDevice: (DeviceNode) -> Unit,
    onOpenAlertDetail: () -> Unit,
    onMuteBuzzer: () -> Unit,
    onFetchLiveData: () -> Unit,
    onUpdateConnectionMode: (Esp32ConnectionMode) -> Unit,
    onUpdateEsp32Ip: (String) -> Unit,
    onUpdateFirebaseUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Device Selector Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { deviceMenuExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Thiết bị",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = selectedDevice.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "${selectedDevice.location} • IP: $esp32IpAddress",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Chọn thiết bị",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DropdownMenu(
                    expanded = deviceMenuExpanded,
                    onDismissRequest = { deviceMenuExpanded = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(device.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${device.location} (${device.ipAddress})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onSelectDevice(device)
                                deviceMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeviceHub,
                                    contentDescription = null,
                                    tint = if (device.id == selectedDevice.id) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        )
                    }
                }
            }
        }

        // 2. Real Hardware ESP32 Connection & Live Telemetry Stream Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHardwareConnected)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                else
                    StatusDisconnected.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("esp32_connection_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isHardwareConnected) StatusNormal else StatusDisconnected)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHardwareConnected) "KẾT NỐI TRỰC TIẾP MẠCH ESP32" else "MẤT KẾT NỐI PHẦN CỨNG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isHardwareConnected) StatusNormal else StatusDisconnected,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showConfigDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Cấu hình IP/Firebase",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onFetchLiveData,
                            enabled = !isFetching,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("refresh_live_data_button")
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Đọc lại dữ liệu cảm biến",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val formattedTime = remember(lastSyncTimestamp) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTimestamp))
                }

                Text(
                    text = "$connectionStatusMessage • Cập nhật: $formattedTime",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (connectionMode == Esp32ConnectionMode.DIRECT_ESP32_WIFI)
                            "Kênh: HTTP REST (http://$esp32IpAddress/data)"
                        else
                            "Kênh: Firebase Realtime Database",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (lastLatencyMs != null && isHardwareConnected) {
                        Text(
                            text = "Độ trễ: ${lastLatencyMs}ms",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = StatusNormal
                        )
                    }
                }
            }
        }

        // 3. Active Buzzer Warning Banner (Appears when ESP32 buzzer is ON)
        AnimatedVisibility(
            visible = sensorData.buzzerActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StatusEmergency),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_buzzer_alert_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Còi hú",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Còi hú ESP32 đang kích hoạt!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Surface(
                        onClick = onMuteBuzzer,
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        modifier = Modifier.testTag("mute_buzzer_quick_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = StatusEmergency,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tắt còi",
                                fontWeight = FontWeight.Bold,
                                color = StatusEmergency,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Two Half-Circle Gauges (Side by Side matching the photo)
        // Left: Green Half-Circle Gauge for Temperature (from DHT11/ESP32)
        // Right: Blue Half-Circle Gauge for Smoke/Gas (from MQ-2/ESP32)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Temperature Gauge (Green Half-Circle)
            GaugeWidget(
                title = "Nhiệt độ",
                value = sensorData.temperature.toFloat(),
                unit = "°C",
                minValue = 0f,
                maxValue = 100f,
                warningThreshold = 50f,
                dangerThreshold = 65f,
                icon = Icons.Default.Thermostat,
                gaugeColor = GaugeGreen,
                trackColor = GaugeGreenTrack,
                modifier = Modifier.weight(1f)
            )

            // Smoke/Gas Gauge (Blue Half-Circle)
            GaugeWidget(
                title = "Mức khói/gas",
                value = sensorData.gasSmokeLevel.toFloat(),
                unit = "ppm",
                minValue = 0f,
                maxValue = 1000f,
                warningThreshold = 400f,
                dangerThreshold = 600f,
                icon = Icons.Default.Sensors,
                gaugeColor = GaugeBlue,
                trackColor = GaugeBlueTrack,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Secondary Metric Indicators: Độ ẩm (DHT11) & Cảm biến lửa (IR DOUT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Humidity Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1F5FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Độ ẩm",
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Độ ẩm (DHT11)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${sensorData.humidity}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }
            }

            // Flame Sensor Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (sensorData.flameStatus) StatusEmergency.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (sensorData.flameStatus) StatusEmergency.copy(alpha = 0.25f)
                                else StatusNormal.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Cảm biến lửa",
                            tint = if (sensorData.flameStatus) StatusEmergency else StatusNormal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cảm biến lửa",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (sensorData.flameStatus) "CÓ LỬA!" else "An toàn",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (sensorData.flameStatus) StatusEmergency else StatusNormal
                            )
                        )
                    }
                }
            }
        }

        // 6. Overall Semantic Status Card at the bottom (as in photo)
        StatusBanner(
            alarmState = sensorData.alarmState,
            timestamp = sensorData.timestamp,
            onClick = onOpenAlertDetail
        )

        // 7. ESP32 Hardware Status Row (Real GPIO pin status)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Trạng thái các cổng I/O mạch ESP32",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    HardwarePinStatus(label = "Buzzer (GPIO25)", isActive = sensorData.buzzerActive)
                    HardwarePinStatus(label = "LED (GPIO26)", isActive = sensorData.ledActive)
                    HardwarePinStatus(label = "DHT11 (GPIO4)", isActive = isHardwareConnected)
                    HardwarePinStatus(label = "MQ-2 (GPIO34)", isActive = isHardwareConnected)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Modal cấu hình IP mạch ESP32 / Firebase Realtime Database
    if (showConfigDialog) {
        var tempIp by remember { mutableStateOf(esp32IpAddress) }
        var tempUrl by remember { mutableStateOf(firebaseDbUrl) }
        var selectedMode by remember { mutableStateOf(connectionMode) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text("Cấu hình kết nối phần cứng ESP32", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Chọn phương thức nhận dữ liệu đo từ vi điều khiển ESP32:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == Esp32ConnectionMode.DIRECT_ESP32_WIFI,
                            onClick = { selectedMode = Esp32ConnectionMode.DIRECT_ESP32_WIFI },
                            label = { Text("WiFi ESP32", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        FilterChip(
                            selected = selectedMode == Esp32ConnectionMode.FIREBASE_RTDB,
                            onClick = { selectedMode = Esp32ConnectionMode.FIREBASE_RTDB },
                            label = { Text("Firebase RTDB", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (selectedMode == Esp32ConnectionMode.DIRECT_ESP32_WIFI) {
                        OutlinedTextField(
                            value = tempIp,
                            onValueChange = { tempIp = it },
                            label = { Text("Địa chỉ IP ESP32 trong mạng WiFi") },
                            placeholder = { Text("192.168.1.105") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Ghi chú: ESP32 trả về JSON tại endpoint /data (ví dụ http://$tempIp/data)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("Firebase Realtime Database URL") },
                            placeholder = { Text("https://xxx-default-rtdb.firebaseio.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Lắng nghe dữ liệu tại /devices/${selectedDevice.id}.json",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateConnectionMode(selectedMode)
                        if (selectedMode == Esp32ConnectionMode.DIRECT_ESP32_WIFI) {
                            onUpdateEsp32Ip(tempIp)
                        } else {
                            onUpdateFirebaseUrl(tempUrl)
                        }
                        showConfigDialog = false
                    }
                ) {
                    Text("Lưu cấu hình")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun HardwarePinStatus(label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isActive) StatusEmergency else StatusNormal)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
