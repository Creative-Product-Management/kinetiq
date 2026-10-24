package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitModality
import com.example.domain.EditorKind
import com.example.domain.MeasurableLogStyle

private enum class CreateTypeStep {
    CHOOSE_KIND,
    CHECK_IN_QUESTION,
    MEASURABLE_STYLE
}

/**
 * Multi-step sheet that locks habit type before the details editor opens.
 * Check-in → question (maps to title). Measurable → Amount / Count / Timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitTypeSheet(
    onDismiss: () -> Unit,
    onComplete: (modality: HabitModality, initialCheckInQuestion: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(CreateTypeStep.CHOOSE_KIND) }
    var kind by remember { mutableStateOf<EditorKind?>(null) }
    var checkInQuestion by remember { mutableStateOf("") }
    var logStyle by remember { mutableStateOf<MeasurableLogStyle?>(null) }

    val chromeAccent = MaterialTheme.colorScheme.primary
    val chromeOnAccent = MaterialTheme.colorScheme.onPrimary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("create_type_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            when (step) {
                CreateTypeStep.CHOOSE_KIND -> {
                    Text(
                        text = "New habit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How do you want to track it? You cannot change this later.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeChoiceChip(
                            label = "Check-in",
                            subtitle = "Simple yes / no",
                            selected = kind == EditorKind.CHECK_IN,
                            accent = chromeAccent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_kind_CHECK_IN"),
                            onClick = { kind = EditorKind.CHECK_IN }
                        )
                        TypeChoiceChip(
                            label = "Measurable",
                            subtitle = "Amount, count, or time",
                            selected = kind == EditorKind.MEASURABLE,
                            accent = chromeAccent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_kind_MEASURABLE"),
                            onClick = { kind = EditorKind.MEASURABLE }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            when (kind) {
                                EditorKind.CHECK_IN -> step = CreateTypeStep.CHECK_IN_QUESTION
                                EditorKind.MEASURABLE -> step = CreateTypeStep.MEASURABLE_STYLE
                                null -> Unit
                            }
                        },
                        enabled = kind != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = chromeAccent,
                            contentColor = chromeOnAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_type_continue")
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }

                CreateTypeStep.CHECK_IN_QUESTION -> {
                    Text(
                        text = "Check-in question",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Asked in check-in reminders. Your habit title is set on the next screen.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = checkInQuestion,
                        onValueChange = { checkInQuestion = it },
                        label = { Text("Question") },
                        placeholder = {
                            Text(
                                "e.g., Did you meditate today?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = chromeAccent,
                            focusedLabelColor = chromeAccent,
                            cursorColor = chromeAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_checkin_question")
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { step = CreateTypeStep.CHOOSE_KIND },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                        Button(
                            onClick = {
                                onComplete(HabitModality.YES_NO, checkInQuestion.trim())
                            },
                            enabled = checkInQuestion.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = chromeAccent,
                                contentColor = chromeOnAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_type_continue")
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                CreateTypeStep.MEASURABLE_STYLE -> {
                    Text(
                        text = "How I log",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pick one logging style. You cannot change this later.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MeasurableLogStyle.entries.forEach { style ->
                            val label = when (style) {
                                MeasurableLogStyle.AMOUNT -> "Amount"
                                MeasurableLogStyle.COUNT -> "Count"
                                MeasurableLogStyle.TIMER -> "Timer"
                            }
                            val subtitle = when (style) {
                                MeasurableLogStyle.AMOUNT -> "ml, pages…"
                                MeasurableLogStyle.COUNT -> "taps / reps"
                                MeasurableLogStyle.TIMER -> "minutes"
                            }
                            TypeChoiceChip(
                                label = label,
                                subtitle = subtitle,
                                selected = logStyle == style,
                                accent = chromeAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_style_${style.name}"),
                                onClick = { logStyle = style }
                            )
                        }
                    }
                    val hint = when (logStyle) {
                        MeasurableLogStyle.AMOUNT -> "Track an amount each day, e.g. 2500 ml of water."
                        MeasurableLogStyle.COUNT -> "Count repeats. Each tap adds 1 until the target is met."
                        MeasurableLogStyle.TIMER -> "Track time in minutes with the built-in timer."
                        null -> ""
                    }
                    if (hint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { step = CreateTypeStep.CHOOSE_KIND },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                        Button(
                            onClick = {
                                val style = logStyle ?: return@Button
                                onComplete(style.toModality(), "")
                            },
                            enabled = logStyle != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = chromeAccent,
                                contentColor = chromeOnAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_type_continue")
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChoiceChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) accent.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) accent else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
