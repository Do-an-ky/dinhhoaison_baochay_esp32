package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AlarmState
import com.example.model.AlertRecord
import com.example.model.DeviceNode
import com.example.model.SensorData
import com.example.network.Esp32ConnectionMode
import com.example.network.Esp32Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class FireAlarmViewModel(
    private val esp32Repository: Esp32Repository = Esp32Repository()
) : ViewModel() {

    // Danh sách các mạch vi điều khiển ESP32 thực tế trong hệ thống
    val devices = listOf(
        DeviceNode(
            id = "phong_bep",
            name = "Phòng Bếp",
            location = "Tầng 1 - Khu nhà bếp",
            ipAddress = "192.168.1.105",
            macAddress = "24:6F:28:B4:7A:1C",
            wifiRssi = -52
        ),
        DeviceNode(
            id = "nha_kho",
            name = "Nhà Kho Vật Tư",
            location = "Tầng trệt - Kho sau",
            ipAddress = "192.168.1.106",
            macAddress = "24:6F:28:B4:7A:2D",
            wifiRssi = -68
        ),
        DeviceNode(
            id = "phong_khach",
            name = "Phòng Khách",
            location = "Tầng 1 - Phòng chính",
            ipAddress = "192.168.1.107",
            macAddress = "24:6F:28:B4:7A:3E",
            wifiRssi = -48
        ),
        DeviceNode(
            id = "xuong_san_xuat",
            name = "Xưởng Gia Công",
            location = "Khu B - Nhà xưởng",
            ipAddress = "192.168.1.108",
            macAddress = "24:6F:28:B4:7A:4F",
            wifiRssi = -62
        )
    )

    private val _selectedDevice = MutableStateFlow(devices.first())
    val selectedDevice: StateFlow<DeviceNode> = _selectedDevice.asStateFlow()

    // Dữ liệu đo đạc thực tế nhận từ cảm biến kết nối vào ESP32
    private val _sensorData = MutableStateFlow(
        SensorData(
            deviceId = "phong_bep",
            deviceName = "Phòng Bếp (ESP32)",
            temperature = 28.5,
            humidity = 65.0,
            gasSmokeLevel = 180,
            flameStatus = false,
            alarmState = AlarmState.NORMAL,
            timestamp = System.currentTimeMillis()
        )
    )
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<AlertRecord>>(
        listOf(
            AlertRecord(
                id = "alt-001",
                deviceId = "phong_bep",
                deviceName = "Phòng Bếp",
                alarmState = AlarmState.SMOKE_WARNING,
                temperature = 42.0,
                gasSmokeLevel = 460,
                flameStatus = false,
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                isAcknowledged = true,
                acknowledgedBy = "Chủ hộ",
                note = "Khói nhẹ khi nấu nướng, đã thông gió"
            )
        )
    )
    val alertHistory: StateFlow<List<AlertRecord>> = _alertHistory.asStateFlow()

    // Điều hướng UI
    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: History, 2: Devices, 3: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _showAlertDetail = MutableStateFlow(false)
    val showAlertDetail: StateFlow<Boolean> = _showAlertDetail.asStateFlow()

    // Cấu hình kết nối phần cứng ESP32 & Firebase Realtime Database
    val connectionMode = MutableStateFlow(Esp32ConnectionMode.FIREBASE_RTDB)
    val esp32IpAddress = MutableStateFlow(devices.first().ipAddress)
    val firebaseDbUrl = MutableStateFlow("https://doan-baochay-esp32-default-rtdb.firebaseio.com/")

    // Trạng thái truyền thông telemetry trực tiếp
    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Đang kết nối mạch ESP32...")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private val _isHardwareConnected = MutableStateFlow(true)
    val isHardwareConnected: StateFlow<Boolean> = _isHardwareConnected.asStateFlow()

    // Cấu hình ngưỡng cảnh báo
    var tempThreshold = MutableStateFlow(60.0)
    var gasThreshold = MutableStateFlow(400)
    var enableVibration = MutableStateFlow(true)
    var enableSound = MutableStateFlow(true)
    var autoPollingEnabled = MutableStateFlow(true)

    private var pollingJob: Job? = null
    private var lastRecordedAlertTimestamp: Long = 0L

    init {
        startHardwarePolling()
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun selectDevice(device: DeviceNode) {
        _selectedDevice.value = device
        esp32IpAddress.value = device.ipAddress
        _sensorData.update {
            it.copy(
                deviceId = device.id,
                deviceName = "${device.name} (ESP32)",
                timestamp = System.currentTimeMillis()
            )
        }
        // Kích hoạt đọc ngay từ mạch mới chọn
        fetchLiveDataNow()
    }

    fun setConnectionMode(mode: Esp32ConnectionMode) {
        connectionMode.value = mode
        fetchLiveDataNow()
    }

    fun updateEsp32Ip(ip: String) {
        esp32IpAddress.value = ip.trim()
        fetchLiveDataNow()
    }

    fun updateFirebaseUrl(url: String) {
        firebaseDbUrl.value = url.trim()
        fetchLiveDataNow()
    }

    fun toggleAutoPolling(enable: Boolean) {
        autoPollingEnabled.value = enable
        if (enable) {
            startHardwarePolling()
        } else {
            pollingJob?.cancel()
            _connectionStatusMessage.value = "Tạm dừng tự động đồng bộ (Chế độ thủ công)"
        }
    }

    fun openAlertDetail() {
        _showAlertDetail.value = true
    }

    fun closeAlertDetail() {
        _showAlertDetail.value = false
    }

    // Đọc dữ liệu đo đạc thực tế từ mạch vi điều khiển ESP32 ngay lập tức
    fun fetchLiveDataNow() {
        viewModelScope.launch {
            fetchEsp32Telemetry()
        }
    }

    // Gửi lệnh tắt còi đến phần cứng ESP32 thực tế
    fun muteBuzzer() {
        viewModelScope.launch {
            val device = _selectedDevice.value
            _sensorData.update {
                it.copy(buzzerActive = false, ledActive = false)
            }

            val result = esp32Repository.muteBuzzerOnHardware(
                mode = connectionMode.value,
                ipAddress = esp32IpAddress.value,
                firebaseBaseUrl = firebaseDbUrl.value,
                deviceId = device.id
            )

            result.onSuccess { msg ->
                _connectionStatusMessage.value = "✓ $msg"
            }.onFailure { err ->
                _connectionStatusMessage.value = "Lưu ý phần cứng: ${err.localizedMessage}"
            }
        }
    }

    fun acknowledgeAlert(note: String = "Người dùng xác nhận an toàn") {
        val current = _sensorData.value
        _sensorData.update {
            it.copy(
                acknowledged = true,
                acknowledgedBy = "Chủ hộ (Mobile App)",
                acknowledgedAt = System.currentTimeMillis(),
                buzzerActive = false
            )
        }

        muteBuzzer()

        if (current.alarmState != AlarmState.NORMAL) {
            val record = AlertRecord(
                id = "alt-${UUID.randomUUID().toString().take(6)}",
                deviceId = current.deviceId,
                deviceName = current.deviceName,
                alarmState = current.alarmState,
                temperature = current.temperature,
                gasSmokeLevel = current.gasSmokeLevel,
                flameStatus = current.flameStatus,
                timestamp = System.currentTimeMillis(),
                isAcknowledged = true,
                acknowledgedBy = "Chủ hộ (Mobile App)",
                note = note
            )
            _alertHistory.update { listOf(record) + it }
        }
    }

    private fun startHardwarePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (autoPollingEnabled.value) {
                    fetchEsp32Telemetry()
                }
                delay(2000) // Đọc định kỳ mỗi 2 giây từ cảm biến ESP32
            }
        }
    }

    private suspend fun fetchEsp32Telemetry() {
        _isFetching.value = true
        val currentDevice = _selectedDevice.value
        val result = esp32Repository.fetchSensorData(
            mode = connectionMode.value,
            ipAddress = esp32IpAddress.value,
            firebaseBaseUrl = firebaseDbUrl.value,
            deviceId = currentDevice.id,
            deviceName = currentDevice.name,
            tempThreshold = tempThreshold.value,
            gasThreshold = gasThreshold.value
        )
        _isFetching.value = false

        _lastLatencyMs.value = result.latencyMs
        _lastSyncTimestamp.value = System.currentTimeMillis()

        if (result.isSuccess) {
            _isHardwareConnected.value = true
            _sensorData.value = result.sensorData
            _connectionStatusMessage.value = "Đã nhận gói đo ESP32 (${result.latencyMs}ms)"

            // Ghi nhận cảnh báo mới vào lịch sử nếu phát hiện khói hoặc cháy
            if (result.sensorData.alarmState != AlarmState.NORMAL &&
                result.sensorData.alarmState != AlarmState.DISCONNECTED
            ) {
                val now = System.currentTimeMillis()
                // Giới hạn ghi log lặp lại trong vòng 30 giây
                if (now - lastRecordedAlertTimestamp > 30000) {
                    lastRecordedAlertTimestamp = now
                    val noteText = when (result.sensorData.alarmState) {
                        AlarmState.FIRE_EMERGENCY -> "CẢNH BÁO: Phát hiện ngọn lửa hoặc nhiệt độ nguy hiểm từ cảm biến ESP32!"
                        AlarmState.SMOKE_WARNING -> "CẢNH BÁO: Cảm biến MQ-2 đo nồng độ khói/gas vượt ngưỡng an toàn!"
                        else -> "Báo động từ phần cứng ESP32"
                    }
                    val newAlert = AlertRecord(
                        id = "alt-${UUID.randomUUID().toString().take(6)}",
                        deviceId = result.sensorData.deviceId,
                        deviceName = result.sensorData.deviceName,
                        alarmState = result.sensorData.alarmState,
                        temperature = result.sensorData.temperature,
                        gasSmokeLevel = result.sensorData.gasSmokeLevel,
                        flameStatus = result.sensorData.flameStatus,
                        timestamp = now,
                        isAcknowledged = false,
                        note = noteText
                    )
                    _alertHistory.update { listOf(newAlert) + it }
                }
            }
        } else {
            _isHardwareConnected.value = false
            _connectionStatusMessage.value = result.errorMessage ?: "Không kết nối được ESP32"
            // Khi mất kết nối phần cứng, chuyển trạng thái hiển thị sang DISCONNECTED
            _sensorData.update {
                it.copy(
                    alarmState = AlarmState.DISCONNECTED,
                    buzzerActive = false,
                    ledActive = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
