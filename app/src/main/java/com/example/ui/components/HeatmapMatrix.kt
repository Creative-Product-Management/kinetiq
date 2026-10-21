package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.HeatmapCell
import com.example.domain.analytics.HeatmapCellState
import com.example.ui.theme.FloorGold
import com.example.ui.theme.MissedRed
import com.example.ui.theme.KinetiqEmerald
import com.example.ui.theme.RestShieldBlue

@Composable
fun HeatmapMatrix(
    cells: List<HeatmapCell>,
    habitColor: Color = KinetiqEmerald,
    modifier: Modifier = Modifier,
    weekStartsOnMonday: Boolean = true
) {
    val scrollState = rememberScrollState()
    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }

    LaunchedEffect(cells.size) {
        if (cells.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Group cells into 7 rows (Mon..Sun)
    // cells are 140 days, each week has 7 days
    val weeks = cells.chunked(7)
    val dayLabels = if (weekStartsOnMonday) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LAST 20 WEEKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendIndicator(color = habitColor, label = "Target")
                Spacer(modifier = Modifier.width(8.dp))
                LegendIndicator(color = FloorGold, label = "Floor")
                Spacer(modifier = Modifier.width(8.dp))
                LegendIndicator(color = RestShieldBlue, label = "Shield")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Day label column
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable grid
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            val cellColor = when (cell.state) {
                                HeatmapCellState.FULL_COMPLETED -> habitColor
                                HeatmapCellState.FLOOR_SUSTAINED -> FloorGold
                                HeatmapCellState.REST_SHIELD -> RestShieldBlue
                                HeatmapCellState.MISSED -> MissedRed.copy(alpha = 0.5f)
                                HeatmapCellState.NONE -> Color(0xFF1E2633)
                            }

                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                                    .clickable { selectedCell = cell }
                            )
                        }
                    }
                }
            }
        }

        // Active selection tooltip banner
        selectedCell?.let { cell ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222B3D))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = cell.tooltip,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
