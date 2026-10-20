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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

enum class NudgeType {
    STACK_PRIMED,
    REST_SHIELD,
    MOMENTUM_BOOST,
    INSIGHT
}

data class InAppNudgeData(
    val type: NudgeType,
    val title: String,
    val subtitle: String,
    val actionLabel: String? = null
)

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
                Brush.horizontalGradient(listOf(Color(0xFF2E1A47), Color(0xFF1E1430))),
                OrbitViolet
            )
            NudgeType.REST_SHIELD -> Triple(
                Icons.Default.Shield,
                Brush.horizontalGradient(listOf(Color(0xFF132742), Color(0xFF101C2E))),
                Color(0xFF3B82F6)
            )
            NudgeType.MOMENTUM_BOOST -> Triple(
                Icons.Default.ElectricBolt,
                Brush.horizontalGradient(listOf(Color(0xFF0C3827), Color(0xFF0F261D))),
                OrbitEmerald
            )
            NudgeType.INSIGHT -> Triple(
                Icons.Default.TipsAndUpdates,
                Brush.horizontalGradient(listOf(Color(0xFF382B0D), Color(0xFF261D09))),
                FloorGold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(gradient)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Nudge Icon",
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nudge.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = nudge.subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    nudge.actionLabel?.let { label ->
                        Spacer(modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = borderColor,
                            modifier = Modifier.clickable { onActionClick?.invoke() }
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Nudge",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
