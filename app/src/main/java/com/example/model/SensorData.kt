package com.example.model

data class SensorData(
    val deviceId: String = "phong_bep",
    val deviceName: String = "Phòng Bếp (ESP32_01)",
    val temperature: Double = 28.5,
    val humidity: Double = 65.0,
    val gasSmokeLevel: Int = 180,
    val flameStatus: Boolean = false,
    val alarmState: AlarmState = AlarmState.NORMAL,
    val timestamp: Long = System.currentTimeMillis(),
    val buzzerActive: Boolean = false,
    val ledActive: Boolean = false,
    val acknowledged: Boolean = false,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null
) {
    val isSafe: Boolean
        get() = alarmState == AlarmState.NORMAL

    val isWarning: Boolean
        get() = alarmState == AlarmState.SMOKE_WARNING

    val isEmergency: Boolean
        get() = alarmState == AlarmState.FIRE_EMERGENCY

    val isOffline: Boolean
        get() = alarmState == AlarmState.DISCONNECTED
}
