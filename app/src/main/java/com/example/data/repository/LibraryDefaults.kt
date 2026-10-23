package com.example.data.repository

import com.example.data.model.Category
import com.example.data.model.RoutineStackEntity

/** Built-in category and stack library shipped with the app (muted accents). */
object LibraryDefaults {

    val categories: List<Category> = listOf(
        Category(name = "Health", colorHex = "#2F6F5E", iconName = "Favorite", orderIndex = 0),
        Category(name = "Fitness", colorHex = "#6B4E31", iconName = "FitnessCenter", orderIndex = 1),
        Category(name = "Mindfulness", colorHex = "#4A5568", iconName = "SelfImprovement", orderIndex = 2),
        Category(name = "Focus", colorHex = "#3D4F6F", iconName = "Psychology", orderIndex = 3),
        Category(name = "Learning", colorHex = "#4A5D4E", iconName = "MenuBook", orderIndex = 4),
        Category(name = "Creative", colorHex = "#6B3F5A", iconName = "Brush", orderIndex = 5),
        Category(name = "Social", colorHex = "#3F5A6B", iconName = "People", orderIndex = 6),
        Category(name = "Personal", colorHex = "#5A5346", iconName = "Home", orderIndex = 7)
    )

    val stacks: List<RoutineStackEntity> = listOf(
        RoutineStackEntity(name = "Morning Routine", description = "Start the day with intention", iconName = "WbSunny", colorHex = "#6B5A3D"),
        RoutineStackEntity(name = "Evening Wind-down", description = "Close the day calmly", iconName = "NightsStay", colorHex = "#3D4A6B"),
        RoutineStackEntity(name = "Deep Work", description = "Protected focus blocks", iconName = "Bolt", colorHex = "#4A3D6B")
    )
}
