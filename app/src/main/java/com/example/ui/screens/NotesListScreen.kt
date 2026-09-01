package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NotesSortOrder
import com.example.ui.viewmodel.NotesUiState
import com.example.ui.viewmodel.NotesViewMode
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    uiState: NotesUiState,
    onBackToFolders: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: (withChecklist: Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onTogglePin: (Long) -> Unit,
    onToggleLock: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onToggleViewMode: () -> Unit,
    onSetSortOrder: (NotesSortOrder) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleNoteSelected: (Long) -> Unit,
    onDeleteSelectedNotes: () -> Unit,
    onMoveSelectedNotes: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var contextMenuNote by remember { mutableStateOf<NoteEntity?>(null) }

    val folderTitle = uiState.currentFolder?.name ?: "Notes"

    // Partition into Pinned vs Others
    val pinnedNotes = remember(uiState.notes) {
        uiState.notes.filter { it.isPinned }
    }
    val unpinnedNotes = remember(uiState.notes) {
        uiState.notes.filter { !it.isPinned }
    }

    // Group unpinned notes by timeframe
    val groupedUnpinnedNotes = remember(unpinnedNotes) {
        groupNotesByDate(unpinnedNotes)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button to Folders
                    IOSBackButton(
                        label = "Folders",
                        onClick = onBackToFolders,
                        modifier = Modifier.testTag("back_to_folders_button")
                    )

                    // Right Actions (... Menu / Done)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uiState.isSelectionMode) {
                            TextButton(
                                onClick = onToggleSelectionMode,
                                modifier = Modifier.testTag("done_selection_button")
                            ) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = IOSYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.testTag("notes_more_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Options",
                                        tint = IOSYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (uiState.viewMode == NotesViewMode.LIST) "View as Grid" else "View as List"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (uiState.viewMode == NotesViewMode.LIST)
                                                    Icons.Outlined.GridView else Icons.Outlined.ViewList,
                                                contentDescription = null,
                                                tint = IOSYellow
                                            )
                                        },
                                        onClick = {
                                            onToggleViewMode()
                                            showMoreMenu = false
                                        },
                                        modifier = Modifier.testTag("toggle_grid_list_menu")
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Select Notes") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = IOSYellow
                                            )
                                        },
                                        onClick = {
                                            onToggleSelectionMode()
                                            showMoreMenu = false
                                        }
                                    )

                                    HorizontalDivider()

                                    Text(
                                        text = "SORT BY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = IOSLightTextSecondary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Date Edited",
                                                fontWeight = if (uiState.sortOrder == NotesSortOrder.DATE_EDITED) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == NotesSortOrder.DATE_EDITED) {
                                                Icon(Icons.Default.Check, null, tint = IOSYellow, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            onSetSortOrder(NotesSortOrder.DATE_EDITED)
                                            showMoreMenu = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Date Created",
                                                fontWeight = if (uiState.sortOrder == NotesSortOrder.DATE_CREATED) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == NotesSortOrder.DATE_CREATED) {
                                                Icon(Icons.Default.Check, null, tint = IOSYellow, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            onSetSortOrder(NotesSortOrder.DATE_CREATED)
                                            showMoreMenu = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Title",
                                                fontWeight = if (uiState.sortOrder == NotesSortOrder.TITLE) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.sortOrder == NotesSortOrder.TITLE) {
                                                Icon(Icons.Default.Check, null, tint = IOSYellow, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        onClick = {
                                            onSetSortOrder(NotesSortOrder.TITLE)
                                            showMoreMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (MaterialTheme.colorScheme.background == IOSDarkGroupedBg) IOSDarkToolbarBg else IOSLightToolbarBg,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    if (uiState.isSelectionMode) {
                        // Selection Mode Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDeleteSelectedNotes,
                                enabled = uiState.selectedNoteIds.isNotEmpty()
                            ) {
                                Text(
                                    text = if (uiState.selectedNoteIds.isEmpty()) "Delete" else "Delete (${uiState.selectedNoteIds.size})",
                                    color = if (uiState.selectedNoteIds.isNotEmpty()) IOSRed else IOSLightTextSecondary
                                )
                            }

                            TextButton(
                                onClick = { showMoveDialog = true },
                                enabled = uiState.selectedNoteIds.isNotEmpty()
                            ) {
                                Text(
                                    text = if (uiState.selectedNoteIds.isEmpty()) "Move All" else "Move (${uiState.selectedNoteIds.size})",
                                    color = if (uiState.selectedNoteIds.isNotEmpty()) IOSYellow else IOSLightTextSecondary
                                )
                            }
                        }
                    } else {
                        // Standard iOS Bottom Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Checklist Note Button
                            IconButton(
                                onClick = { onNewNote(true) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quick_checklist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = "New Checklist Note",
                                    tint = IOSYellow,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Dynamic Notes Count (SF Pro footnote uppercase tracking)
                            Text(
                                text = "${uiState.notes.size} Notes".uppercase(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = IOSLightTextSecondary,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            // New Note Compose Button
                            IconButton(
                                onClick = { onNewNote(false) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("compose_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Compose Note",
                                    tint = IOSYellow,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Large SF Pro Title Header
            Text(
                text = folderTitle,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-0.8).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
            )

            // iOS Search Bar
            IOSSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchChange,
                selectedFilter = uiState.searchFilter,
                onFilterSelect = onFilterSelect,
                placeholder = "Search in $folderTitle"
            )

            if (uiState.notes.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = IOSLightTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No Results Found" else "No Notes",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IOSLightTextSecondary
                            )
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotBlank())
                                "Try searching for a different keyword"
                            else
                                "Tap the compose button below to create your first note.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = IOSLightTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else if (uiState.viewMode == NotesViewMode.GRID) {
                // iOS Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        Box {
                            IOSNoteGridCard(
                                note = note,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleNoteSelected(note.id)
                                    } else {
                                        onNoteClick(note)
                                    }
                                }
                            )

                            if (uiState.isSelectionMode) {
                                val isSelected = uiState.selectedNoteIds.contains(note.id)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IOSYellow else Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // iOS Grouped List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Pinned Notes Group
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = IOSLightTextSecondary,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp)
                            )
                        }

                        item {
                            IOSGroupedCard {
                                pinnedNotes.forEachIndexed { index, note ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.isSelectionMode) {
                                            val isSelected = uiState.selectedNoteIds.contains(note.id)
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleNoteSelected(note.id) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = IOSYellow,
                                                    uncheckedColor = IOSLightTextSecondary
                                                ),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            val folderName = if (uiState.currentFolder == null) {
                                                uiState.folders.firstOrNull { it.id == note.folderId }?.name
                                            } else null

                                            IOSNoteRow(
                                                note = note,
                                                folderName = folderName,
                                                onClick = {
                                                    if (uiState.isSelectionMode) {
                                                        onToggleNoteSelected(note.id)
                                                    } else {
                                                        onNoteClick(note)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (index < pinnedNotes.size - 1) {
                                        IOSDivider()
                                    }
                                }
                            }
                        }
                    }

                    // Grouped Unpinned Notes by Timeframe
                    groupedUnpinnedNotes.forEach { (timeframe, notesInGroup) ->
                        item {
                            Text(
                                text = timeframe.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = IOSLightTextSecondary,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                            )
                        }

                        item {
                            IOSGroupedCard {
                                notesInGroup.forEachIndexed { index, note ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.isSelectionMode) {
                                            val isSelected = uiState.selectedNoteIds.contains(note.id)
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleNoteSelected(note.id) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = IOSYellow,
                                                    uncheckedColor = IOSLightTextSecondary
                                                ),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            val folderName = if (uiState.currentFolder == null) {
                                                uiState.folders.firstOrNull { it.id == note.folderId }?.name
                                            } else null

                                            IOSNoteRow(
                                                note = note,
                                                folderName = folderName,
                                                onClick = {
                                                    if (uiState.isSelectionMode) {
                                                        onToggleNoteSelected(note.id)
                                                    } else {
                                                        onNoteClick(note)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (index < notesInGroup.size - 1) {
                                        IOSDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Move to Folder Dialog
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Move to Folder",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(uiState.folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onMoveSelectedNotes(folder.id)
                                    showMoveDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = getFolderIcon(folder.iconName),
                                contentDescription = null,
                                tint = IOSYellow
                            )
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel", color = IOSLightTextSecondary)
                }
            }
        )
    }
}

fun groupNotesByDate(notes: List<NoteEntity>): Map<String, List<NoteEntity>> {
    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val sevenDaysAgo = todayStart - 6 * 86400 * 1000L
    val thirtyDaysAgo = todayStart - 29 * 86400 * 1000L

    return notes.groupBy { note ->
        when {
            note.updatedAt >= todayStart -> "Today"
            note.updatedAt >= sevenDaysAgo -> "Previous 7 Days"
            note.updatedAt >= thirtyDaysAgo -> "Previous 30 Days"
            else -> "Earlier"
        }
    }
}
