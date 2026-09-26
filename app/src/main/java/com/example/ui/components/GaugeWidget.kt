package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GaugeBlue
import com.example.ui.theme.GaugeBlueTrack
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeGreenTrack
import com.example.ui.theme.StatusEmergency
import com.example.ui.theme.StatusWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Half-Circle (180°) Analog & Digital Gauge Widget.
 * Designed to match the dashboard interface with distinct Green (Temperature)
 * and Blue (Gas/Smoke) semi-circle arcs, lighter background fills, needle pointer,
 * and bold central digital readings.
 */
@Composable
fun GaugeWidget(
    title: String,
    value: Float,
    unit: String,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    warningThreshold: Float = 50f,
    dangerThreshold: Float = 70f,
    icon: ImageVector? = null,
    gaugeColor: Color = GaugeGreen,
    trackColor: Color = GaugeGreenTrack,
    modifier: Modifier = Modifier
) {
    val clampedValue = value.coerceIn(minValue, maxValue)
    val fraction = ((clampedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)

    // Semi-circle sweep is 180 degrees, starting at 180° (west/left) and ending at 360°/0° (east/right)
    val startAngle = 180f
    val totalSweepAngle = 180f
    val targetAngle = startAngle + (fraction * totalSweepAngle)

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 650),
        label = "semi_gauge_needle_angle"
    )

    // Active color: if reaches critical danger, can highlight with emergency tone
    val activeColor = when {
        clampedValue >= dangerThreshold -> StatusEmergency
        clampedValue >= warningThreshold -> StatusWarning
        else -> gaugeColor
    }

    Card(
        modifier = modifier
            .testTag("gauge_${title.lowercase().replace(" ", "_")}")
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = activeColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Half-Circle Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val bottomPadding = 12.dp.toPx()
                    val centerX = size.width / 2f
                    val centerY = size.height - bottomPadding

                    // Determine max radius fitting inside canvas
                    val maxRadiusByWidth = (size.width - strokeWidth * 2) / 2f
                    val maxRadiusByHeight = size.height - strokeWidth - bottomPadding
                    val radius = minOf(maxRadiusByWidth, maxRadiusByHeight)

                    val topLeft = Offset(centerX - radius, centerY - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)

                    // 1. Semi-Circle Background Track (Lighter fill)
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = totalSweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Semi-Circle Active Progress Arc
                    val activeSweep = (fraction * totalSweepAngle).coerceIn(1f, totalSweepAngle)
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 3. Tick Marks along 180° semi-circle (5 intervals)
                    val ticks = 5
                    for (i in 0..ticks) {
                        val tickRatio = i.toFloat() / ticks
                        val tickDeg = startAngle + tickRatio * totalSweepAngle
                        val tickRad = tickDeg * (PI / 180f)

                        val innerR = radius - strokeWidth * 0.95f
                        val outerR = radius - strokeWidth * 0.25f

                        val startP = Offset(
                            x = centerX + innerR * cos(tickRad).toFloat(),
                            y = centerY + innerR * sin(tickRad).toFloat()
                        )
                        val endP = Offset(
                            x = centerX + outerR * cos(tickRad).toFloat(),
                            y = centerY + outerR * sin(tickRad).toFloat()
                        )

                        drawLine(
                            color = Color(0xFF9E9E9E),
                            start = startP,
                            end = endP,
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // 4. Needle Pointer pointing from bottom center towards the arc
                    val needleRad = animatedAngle * (PI / 180f).toFloat()
                    val needleLength = radius * 0.72f
                    val needleEnd = Offset(
                        x = centerX + needleLength * cos(needleRad),
                        y = centerY + needleLength * sin(needleRad)
                    )

                    // Needle Line
                    drawLine(
                        color = activeColor,
                        start = Offset(centerX, centerY),
                        end = needleEnd,
                        strokeWidth = 3.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Pivot Outer Circle
                    drawCircle(
                        color = activeColor,
                        radius = 7.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )

                    // Pivot Inner White Dot
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                }

                // Central Digital Reading positioned right above the pivot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 18.dp)
                ) {
                    val displayValue = if (value % 1 == 0f) "${value.toInt()}" else String.format("%.1f", value)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp
                            ),
                            color = activeColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Min and Max scale markers at bottom corners
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${minValue.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${maxValue.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
