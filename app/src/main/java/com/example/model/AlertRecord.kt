package com.example.model

data class AlertRecord(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val alarmState: AlarmState,
    val temperature: Double,
    val gasSmokeLevel: Int,
    val flameStatus: Boolean,
    val timestamp: Long,
    val isAcknowledged: Boolean = false,
    val acknowledgedBy: String? = null,
    val note: String = ""
)
