package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CategoryManageScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HabitDetailAnalyticsScreen
import com.example.ui.screens.HabitEditorSheet
import com.example.ui.screens.HabitSortScreen
import com.example.ui.screens.KineticDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StackManageScreen
import com.example.ui.theme.KineticRoutinesTheme
import com.example.ui.viewmodel.KineticViewModel

class MainActivity : ComponentActivity() {

    private var viewModelRef: KineticViewModel? = null
    private var pendingDeepLinkId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)

        setContent {
            val app = application as KineticRoutinesApplication
            val viewModel: KineticViewModel = viewModel(factory = KineticViewModel.Factory(app.container))
            viewModelRef = viewModel
            LaunchedEffect(viewModel) {
                if (pendingDeepLinkId > 0) { viewModel.onNotificationDeepLink(pendingDeepLinkId); pendingDeepLinkId = -1L }
            }
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            KineticRoutinesTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val pendingDetail by viewModel.selectedHabitForDetail.collectAsStateWithLifecycle()

                    // Notification deep link: navigate once the habit is resolved.
                    LaunchedEffect(pendingDetail) {
                        val habit = pendingDetail
                        if (habit != null && navController.currentDestination?.route == "dashboard") {
                            navController.navigate("detail/${habit.id}")
                        }
                    }

                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            KineticDashboardScreen(
                                viewModel = viewModel,
                                onOpenHabitDetail = { id -> navController.navigate("detail/$id") },
                                onOpenEditor = { id -> navController.navigate(if (id == null) "editor" else "editor?habitId=$id") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenSort = { navController.navigate("sort") },
                                onOpenFaqs = { navController.navigate("faqs") },
                                onOpenAbout = { navController.navigate("about") }
                            )
                        }
                        composable("sort") {
                            HabitSortScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable("faqs") {
                            FaqScreen(onBack = { navController.popBackStack() })
                        }
                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = "detail/{habitId}",
                            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
                        ) { entry ->
                            val habitId = entry.arguments?.getLong("habitId") ?: -1L
                            LaunchedEffect(habitId) { viewModel.selectHabitById(habitId) }
                            val habit by viewModel.selectedHabitForDetail.collectAsStateWithLifecycle()
                            val records by viewModel.detailRecords.collectAsStateWithLifecycle()
                            val analytics by viewModel.selectedAnalytics.collectAsStateWithLifecycle()
                            val restShield by viewModel.restShieldEnabled.collectAsStateWithLifecycle()
                            val weekMonday by viewModel.weekStartsOnMonday.collectAsStateWithLifecycle()
                            if (habit != null && habit!!.id == habitId) {
                                HabitDetailAnalyticsScreen(
                                    habit = habit!!,
                                    records = records,
                                    onBack = { viewModel.closeHabitDetail(); navController.popBackStack() },
                                    onEditHabit = { navController.navigate("editor?habitId=${habit!!.id}") },
                                    onDeleteRecord = { dateStr -> viewModel.clearLog(habit!!.id, dateStr) },
                                    analyticsResult = analytics,
                                    restShieldEnabled = restShield,
                                    weekStartsOnMonday = weekMonday
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            }
                        }
                        composable(
                            route = "editor?habitId={habitId}",
                            arguments = listOf(navArgument("habitId") { type = NavType.LongType; defaultValue = -1L })
                        ) { entry ->
                            val habitId = entry.arguments?.getLong("habitId") ?: -1L
                            val habits by viewModel.allHabits.collectAsStateWithLifecycle()
                            val archived by viewModel.archivedHabits.collectAsStateWithLifecycle()
                            val categories by viewModel.allCategories.collectAsStateWithLifecycle()
                            val stacks by viewModel.allStacks.collectAsStateWithLifecycle()
                            val floorPct by viewModel.defaultFloorPercent.collectAsStateWithLifecycle()
                            val habit = if (habitId > 0) (habits + archived).firstOrNull { it.id == habitId } else null
                            HabitEditorSheet(
                                habit = habit,
                                onDismiss = { viewModel.closeEditor(); navController.popBackStack() },
                                onSave = { updated -> viewModel.saveHabit(updated); navController.popBackStack() },
                                onDelete = { toDelete -> viewModel.deleteHabit(toDelete); navController.popBackStack("dashboard", inclusive = false) },
                                onArchive = { toArchive -> viewModel.archiveHabit(toArchive); navController.popBackStack("dashboard", inclusive = false) },
                                categories = categories,
                                stacks = stacks,
                                defaultFloorPercent = floorPct,
                                onManageCategories = { navController.navigate("settings/categories") },
                                onManageStacks = { navController.navigate("settings/stacks") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenCategories = { navController.navigate("settings/categories") },
                                onOpenStacks = { navController.navigate("settings/stacks") }
                            )
                        }
                        composable("settings/categories") {
                            CategoryManageScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable("settings/stacks") {
                            StackManageScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val habitId = intent?.getLongExtra("selected_habit_id", -1L) ?: -1L
        if (habitId > 0) {
            val vm = viewModelRef
            if (vm != null) vm.onNotificationDeepLink(habitId) else pendingDeepLinkId = habitId
        }
    }
}
