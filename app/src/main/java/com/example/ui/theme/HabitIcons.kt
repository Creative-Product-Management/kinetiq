package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class HabitIconEntry(val name: String, val vector: ImageVector)

/** Curated Material icons available in the habit editor (stored as [name] strings). */
val HABIT_ICON_CATALOG: List<HabitIconEntry> = listOf(
    HabitIconEntry("SelfImprovement", Icons.Default.SelfImprovement),
    HabitIconEntry("WaterDrop", Icons.Default.WaterDrop),
    HabitIconEntry("FitnessCenter", Icons.Default.FitnessCenter),
    HabitIconEntry("Psychology", Icons.Default.Psychology),
    HabitIconEntry("Timer", Icons.Default.Timer),
    HabitIconEntry("Bolt", Icons.Default.Bolt),
    HabitIconEntry("Favorite", Icons.Default.Favorite),
    HabitIconEntry("MenuBook", Icons.Default.MenuBook),
    HabitIconEntry("AutoStories", Icons.Default.AutoStories),
    HabitIconEntry("Brush", Icons.Default.Brush),
    HabitIconEntry("Create", Icons.Default.Create),
    HabitIconEntry("People", Icons.Default.People),
    HabitIconEntry("Home", Icons.Default.Home),
    HabitIconEntry("WbSunny", Icons.Default.WbSunny),
    HabitIconEntry("NightsStay", Icons.Default.NightsStay),
    HabitIconEntry("Bedtime", Icons.Default.Bedtime),
    HabitIconEntry("Spa", Icons.Default.Spa),
    HabitIconEntry("DirectionsRun", Icons.Default.DirectionsRun),
    HabitIconEntry("DirectionsBike", Icons.Default.DirectionsBike),
    HabitIconEntry("Hiking", Icons.Default.Hiking),
    HabitIconEntry("Pool", Icons.Default.Pool),
    HabitIconEntry("LocalCafe", Icons.Default.LocalCafe),
    HabitIconEntry("Restaurant", Icons.Default.Restaurant),
    HabitIconEntry("Park", Icons.Default.Park),
    HabitIconEntry("Pets", Icons.Default.Pets),
    HabitIconEntry("MusicNote", Icons.Default.MusicNote),
    HabitIconEntry("Headphones", Icons.Default.Headphones),
    HabitIconEntry("Code", Icons.Default.Code),
    HabitIconEntry("Laptop", Icons.Default.Laptop),
    HabitIconEntry("Phone", Icons.Default.Phone),
    HabitIconEntry("Lightbulb", Icons.Default.Lightbulb),
    HabitIconEntry("EditNote", Icons.Default.EditNote),
    HabitIconEntry("CheckCircle", Icons.Default.CheckCircle),
    HabitIconEntry("Flag", Icons.Default.Flag),
    HabitIconEntry("LocalFireDepartment", Icons.Default.LocalFireDepartment),
    HabitIconEntry("Air", Icons.Default.Air),
    HabitIconEntry("Star", Icons.Default.Star)
)

private val iconByName: Map<String, ImageVector> =
    HABIT_ICON_CATALOG.associate { it.name to it.vector }

fun habitIconVector(iconName: String): ImageVector =
    iconByName[iconName] ?: Icons.Default.Star
