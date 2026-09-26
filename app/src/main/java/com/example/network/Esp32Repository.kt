package com.example.network

import com.example.model.AlarmState
import com.example.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class Esp32ConnectionMode(val label: String) {
    DIRECT_ESP32_WIFI("WiFi ESP32 trực tiếp"),
    FIREBASE_RTDB("Firebase Realtime DB")
}

data class Esp32TelemetryResult(
    val sensorData: SensorData,
    val latencyMs: Long,
    val sourceUrl: String,
    val isSuccess: Boolean,
    val rawJson: String? = null,
    val errorMessage: String? = null
)

class Esp32Repository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3000, TimeUnit.MILLISECONDS)
        .readTimeout(3000, TimeUnit.MILLISECONDS)
        .writeTimeout(3000, TimeUnit.MILLISECONDS)
        .build()
) {

    suspend fun fetchSensorData(
        mode: Esp32ConnectionMode,
        ipAddress: String,
        firebaseBaseUrl: String,
        deviceId: String,
        deviceName: String,
        tempThreshold: Double,
        gasThreshold: Int
    ): Esp32TelemetryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val targetUrl = when (mode) {
            Esp32ConnectionMode.DIRECT_ESP32_WIFI -> {
                val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
                "http://$cleanIp/data"
            }
            Esp32ConnectionMode.FIREBASE_RTDB -> {
                val cleanBase = firebaseBaseUrl.trim().removeSuffix("/")
                "$cleanBase/$deviceId.json"
            }
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    return@withContext Esp32TelemetryResult(
                        sensorData = SensorData(
                            deviceId = deviceId,
                            deviceName = deviceName,
                            alarmState = AlarmState.DISCONNECTED,
                            timestamp = System.currentTimeMillis()
                        ),
                        latencyMs = latency,
                        sourceUrl = targetUrl,
                        isSuccess = false,
                        errorMessage = "Lỗi HTTP ${response.code}: ${response.message}"
                    )
                }

                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isBlank() || bodyString == "null") {
                    return@withContext Esp32TelemetryResult(
                        sensorData = SensorData(
                            deviceId = deviceId,
                            deviceName = deviceName,
                            alarmState = AlarmState.DISCONNECTED,
                            timestamp = System.currentTimeMillis()
                        ),
                        latencyMs = latency,
                        sourceUrl = targetUrl,
                        isSuccess = false,
                        errorMessage = "Không có dữ liệu trả về từ ESP32 (null payload)"
                    )
                }

                val parsed = parseSensorJson(
                    jsonString = bodyString,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    tempThreshold = tempThreshold,
                    gasThreshold = gasThreshold
                )

                Esp32TelemetryResult(
                    sensorData = parsed,
                    latencyMs = latency,
                    sourceUrl = targetUrl,
                    isSuccess = true,
                    rawJson = bodyString
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val message = when (e) {
                is java.net.SocketTimeoutException -> "Hết thời gian chờ kết nối (Timeout > 3s)"
                is java.net.ConnectException -> "Không thể kết nối tới $targetUrl (Kiểm tra IP & WiFi ESP32)"
                is IOException -> "Lỗi mạng I/O: ${e.localizedMessage}"
                else -> "Lỗi kết nối: ${e.localizedMessage}"
            }

            Esp32TelemetryResult(
                sensorData = SensorData(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    alarmState = AlarmState.DISCONNECTED,
                    timestamp = System.currentTimeMillis()
                ),
                latencyMs = latency,
                sourceUrl = targetUrl,
                isSuccess = false,
                errorMessage = message
            )
        }
    }

    suspend fun muteBuzzerOnHardware(
        mode: Esp32ConnectionMode,
        ipAddress: String,
        firebaseBaseUrl: String,
        deviceId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (mode) {
                Esp32ConnectionMode.DIRECT_ESP32_WIFI -> {
                    val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
                    val url = "http://$cleanIp/buzzer"
                    val jsonBody = JSONObject().apply {
                        put("buzzer", false)
                        put("state", 0)
                        put("command", "MUTE")
                    }.toString()

                    val request = Request.Builder()
                        .url(url)
                        .post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Result.success("Đã gửi lệnh tắt còi đến ESP32 ($url)")
                        } else {
                            // Fallback to GET /buzzer?state=0 for simple Arduino web servers
                            val getUrl = "http://$cleanIp/buzzer?state=0"
                            val getRequest = Request.Builder().url(getUrl).get().build()
                            client.newCall(getRequest).execute().use { getResp ->
                                if (getResp.isSuccessful) {
                                    Result.success("Đã tắt còi thành công qua GET ($getUrl)")
                                } else {
                                    Result.failure(Exception("Lỗi HTTP ${response.code} khi gửi lệnh tắt còi ESP32"))
                                }
                            }
                        }
                    }
                }
                Esp32ConnectionMode.FIREBASE_RTDB -> {
                    val cleanBase = firebaseBaseUrl.trim().removeSuffix("/")
                    val url = "$cleanBase/$deviceId.json"
                    val patchBody = JSONObject().apply {
                        put("buzzer_active", false)
                        put("acknowledged", true)
                        put("acknowledged_at", System.currentTimeMillis())
                    }.toString()

                    val request = Request.Builder()
                        .url(url)
                        .patch(patchBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Result.success("Đã cập nhật trạng thái tắt còi trên Firebase RTDB")
                        } else {
                            Result.failure(Exception("Lỗi Firebase RTDB: ${response.code}"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSensorJson(
        jsonString: String,
        deviceId: String,
        deviceName: String,
        tempThreshold: Double,
        gasThreshold: Int
    ): SensorData {
        val root = JSONObject(jsonString)

        // Handle possible wrapper: {"sensor_data": {...}} or direct object
        val obj = if (root.has("sensor_data") && root.optJSONObject("sensor_data") != null) {
            root.getJSONObject("sensor_data")
        } else {
            root
        }

        // Temperature (DHT11/DHT22)
        val temp = when {
            obj.has("temperature") -> obj.optDouble("temperature", 28.0)
            obj.has("temp") -> obj.optDouble("temp", 28.0)
            obj.has("nhiet_do") -> obj.optDouble("nhiet_do", 28.0)
            obj.has("t") -> obj.optDouble("t", 28.0)
            else -> 28.0
        }

        // Humidity (DHT11/DHT22)
        val hum = when {
            obj.has("humidity") -> obj.optDouble("humidity", 65.0)
            obj.has("hum") -> obj.optDouble("hum", 65.0)
            obj.has("do_am") -> obj.optDouble("do_am", 65.0)
            obj.has("h") -> obj.optDouble("h", 65.0)
            else -> 65.0
        }

        // Gas / Smoke Level (MQ-2)
        val gas = when {
            obj.has("gas_smoke_level") -> obj.optInt("gas_smoke_level", 180)
            obj.has("gasSmokeLevel") -> obj.optInt("gasSmokeLevel", 180)
            obj.has("gas") -> obj.optInt("gas", 180)
            obj.has("smoke") -> obj.optInt("smoke", 180)
            obj.has("mq2") -> obj.optInt("mq2", 180)
            obj.has("g") -> obj.optInt("g", 180)
            else -> 180
        }

        // Flame sensor (IR DOUT)
        val flame = when {
            obj.has("flame_status") -> parseBoolean(obj.opt("flame_status"))
            obj.has("flameStatus") -> parseBoolean(obj.opt("flameStatus"))
            obj.has("flame") -> parseBoolean(obj.opt("flame"))
            obj.has("lua") -> parseBoolean(obj.opt("lua"))
            obj.has("fire") -> parseBoolean(obj.opt("fire"))
            obj.has("f") -> parseBoolean(obj.opt("f"))
            else -> false
        }

        // Buzzer active state
        val buzzer = when {
            obj.has("buzzer_active") -> parseBoolean(obj.opt("buzzer_active"))
            obj.has("buzzerActive") -> parseBoolean(obj.opt("buzzerActive"))
            obj.has("buzzer") -> parseBoolean(obj.opt("buzzer"))
            obj.has("coi") -> parseBoolean(obj.opt("coi"))
            else -> false
        }

        // LED active state
        val led = when {
            obj.has("led_active") -> parseBoolean(obj.opt("led_active"))
            obj.has("ledActive") -> parseBoolean(obj.opt("ledActive"))
            obj.has("led") -> parseBoolean(obj.opt("led"))
            else -> false
        }

        // Timestamp
        val ts = when {
            obj.has("timestamp") -> obj.optLong("timestamp", System.currentTimeMillis())
            obj.has("time") -> obj.optLong("time", System.currentTimeMillis())
            else -> System.currentTimeMillis()
        }

        // Alarm State
        val rawAlarmStateStr = when {
            obj.has("alarm_state") -> obj.optString("alarm_state", "")
            obj.has("alarmState") -> obj.optString("alarmState", "")
            obj.has("status") -> obj.optString("status", "")
            obj.has("state") -> obj.optString("state", "")
            else -> ""
        }

        val calculatedState = if (rawAlarmStateStr.isNotBlank()) {
            AlarmState.fromString(rawAlarmStateStr)
        } else {
            // Compute based on hardware reading and thresholds
            when {
                flame || temp >= tempThreshold || (gas >= gasThreshold && temp >= 45.0) -> AlarmState.FIRE_EMERGENCY
                gas >= gasThreshold -> AlarmState.SMOKE_WARNING
                else -> AlarmState.NORMAL
            }
        }

        return SensorData(
            deviceId = deviceId,
            deviceName = deviceName,
            temperature = Math.round(temp * 10.0) / 10.0,
            humidity = Math.round(hum * 10.0) / 10.0,
            gasSmokeLevel = gas,
            flameStatus = flame,
            alarmState = calculatedState,
            timestamp = ts,
            buzzerActive = buzzer || calculatedState == AlarmState.FIRE_EMERGENCY,
            ledActive = led || calculatedState != AlarmState.NORMAL,
            acknowledged = obj.optBoolean("acknowledged", false),
            acknowledgedBy = obj.optString("acknowledged_by", null),
            acknowledgedAt = if (obj.has("acknowledged_at")) obj.optLong("acknowledged_at") else null
        )
    }

    private fun parseBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1" || value.equals("danger", ignoreCase = true)
            else -> false
        }
    }
}
