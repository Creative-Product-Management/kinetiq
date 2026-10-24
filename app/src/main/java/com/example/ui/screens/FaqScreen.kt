package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FaqItem(val category: String, val question: String, val answer: String)

val FAQ_ITEMS: List<FaqItem> = listOf(
    // Getting started
    FaqItem("Getting started", "How do I create my first habit?", "Tap the **+ button** on the Home screen.\n\n• Choose **Check-in** or **Measurable**\n• For Check-in, enter a short **check-in question** (used in reminders); for Measurable, pick Amount, Count, or Timer\n• Then set a **title** and the rest of the details (target/floor for measurable habits, cue, reminder, and so on)\n\nTap **Save** and the habit appears on your home screen."),
    FaqItem("Getting started", "What are the habit types?", "Habits are either **Check-in** or **Measurable**.\n\n• **Check-in** — a simple yes/no habit. It has a display **title** plus a separate **check-in question** used in reminders\n• **Measurable · Amount** — tracks a quantity, such as pages, glasses, or kilometres\n• **Measurable · Count** — counts taps or repetitions, such as sets, sprints, or calls\n• **Measurable · Timer** — tracks minutes using the built-in timer"),
    FaqItem("Getting started", "Can I change a habit's type later?", "**No.** Check-in vs Measurable (and Amount / Count / Timer) is locked when you create the habit, so past logs and Momentum stay consistent.\n\nTo track differently, **delete** the habit and create a new one with the type you want. Prefer **Archive** if you only want to hide it and keep history."),
    FaqItem("Getting started", "What is a cue?", "A cue is the trigger that reminds you to start — for example, \"After breakfast\" or \"When I sit at my desk\". Adding a cue makes the habit easier to remember because it attaches the new habit to something you already do. Your cue shows on the right side of each habit card."),
    FaqItem("Getting started", "What is a routine stack?", "A stack is a **group of habits you do together in sequence**, such as a Morning Launchpad.\n\nGrouping habits into a stack makes a routine easier to follow than scattered individual habits.\n\nOn Home, use the **view menu beside the bell** to switch between Group by Stack and Flat List."),

    // Logging
    FaqItem("Logging", "How do I log a habit for today?", "Tap **today's circle** on the habit card.\n\n• **Yes/No** — toggles done / not done\n• **Tally** — adds one count with each tap\n• **Timer** — opens the built-in timer\n• **Number** — a tap logs your target; once logged, tapping again opens more options"),
    FaqItem("Logging", "How do I log a floor, a custom amount, or a past day?", "**Long-press** any day circle on the habit card.\n\nThe quick-log menu lets you:\n\n• Log the target or the floor\n• Enter a custom amount\n• Record what got in the way (friction)\n• Clear the day\n\nThis also works for the past days shown on the card."),
    FaqItem("Logging", "What is the difference between Target and Floor?", "**Target** is the full goal for the day.\n\n**Floor** is the smallest version that still counts as showing up — for example, Target 30 reps, Floor 10 reps.\n\nOn a hard day, logging the floor keeps your streak and momentum alive. It earns slightly less momentum than a full target day."),
    FaqItem("Logging", "Can I edit or delete a habit?", "Tap the **three-dot menu** on any habit card (or long-press the card):\n\n• **Edit** — change title, target, floor, cue, schedule, and appearance (tracking type stays locked)\n• **Archive** — hides the habit but keeps its history; restore it anytime from Settings\n• **Delete** — permanent, and asks for confirmation first"),
    FaqItem("Logging", "How do I change the order of habits on Home?", "Tap the view icon beside the bell on Home and choose **Sort**.\n\n• **Manual order** — long-press and drag habits into your preferred order; it is saved automatically\n• **Momentum Score ↓** — habits are auto-ordered highest Momentum first; tap it again to flip to lowest first (↑), and the order updates as scores change\n\nYou can switch between the two at any time; your manual order is kept when you switch back."),

    // Momentum
    FaqItem("Momentum score", "What is the Kinetic Momentum score?", "Momentum is a **0–100% score** on each habit's detail screen. It answers: *\"How strong is this habit right now?\"*\n\nIt is **not** your all-time completion rate. Recent days count far more than older days, so it rises as you build consistency and falls gently when you miss days."),
    FaqItem("Momentum score", "How is Momentum calculated?", "Each day gets a value:\n\n• **Target hit** — 1.0\n• **Floor hit** — 0.65\n• **Missed day** — 0\n\nThe score then updates daily:\n\n**Momentum = 0.93 × yesterday's Momentum + 0.07 × today's value**\n\nOnly the **last 90 days** count, starting from the day you created the habit — earlier days never count against you. If you backfill past days on a new habit, those logged days **do** count, just as if you had logged them at the time."),
    FaqItem("Momentum score", "How fast does Momentum grow?", "Logging your target every day from a new habit:\n\n• **1 day** — ~7%\n• **1 week** — ~40%\n• **2 weeks** — ~64%\n• **1 month** — ~89%\n• **2 months** — ~99%\n\nA floor-only streak tops out around **65%**, because floor days are worth 0.65."),
    FaqItem("Momentum score", "What happens when I miss a day?", "A miss does **not** reset you to zero.\n\nYour score is multiplied by **0.93** for each missed day, so it roughly halves every 10 missed days.\n\nBecause recent days matter most, about **two solid weeks** of logging rebuilds most of a decayed score."),
    FaqItem("Momentum score", "Do rest days hurt my Momentum?", "**No.** On a scheduled rest day (Rest Shield), your score is **frozen** — no gain and no loss — as long as the Rest Day Shield is on in Settings.\n\nIf you log on a rest day anyway, it counts normally."),
    FaqItem("Momentum score", "What do Accelerating, Rebuilding, Steady High, and Forming Routine mean?", "These labels compare today's Momentum with **7 days ago**:\n\n• **Accelerating** — up more than 3 points\n• **Rebuilding** — down more than 3 points\n• **Steady High** — holding at 70% or above\n• **Forming Routine** — everything else, while the habit is still building"),
    FaqItem("Momentum score", "Is Momentum based on science?", "**The maths is standard; the exact recipe is Kinetic's own.**\n\n• It uses an *exponentially weighted moving average*, a standard statistical technique that weights recent behaviour more heavily.\n• Floor credit follows the \"shrink the habit\" principle; gentle decay avoids all-or-nothing abandonment after a slip.\n• Habit-formation research (Lally et al., 2010) found automaticity took on average about **66 days**, and that missing a single occasion did not materially affect habit formation.\n\nHowever, Kinetic's exact weights (0.93, floor = 0.65, 70% threshold) are tuned for the app.\n\n**Momentum is a helpful feedback mirror — not a clinical or scientific measure of habit strength.**"),
    FaqItem("Momentum score", "What does not affect Momentum?", "• Exceeding your target (150% counts the same as 100%)\n• Partial progress below your floor — it counts the same as a miss, so always log at least the floor\n• Your all-time streak or total completions\n• The time of day you log\n• Cues and friction tags"),

    // Rest Shield & streaks
    FaqItem("Rest Shield & streaks", "What is Rest Shield?", "Rest Shield lets you **schedule planned rest days** for a habit — for example, weekends off.\n\nOn rest days:\n\n• Your streak is **preserved**\n• Your Momentum is **frozen**\n• The day does **not** count as a miss\n\nSet rest days when creating or editing a habit, and turn the Rest Day Shield on or off in Settings."),
    FaqItem("Rest Shield & streaks", "How do streaks work?", "Your **current streak** counts consecutive active days where you hit your target or floor.\n\n• **Rest days** preserve the streak without adding to it — they neither extend nor break it\n• **Missing an active day** ends the current streak\n• Your **best streak** is always kept"),

    // Reminders
    FaqItem("Reminders", "How do reminders work?", "Set a reminder time when creating or editing a habit.\n\n• Routines sends a **local notification** on scheduled days, skipping rest days\n• For **Check-in** habits, the notification leads with your **check-in question** (the title appears as supporting text)\n• From the notification, you can mark the habit done\n• Reminders are **re-armed after your phone restarts**"),
    FaqItem("Reminders", "I am not receiving notifications. What should I check?", "Tap the **bell** on Home.\n\n• If app notifications are off, choose **Open Settings** and turn notifications on for Routines\n• Use **Send Test** to check that notifications arrive\n\nExact reminder timing also depends on your device's battery and alarm settings."),

    // Privacy & data
    FaqItem("Privacy & data", "Where is my data stored?", "**Everything stays on this device.**\n\nRoutines has no accounts, no servers, and no sync — and the app does not use the internet.\n\nYour habits, logs, and settings are stored locally on your phone."),
    FaqItem("Privacy & data", "How do I back up or move my data?", "In **Settings**:\n\n• Use **Export** to save an encrypted backup file (.kqbak) protected by a passphrase you choose\n• Use **Import** on any device with Routines and enter the same passphrase to restore\n\nKeep your passphrase safe — without it, a backup cannot be opened."),
    FaqItem("Privacy & data", "I archived a habit by mistake. Can I get it back?", "**Yes — archiving is reversible.**\n\nOpen Settings, find the **Archived** section, and restore the habit. Its history comes back with it.\n\nOnly permanent deletion from the confirmation dialog removes a habit and its logs for good.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expandedQuestion by remember { mutableStateOf<String?>(null) }
    val categories = remember { FAQ_ITEMS.map { it.category }.distinct() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAQs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("faq_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("faq_screen")
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Text(
                    text = "How Routines works, in plain language.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            categories.forEach { category ->
                item(key = "faq_category_$category") {
                    Text(
                        text = category.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                    )
                }
                items(FAQ_ITEMS.filter { it.category == category }, key = { it.question }) { faq ->
                    val expanded = expandedQuestion == faq.question
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { expandedQuestion = if (expanded) null else faq.question }
                            .padding(14.dp)
                            .semantics { contentDescription = faq.question }
                            .testTag("faq_item_${faq.question.hashCode()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = faq.question,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse answer" else "Expand answer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                FaqAnswerText(answer = faq.answer)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * Minimal FAQ formatting so answers read as structured text, not a blob.
 * Supported markup (kept deliberately tiny):
 *  - Blank line ("\\n\\n") starts a new paragraph
 *  - Lines starting with "• " render as a bullet list
 *  - **text** renders bold inline
 */
@Composable
private fun FaqAnswerText(answer: String) {
    val blocks = remember(answer) { answer.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() } }
    Column {
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty() && lines.all { it.startsWith("• ") }) {
                Column {
                    lines.forEach { line ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = faqInlineText(line.removePrefix("• ")),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = faqInlineText(lines.joinToString(" ")),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Parses **bold** spans into an [AnnotatedString]; everything else is plain. */
private fun faqInlineText(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val start = text.indexOf("**", cursor)
        if (start < 0) {
            append(text.substring(cursor))
            break
        }
        val end = text.indexOf("**", start + 2)
        if (end < 0) {
            append(text.substring(cursor))
            break
        }
        append(text.substring(cursor, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 2, end))
        }
        cursor = end + 2
    }
}
