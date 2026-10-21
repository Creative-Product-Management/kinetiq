package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.ui.theme.parseHabitColor
import kotlinx.coroutines.delay

/**
 * Wall-clock timer: elapsed time is derived from timestamps, so backgrounding,
 * recomposition, or process recreation cannot inflate or lose a session.
 * Completing with zero elapsed seconds is impossible.
 */
@Composable
fun ActiveTimerDialog(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onFinishTimer: (durationSeconds: Int) -> Unit
) {
    val targetSeconds = (habit.targetValue * 60).toInt().coerceAtLeast(60)
    var accumulatedMs by rememberSaveable { mutableLongStateOf(0L) }
    var runningSinceMs by rememberSaveable { mutableLongStateOf(-1L) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var nowMs by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var bonusSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(isRunning) {
        while (isRunning) { nowMs = System.currentTimeMillis(); delay(250L) }
    }

    val elapsedMs = accumulatedMs + if (isRunning && runningSinceMs > 0) (nowMs - runningSinceMs).coerceAtLeast(0) else 0L
    val elapsedSeconds = (elapsedMs / 1000).toInt()
    val totalTarget = targetSeconds + bonusSeconds
    val secondsLeft = (totalTarget - elapsedSeconds).coerceAtLeast(0)
    val progress = if (totalTarget > 0) (elapsedSeconds.toFloat() / totalTarget).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")
    val timeFormatted = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
    val habitColor = parseHabitColor(habit.colorHex)

    fun pause() {
        if (isRunning && runningSinceMs > 0) accumulatedMs += (System.currentTimeMillis() - runningSinceMs).coerceAtLeast(0)
        isRunning = false; runningSinceMs = -1L
    }
    fun start() { runningSinceMs = System.currentTimeMillis(); nowMs = runningSinceMs; isRunning = true }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = habit.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Target: ${habit.targetValue.toInt()} mins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(180.dp), color = MaterialTheme.colorScheme.outline, strokeWidth = 8.dp)
                    CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.size(180.dp), color = habitColor, strokeWidth = 8.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = timeFormatted, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (isRunning) "FOCUSING" else if (secondsLeft == 0 && elapsedSeconds > 0) "GOAL MET!" else "PAUSED",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (secondsLeft == 0 && elapsedSeconds > 0) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { pause(); accumulatedMs = 0L; bonusSeconds = 0 },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Timer", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(modifier = Modifier.width(18.dp))
                    IconButton(
                        onClick = { if (isRunning) pause() else start() },
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(habitColor).testTag("timer_toggle_btn")
                    ) {
                        Icon(imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isRunning) "Pause" else "Start", tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    IconButton(
                        onClick = { bonusSeconds += 60 },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) { Text(text = "+1m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { pause(); onFinishTimer(elapsedSeconds) },
                    enabled = elapsedSeconds > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                    modifier = Modifier.fillMaxWidth().testTag("finish_log_timer_btn")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (elapsedSeconds > 0) "Complete & Log ${elapsedSeconds}s" else "Start the timer to log", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}
