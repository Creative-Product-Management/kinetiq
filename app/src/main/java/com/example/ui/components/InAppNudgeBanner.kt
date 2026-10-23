package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FloorGold
import com.example.ui.theme.KineticEmerald
import com.example.ui.theme.KineticViolet
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
    val actionLabel: String? = null,
    val targetHabitId: Long? = null
)

/** How long each hero slide stays fully visible before auto-advancing. */
const val HERO_AUTO_ADVANCE_DELAY_MILLIS = 8_000L

/** Fixed height of the lean hero strip shown below the app bar. */
val HERO_STRIP_HEIGHT = 60.dp

/** Next slide index, wrapping from the last slide back to the first (endless loop). */
fun nextHeroPage(currentPage: Int, pageCount: Int): Int =
    if (pageCount <= 1) 0 else (currentPage + 1) % pageCount

/** Icon, gradient, and accent colour for a slide type. Shared by strip slides. */
internal fun nudgeVisuals(type: NudgeType): Triple<ImageVector, Brush, Color> = when (type) {
    NudgeType.STACK_PRIMED -> Triple(
        Icons.Default.ElectricBolt,
        Brush.horizontalGradient(listOf(Color(0xFF4C2A78), Color(0xFF2A1848))),
        KineticViolet
    )
    NudgeType.REST_SHIELD -> Triple(
        Icons.Default.Shield,
        Brush.horizontalGradient(listOf(Color(0xFF1A3A66), Color(0xFF12243F))),
        Color(0xFF3B82F6)
    )
    NudgeType.MOMENTUM_BOOST -> Triple(
        Icons.Default.ElectricBolt,
        Brush.horizontalGradient(listOf(Color(0xFF146348), Color(0xFF0E3328))),
        KineticEmerald
    )
    NudgeType.INSIGHT -> Triple(
        Icons.Default.TipsAndUpdates,
        Brush.horizontalGradient(listOf(Color(0xFF5A4314), Color(0xFF3A2C0C))),
        FloorGold
    )
}

/**
 * Lean hero strip below the app bar: a slim (60dp) rotating banner — icon,
 * one-line title and subtitle, tap for the slide action, dismiss for today,
 * tiny dots when there is more than one slide. Auto-advances every
 * [HERO_AUTO_ADVANCE_DELAY_MILLIS] while not being interacted with.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppNudgeStrip(
    nudges: List<InAppNudgeData>,
    onActionClick: ((InAppNudgeData) -> Unit)? = null,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nudges.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { nudges.size }

    LaunchedEffect(pagerState.settledPage, nudges.size) {
        if (nudges.size > 1) {
            delay(HERO_AUTO_ADVANCE_DELAY_MILLIS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(nextHeroPage(pagerState.settledPage, nudges.size))
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(HERO_STRIP_HEIGHT).testTag("hero_strip")) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> nudges[index].id }
        ) { page ->
            val nudge = nudges[page]
            val (icon, gradient, accent) = nudgeVisuals(nudge.type)
            val shape = RoundedCornerShape(16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(gradient)
                    .border(1.dp, accent.copy(alpha = 0.85f), shape)
                    .clickable { onActionClick?.invoke(nudge) }
                    .padding(start = 12.dp, end = 4.dp)
                    .semantics { contentDescription = "${nudge.title}. ${nudge.subtitle}" }
                    .testTag("hero_slide_${nudge.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f))
                        .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nudge.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nudge.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onDismiss(nudge.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
