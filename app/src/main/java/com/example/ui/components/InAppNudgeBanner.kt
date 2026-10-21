package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.FloorGold
import com.example.ui.theme.KinetiqEmerald
import com.example.ui.theme.KinetiqViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/**
 * How long each hero slide stays fully visible before auto-advancing.
 * Long enough to read a title, subtitle, and action (roughly 35-45 words
 * at a comfortable reading pace) without feeling rushed.
 */
const val HERO_AUTO_ADVANCE_DELAY_MILLIS = 8_000L

/**
 * Fixed height of every hero slide, sized for the worst case
 * (2-line title + 3-line subtitle + action button + padding).
 * Combined with line caps and a bottom-pinned action button, every
 * slide occupies exactly this height so the dashboard below never jumps.
 */
val HERO_SLIDE_HEIGHT = 244.dp

/** Next slide index, wrapping from the last slide back to the first (endless loop). */
fun nextHeroPage(currentPage: Int, pageCount: Int): Int =
    if (pageCount <= 1) 0 else (currentPage + 1) % pageCount

/**
 * Hero carousel for today's nudges. Swipeable via [HorizontalPager] (horizontal
 * drag/fling works even inside the vertically scrolling dashboard), auto-advances
 * every [HERO_AUTO_ADVANCE_DELAY_MILLIS] while the user is not interacting, loops
 * from the last slide back to the first, and lets the user jump between nudges
 * with the dots or dismiss the current one for today.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppNudgeHero(
    nudges: List<InAppNudgeData>,
    onActionClick: ((InAppNudgeData) -> Unit)? = null,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nudges.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { nudges.size }
    val scope = rememberCoroutineScope()

    // Auto-advance with a full dwell per slide. Keyed on settledPage so the
    // reading timer restarts after every manual swipe or dot tap as well as
    // after each automatic transition, and skipped while a swipe is in flight.
    LaunchedEffect(pagerState.settledPage, nudges.size) {
        if (nudges.size > 1) {
            delay(HERO_AUTO_ADVANCE_DELAY_MILLIS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(nextHeroPage(pagerState.settledPage, nudges.size))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().testTag("hero_carousel")) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(HERO_SLIDE_HEIGHT),
            key = { index -> nudges[index].id }
        ) { page ->
            val pageNudge = nudges[page]
            InAppNudgeBanner(
                nudge = pageNudge,
                onActionClick = { onActionClick?.invoke(pageNudge) },
                onDismiss = { onDismiss(pageNudge.id) },
                modifier = Modifier.height(HERO_SLIDE_HEIGHT)
            )
        }

        if (nudges.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                nudges.forEachIndexed { index, item ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .semantics { contentDescription = "Show insight ${index + 1}: ${item.title}" }
                            .testTag("hero_dot_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                        )
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
                KinetiqViolet
            )
            NudgeType.REST_SHIELD -> Triple(
                Icons.Default.Shield,
                Brush.horizontalGradient(listOf(Color(0xFF1A3A66), Color(0xFF12243F))),
                Color(0xFF3B82F6)
            )
            NudgeType.MOMENTUM_BOOST -> Triple(
                Icons.Default.ElectricBolt,
                Brush.horizontalGradient(listOf(Color(0xFF146348), Color(0xFF0E3328))),
                KinetiqEmerald
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
            Column(modifier = Modifier.fillMaxSize()) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = nudge.subtitle,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                    lineHeight = 21.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Absorbs any height difference between slides so the action
                // button is bottom-pinned at the same position on every slide.
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(12.dp))

                nudge.actionLabel?.let { label ->
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
