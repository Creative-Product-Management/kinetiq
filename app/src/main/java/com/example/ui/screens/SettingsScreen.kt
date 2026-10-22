package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.areAppNotificationsEnabled
import com.example.ui.components.openAppNotificationSettings
import com.example.ui.theme.FloorGold
import com.example.ui.theme.MissedRed
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.KinetiqViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: KinetiqViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val weekStartsOnMonday by viewModel.weekStartsOnMonday.collectAsStateWithLifecycle()
    val defaultFloorPercent by viewModel.defaultFloorPercent.collectAsStateWithLifecycle()
    val restShieldEnabled by viewModel.restShieldEnabled.collectAsStateWithLifecycle()
    val dailyEveningDigestEnabled by viewModel.dailyEveningDigestEnabled.collectAsStateWithLifecycle()
    val dailyEveningDigestHour by viewModel.dailyEveningDigestHour.collectAsStateWithLifecycle()
    val showHeroSection by viewModel.showHeroSection.collectAsStateWithLifecycle()
    val archivedHabits by viewModel.archivedHabits.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val bytes = viewModel.exportBackup(backupPassphrase.toCharArray())
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open file")
            }.onSuccess { backupStatus = "Backup saved." }
                .onFailure { backupStatus = "Export failed: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read file")
                viewModel.importBackup(bytes, backupPassphrase.toCharArray())
            }.onSuccess { backupStatus = "Backup restored." }
                .onFailure { backupStatus = "Import failed: ${it.message}" }
        }
    }

    val safeInsets = WindowInsets.safeDrawing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = safeInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = safeInsets,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsSection("Appearance") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionButton("Dark", Icons.Default.DarkMode, themeMode == ThemeMode.DARK, { viewModel.setThemeMode(ThemeMode.DARK) }, Modifier.weight(1f).testTag("theme_dark_btn"))
                    ThemeOptionButton("Light", Icons.Default.LightMode, themeMode == ThemeMode.LIGHT, { viewModel.setThemeMode(ThemeMode.LIGHT) }, Modifier.weight(1f).testTag("theme_light_btn"))
                    ThemeOptionButton("System", Icons.Default.SettingsBrightness, themeMode == ThemeMode.SYSTEM, { viewModel.setThemeMode(ThemeMode.SYSTEM) }, Modifier.weight(1f).testTag("theme_system_btn"))
                }
            }

            SettingsSection("Home") {
                SettingsToggleRow(
                    title = "Hero Insights",
                    subtitle = "Top carousel on Home",
                    checked = showHeroSection,
                    onCheckedChange = { viewModel.setShowHeroSection(it) },
                    testTag = "hero_section_toggle"
                )
            }

            SettingsSection("Habits") {
                SettingsToggleRow(
                    title = "Rest Day Shield",
                    subtitle = "Rest days don't break streaks",
                    checked = restShieldEnabled,
                    onCheckedChange = { viewModel.toggleRestShield(it) },
                    testTag = "rest_shield_toggle"
                )
                SettingsDivider()
                Text("Default Floor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("For new habits, on low-energy days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 25, 33, 50).forEach { pct ->
                        val isSelected = defaultFloorPercent == pct
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) FloorGold else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setDefaultFloorPercent(pct) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$pct%", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SettingsDivider()
                SettingsValueRow(
                    title = "Week Starts",
                    value = if (weekStartsOnMonday) "Monday" else "Sunday",
                    onClick = { viewModel.setWeekStartsOnMonday(!weekStartsOnMonday) },
                    testTag = "toggle_week_start_btn"
                )
            }

            SettingsSection("Notifications") {
                SettingsToggleRow(
                    title = "Evening Reminder",
                    subtitle = "Daily check-in nudge",
                    checked = dailyEveningDigestEnabled,
                    onCheckedChange = { viewModel.toggleDailyEveningDigest(it) },
                    testTag = "evening_digest_toggle"
                )
                if (dailyEveningDigestEnabled) {
                    SettingsDivider()
                    SettingsValueRow(
                        title = "Time",
                        value = String.format("%02d:00", dailyEveningDigestHour),
                        onClick = { viewModel.setDailyEveningDigestHour((dailyEveningDigestHour + 1) % 24) },
                        testTag = "digest_hour_btn"
                    )
                    if (!viewModel.canScheduleExactAlarms()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("May be delayed without exact alarms.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SettingsDivider()
                SettingsActionRow(
                    title = "Test Notification",
                    actionLabel = "Send",
                    onClick = {
                        if (areAppNotificationsEnabled(context)) {
                            viewModel.sendTestNotification()
                            Toast.makeText(context, "Test sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            openAppNotificationSettings(context)
                        }
                    },
                    testTag = "settings_send_test_notification_btn"
                )
            }

            SettingsSection("Data") {
                Text("Data stays on this device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = backupPassphrase,
                    onValueChange = { backupPassphrase = it },
                    label = { Text("Backup Passphrase") },
                    supportingText = { Text("At least 8 characters") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("kinetiq-backup.kqbak") },
                        enabled = backupPassphrase.length >= 8,
                        modifier = Modifier.weight(1f).testTag("export_data_btn")
                    ) { Text("Export", fontSize = 12.sp) }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = backupPassphrase.length >= 8,
                        modifier = Modifier.weight(1f).testTag("import_data_btn")
                    ) { Text("Import", fontSize = 12.sp) }
                }
                backupStatus?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SettingsDivider()
                SettingsActionRow(
                    title = "Sample Habit",
                    actionLabel = "Load",
                    onClick = {
                        viewModel.loadSampleHabit()
                        Toast.makeText(context, "Sample habit loaded.", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "reseed_data_btn"
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "All Activity",
                    actionLabel = "Clear",
                    danger = true,
                    onClick = { showResetDialog = true },
                    testTag = "reset_all_records_btn"
                )
            }

            if (archivedHabits.isNotEmpty()) {
                SettingsSection("Archived") {
                    archivedHabits.forEachIndexed { index, habit ->
                        if (index > 0) SettingsDivider()
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(habit.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.restoreHabit(habit) }) { Text("Restore") }
                            TextButton(onClick = { viewModel.deleteHabit(habit) }) { Text("Delete", color = MissedRed) }
                        }
                    }
                }
            }

            Text(
                "Kinetiq ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 24.dp)
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear all activity?") },
            text = { Text("All logs will be deleted. Habits stay.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllRecords()
                        showResetDialog = false
                        Toast.makeText(context, "Activity cleared.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) { Text("Clear", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
    }
}

@Composable
private fun SettingsValueRow(title: String, value: String, onClick: () -> Unit, testTag: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) { Text(value) }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String,
    danger: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) MissedRed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Text(actionLabel, color = if (danger) MissedRed else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
