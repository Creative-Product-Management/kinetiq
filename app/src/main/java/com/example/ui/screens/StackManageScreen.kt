package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.model.RoutineStackEntity
import com.example.ui.theme.MissedRed
import com.example.ui.viewmodel.KineticViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StackManageScreen(
    viewModel: KineticViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val stacks by viewModel.allStacks.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RoutineStackEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RoutineStackEntity?>(null) }

    val safeInsets = WindowInsets.safeDrawing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routine stacks", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stacks_back_btn")) {
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
                modifier = Modifier.testTag("add_stack_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add stack", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = safeInsets,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (stacks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No stacks yet. Add one to group habits into routines.",
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
                items(stacks, key = { it.id }) { stack ->
                    val usage = viewModel.habitCountForStack(stack.id)
                    StackRow(
                        stack = stack,
                        usageCount = usage,
                        onEdit = { editing = stack },
                        onDelete = { pendingDelete = stack }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showAdd) {
        StackEditDialog(
            initial = RoutineStackEntity(name = "", description = ""),
            title = "New stack",
            onDismiss = { showAdd = false },
            onSave = { viewModel.saveStack(it); showAdd = false }
        )
    }
    editing?.let { stack ->
        StackEditDialog(
            initial = stack,
            title = "Edit stack",
            onDismiss = { editing = null },
            onSave = { viewModel.saveStack(it); editing = null }
        )
    }
    pendingDelete?.let { stack ->
        val usage = viewModel.habitCountForStack(stack.id)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${stack.name}?") },
            text = {
                Text(
                    if (usage > 0) "Habits in this stack become unstacked ($usage habit${if (usage == 1) "" else "s"})."
                    else "This stack will be removed from the library."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteStack(stack); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StackRow(
    stack: RoutineStackEntity,
    usageCount: Int,
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
            .testTag("stack_row_${stack.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stack.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (stack.description.isNotBlank()) {
                Text(stack.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (usageCount == 0) "Unused" else "Used by $usageCount habit${if (usageCount == 1) "" else "s"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.testTag("stack_edit_${stack.id}")) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("stack_delete_${stack.id}")) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MissedRed)
        }
    }
}

@Composable
private fun StackEditDialog(
    initial: RoutineStackEntity,
    title: String,
    onDismiss: () -> Unit,
    onSave: (RoutineStackEntity) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var description by remember(initial.id) { mutableStateOf(initial.description) }
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
                    modifier = Modifier.fillMaxWidth().testTag("stack_name_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("stack_description_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onSave(initial.copy(name = trimmed, description = description.trim()))
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("stack_save_btn")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
