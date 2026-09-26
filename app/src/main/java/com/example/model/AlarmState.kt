package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusDisconnectedContainer
import com.example.ui.theme.StatusEmergency
import com.example.ui.theme.StatusEmergencyContainer
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusNormalContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer

enum class AlarmState(
    val id: String,
    val displayName: String,
    val color: Color,
    val containerColor: Color,
    val symbol: String,
    val icon: ImageVector,
    val description: String
) {
    NORMAL(
        id = "Binh thuong",
        displayName = "Bình thường",
        color = StatusNormal,
        containerColor = StatusNormalContainer,
        symbol = "✓",
        icon = Icons.Default.CheckCircle,
        description = "Môi trường ổn định, các chỉ số trong ngưỡng an toàn."
    ),
    SMOKE_WARNING(
        id = "Canh bao khoi",
        displayName = "Cảnh báo khói",
        color = StatusWarning,
        containerColor = StatusWarningContainer,
        symbol = "!",
        icon = Icons.Default.Warning,
        description = "Phát hiện khói hoặc nồng độ khí gas tăng cao bất thường!"
    ),
    FIRE_EMERGENCY(
        id = "Bao chay khan cap",
        displayName = "Báo cháy khẩn cấp",
        color = StatusEmergency,
        containerColor = StatusEmergencyContainer,
        symbol = "🔥",
        icon = Icons.Default.LocalFireDepartment,
        description = "CẢNH BÁO NGUY HIỂM! Phát hiện ngọn lửa hoặc nhiệt độ & khói vượt mức!"
    ),
    DISCONNECTED(
        id = "Mat ket noi",
        displayName = "Mất kết nối",
        color = StatusDisconnected,
        containerColor = StatusDisconnectedContainer,
        symbol = "✕",
        icon = Icons.Default.WifiOff,
        description = "Không nhận được tín hiệu cảm biến từ ESP32."
    );

    companion object {
        fun fromString(state: String?): AlarmState {
            return when (state?.trim()?.lowercase()) {
                "binh thuong", "bình thường", "normal" -> NORMAL
                "canh bao khoi", "cảnh báo khói", "smoke_warning", "warning" -> SMOKE_WARNING
                "bao chay khan cap", "báo cháy khẩn cấp", "khan cap", "emergency", "fire" -> FIRE_EMERGENCY
                "mat ket noi", "mất kết nối", "disconnected", "offline" -> DISCONNECTED
                else -> NORMAL
            }
        }
    }
}
