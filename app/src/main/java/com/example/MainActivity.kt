package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.OrbitDashboardScreen
import com.example.ui.theme.OrbitHabitTheme
import com.example.ui.viewmodel.OrbitViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: OrbitViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            OrbitHabitTheme(themeMode = themeMode) {
                // Check if opened from notification with selected habit
                LaunchedEffect(intent) {
                    val habitId = intent?.getLongExtra("selected_habit_id", -1L) ?: -1L
                    if (habitId > 0) {
                        viewModel.allHabits.value.firstOrNull { it.id == habitId }?.let { habit ->
                            viewModel.openHabitDetail(habit)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OrbitDashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
