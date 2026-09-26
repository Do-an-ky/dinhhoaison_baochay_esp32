package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.AlarmState
import com.example.ui.theme.StatusEmergency
import kotlinx.coroutines.delay

@Composable
fun EmergencyStrobeOverlay(
    alarmState: AlarmState,
    enableVibration: Boolean = true
) {
    if (alarmState != AlarmState.FIRE_EMERGENCY) return

    val context = LocalContext.current

    // Infinite transition for red flashing strobe effect
    val infiniteTransition = rememberInfiniteTransition(label = "strobe_trans")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_alpha"
    )

    // Trigger vibration pattern when fire emergency is active
    LaunchedEffect(alarmState, enableVibration) {
        if (!enableVibration) return@LaunchedEffect

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        while (true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            } catch (_: Exception) {
            }
            delay(1600)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StatusEmergency.copy(alpha = alpha))
    )
}
