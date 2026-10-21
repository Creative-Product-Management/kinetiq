package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KinetiqEmerald
import kotlin.math.roundToInt

@Composable
fun WeeklyBarChart(
    dayRates: List<Float>, // size 7: Mon..Sun
    habitColor: Color = KinetiqEmerald,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "DAY-OF-WEEK PERFORMANCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            labels.forEachIndexed { index, label ->
                val rate = dayRates.getOrElse(index) { 0f }.coerceIn(0f, 1f)
                val percent = (rate * 100).roundToInt()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight().weight(1f)
                ) {
                    Text(
                        text = if (percent > 0) "$percent%" else "",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar Track
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(70.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E2633)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val barHeightFraction = rate.coerceAtLeast(0.04f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (rate > 0f) habitColor else Color.Transparent)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
