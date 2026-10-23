package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.ui.theme.DEFAULT_HABIT_COLOR_HEX
import com.example.ui.theme.HABIT_COLOR_PRESETS
import com.example.ui.theme.MissedRed
import com.example.ui.theme.parseHabitColor
import com.example.ui.viewmodel.KineticViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    viewModel: KineticViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    val safeInsets = WindowInsets.safeDrawing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("categories_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = safeInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = safeInsets,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No categories yet. Add one to organize habits in the editor.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                itemsIndexed(categories, key = { _, c -> c.id }) { index, category ->
                    val usage = viewModel.habitCountForCategory(category.id)
                    CategoryRow(
                        category = category,
                        usageCount = usage,
                        canMoveUp = index > 0,
                        canMoveDown = index < categories.lastIndex,
                        onMoveUp = {
                            val reordered = categories.toMutableList()
                            reordered[index] = reordered[index - 1].also { reordered[index - 1] = reordered[index] }
                            viewModel.reorderCategories(reordered)
                        },
                        onMoveDown = {
                            val reordered = categories.toMutableList()
                            reordered[index] = reordered[index + 1].also { reordered[index + 1] = reordered[index] }
                            viewModel.reorderCategories(reordered)
                        },
                        onEdit = { editing = category },
                        onDelete = { pendingDelete = category }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showAdd) {
        CategoryEditDialog(
            initial = Category(name = "", colorHex = DEFAULT_HABIT_COLOR_HEX, orderIndex = categories.size),
            title = "New category",
            onDismiss = { showAdd = false },
            onSave = { viewModel.saveCategory(it); showAdd = false }
        )
    }
    editing?.let { cat ->
        CategoryEditDialog(
            initial = cat,
            title = "Edit category",
            onDismiss = { editing = null },
            onSave = { viewModel.saveCategory(it); editing = null }
        )
    }
    pendingDelete?.let { cat ->
        val usage = viewModel.habitCountForCategory(cat.id)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${cat.name}?") },
            text = {
                Text(
                    if (usage > 0) "Habits in this category become uncategorized ($usage habit${if (usage == 1) "" else "s"})."
                    else "This category will be removed from the library."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCategory(cat); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    usageCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("category_row_${category.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(parseHabitColor(category.colorHex))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (usageCount == 0) "Unused" else "Used by $usageCount habit${if (usageCount == 1) "" else "s"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.testTag("category_up_${category.id}")) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.testTag("category_down_${category.id}")) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
        }
        IconButton(onClick = onEdit, modifier = Modifier.testTag("category_edit_${category.id}")) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("category_delete_${category.id}")) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MissedRed)
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initial: Category,
    title: String,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var colorHex by remember(initial.id) { mutableStateOf(initial.colorHex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )
                Spacer(Modifier.height(12.dp))
                Text("Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HABIT_COLOR_PRESETS.take(6).forEach { hex ->
                        val selected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHabitColor(hex))
                                .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onSave(initial.copy(name = trimmed, colorHex = colorHex))
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("category_save_btn")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
