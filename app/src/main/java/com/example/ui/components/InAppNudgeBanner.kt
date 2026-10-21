package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FloorGold
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class NudgeType {
    STACK_PRIMED,
    REST_SHIELD,
    MOMENTUM_BOOST,
    INSIGHT
}

data class InAppNudgeData(
    val id: String,
    val type: NudgeType,
    val title: String,
    val subtitle: String,
    val actionLabel: String? = null
)

/**
 * Hero carousel for today's nudges. Shows one nudge at a time as a large
 * highlighted card, auto-advances every 6 seconds, and lets the user jump
 * between nudges with the dots or dismiss the current one.
 */
@Composable
fun InAppNudgeHero(
    nudges: List<InAppNudgeData>,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nudges.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, nudges.lastIndex)
    val nudge = nudges[safeIndex]

    LaunchedEffect(safeIndex, nudges.size) {
        if (nudges.size > 1) {
            delay(6000)
            currentIndex = (safeIndex + 1) % nudges.size
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        InAppNudgeBanner(
            nudge = nudge,
            onActionClick = onActionClick,
            onDismiss = { onDismiss(nudge.id) }
        )

        if (nudges.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                nudges.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == safeIndex) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                            )
                            .clickable { currentIndex = index }
                    )
                    if (index < nudges.lastIndex) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InAppNudgeBanner(
    nudge: InAppNudgeData?,
    onActionClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = nudge != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (nudge == null) return@AnimatedVisibility

        val (icon: ImageVector, gradient: Brush, borderColor: Color) = when (nudge.type) {
            NudgeType.STACK_PRIMED -> Triple(
                Icons.Default.ElectricBolt,
                Brush.horizontalGradient(listOf(Color(0xFF4C2A78), Color(0xFF2A1848))),
                OrbitViolet
            )
            NudgeType.REST_SHIELD -> Triple(
                Icons.Default.Shield,
                Brush.horizontalGradient(listOf(Color(0xFF1A3A66), Color(0xFF12243F))),
                Color(0xFF3B82F6)
            )
            NudgeType.MOMENTUM_BOOST -> Triple(
                Icons.Default.ElectricBolt,
                Brush.horizontalGradient(listOf(Color(0xFF146348), Color(0xFF0E3328))),
                OrbitEmerald
            )
            NudgeType.INSIGHT -> Triple(
                Icons.Default.TipsAndUpdates,
                Brush.horizontalGradient(listOf(Color(0xFF5A4314), Color(0xFF3A2C0C))),
                FloorGold
            )
        }

        val shape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(gradient)
                .border(1.5.dp, borderColor.copy(alpha = 0.85f), shape)
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.22f))
                            .border(1.dp, borderColor.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TODAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = borderColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nudge.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = nudge.subtitle,
                    fontSize = 15.sp,
                    color = TextPrimary.copy(alpha = 0.92f),
                    lineHeight = 21.sp
                )

                nudge.actionLabel?.let { label ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onActionClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = borderColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
