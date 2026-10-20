package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FloorGold
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitEmeraldGlow
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OrbitGauge(
    strengthPercent: Int,
    trajectory: String,
    modifier: Modifier = Modifier,
    habitColor: Color = OrbitEmerald
) {
    val animatedPercent = remember { Animatable(0f) }

    LaunchedEffect(strengthPercent) {
        animatedPercent.animateTo(
            targetValue = strengthPercent.toFloat(),
            animationSpec = tween(durationMillis = 900)
        )
    }

    val primaryAccent = if (habitColor != Color.Unspecified) habitColor else OrbitEmerald
    val gradientColors = listOf(
        primaryAccent,
        OrbitViolet,
        OrbitEmeraldGlow
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track
            drawArc(
                color = SurfaceBorder,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc
            val sweep = (animatedPercent.value / 100f) * 270f
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(gradientColors),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${animatedPercent.value.toInt()}%",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "ORBIT MOMENTUM",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            val badgeColor = when {
                strengthPercent >= 70 -> OrbitEmerald
                strengthPercent >= 40 -> FloorGold
                else -> OrbitViolet
            }
            Text(
                text = trajectory.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}
