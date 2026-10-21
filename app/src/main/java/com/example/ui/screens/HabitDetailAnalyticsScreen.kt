package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.analytics.HabitAnalyticsResult
import com.example.ui.components.HeatmapMatrix
import com.example.ui.components.KinetiqGauge
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.FloorGold
import com.example.ui.theme.MissedRed
import com.example.ui.theme.KinetiqEmerald
import com.example.ui.theme.RestShieldBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailAnalyticsScreen(
    habit: HabitEntity,
    records: List<HabitRecordEntity>,
    onBack: () -> Unit,
    onEditHabit: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    analyticsResult: HabitAnalyticsResult? = null,
    restShieldEnabled: Boolean = true,
    weekStartsOnMonday: Boolean = true
) {
    BackHandler { onBack() }

    val habitColor = remember(habit.colorHex) { com.example.ui.theme.parseHabitColor(habit.colorHex) }

    val analytics: HabitAnalyticsResult = analyticsResult ?: remember(habit, records, restShieldEnabled, weekStartsOnMonday) {
        HabitAnalyticsEngine.calculateAnalytics(habit, records, restShieldEnabled = restShieldEnabled, weekStartsOnMonday = weekStartsOnMonday)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = habit.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (habit.cueTrigger.isNotBlank()) {
                            Text(
                                text = "⚡ ${habit.cueTrigger}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditHabit, modifier = Modifier.testTag("edit_habit_btn")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Habit",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Circular Kinetiq Momentum Gauge Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KinetiqGauge(
                        strengthPercent = analytics.habitStrengthPercent,
                        trajectory = analytics.trajectory,
                        habitColor = habitColor
                    )
                }
            }

            // Key Metrics 2x2 Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "CURRENT STREAK",
                        value = "${analytics.currentStreak}",
                        unit = "days active",
                        color = habitColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "BEST STREAK",
                        value = "${analytics.bestStreak}",
                        unit = "days record",
                        color = KinetiqEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "TARGET COMPLETED",
                        value = "${analytics.totalTargetCompletions}",
                        unit = "times achieved",
                        color = habitColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "FLOOR SUSTAINED",
                        value = "${analytics.totalFloorSustains}",
                        unit = "rescued streaks",
                        color = FloorGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Heatmap Matrix
            item {
                HeatmapMatrix(
                    cells = analytics.heatmapCells,
                    habitColor = habitColor,
                    weekStartsOnMonday = weekStartsOnMonday
                )
            }

            // Weekly consistency bar chart
            item {
                WeeklyBarChart(
                    dayRates = analytics.dayOfWeekRates,
                    habitColor = habitColor
                )
            }

            // Friction Diagnostics (if any obstacles were logged)
            if (analytics.frictionCounts.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "FRICTION & OBSTACLE DIAGNOSTICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MissedRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Root causes identified when habits were skipped:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        analytics.frictionCounts.forEach { (tag, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MoodBad,
                                        contentDescription = null,
                                        tint = MissedRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = tag, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(
                                    text = "$count times",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recent History Log List
            item {
                Text(
                    text = "RECENT ACTIVITY LOG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val recentRecords = records.sortedByDescending { it.dateString }.take(15)
            if (recentRecords.isEmpty()) {
                item {
                    Text(
                        text = "No recorded logs yet for this habit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentRecords, key = { it.id }) { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val badgeColor = when {
                                rec.isCompleted -> habitColor
                                rec.isFloorSustained -> FloorGold
                                rec.frictionTag != null -> MissedRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    rec.isCompleted -> Icon(Icons.Default.Check, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                    rec.isFloorSustained -> Icon(Icons.Default.Shield, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                    else -> Icon(Icons.Default.MoodBad, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = rec.dateString, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                val desc = when {
                                    rec.isCompleted -> "Target completed"
                                    rec.isFloorSustained -> "Sustained minimum floor"
                                    rec.frictionTag != null -> "Friction: ${rec.frictionTag}"
                                    else -> "Logged entry"
                                }
                                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(
                            onClick = { onDeleteRecord(rec.dateString) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = unit,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
