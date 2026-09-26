package com.example.model

data class DeviceNode(
    val id: String,
    val name: String,
    val location: String,
    val ipAddress: String,
    val macAddress: String,
    val wifiRssi: Int, // e.g. -58 dBm
    val isOnline: Boolean = true,
    val maxTempThreshold: Double = 60.0,
    val maxGasThreshold: Int = 400
)
