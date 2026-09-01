package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.ui.components.IOSDivider
import com.example.ui.components.IOSFolderRow
import com.example.ui.components.IOSGroupedCard
import com.example.ui.components.IOSSearchBar
import com.example.ui.components.getFolderIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.NotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    uiState: NotesUiState,
    onSelectFolder: (FolderEntity?) -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onNewFolder: (String, String, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onNewNote: () -> Unit,
    onSearchChange: (String) -> Unit,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#E3A108") }
    var selectedIconName by remember { mutableStateOf("folder") }

    val folderColors = listOf(
        "#E3A108", // Yellow
        "#FF9500", // Orange
        "#FF2D55", // Pink
        "#AF52DE", // Purple
        "#007AFF", // Blue
        "#34C759", // Green
        "#5856D6"  // Indigo
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // New Folder Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showNewFolderDialog = true }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("new_folder_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = "New Folder",
                                tint = IOSYellow,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "New Folder",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = IOSYellow,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                )
                            )
                        }

                        // New Note Button
                        IconButton(
                            onClick = onNewNote,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("new_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "New Note",
                                tint = IOSYellow,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Top App Bar row with "Edit"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = IOSYellow,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isEditMode = !isEditMode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("edit_folders_toggle")
                    )
                }
            }

            // Large Collapsing SF Pro Title "Folders"
            item {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = (-0.8).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // iOS Search Bar
            item {
                IOSSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = "Search Notes & Folders"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Section: "ICLOUD"
            item {
                Text(
                    text = "ICLOUD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = IOSLightTextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.padding(start = 24.dp, bottom = 6.dp, top = 6.dp)
                )
            }

            // Grouped iCloud Folders Card
            item {
                IOSGroupedCard {
                    // "All iCloud" Folder
                    IOSFolderRow(
                        icon = Icons.Outlined.CloudQueue,
                        iconColor = IOSBlue,
                        title = "All iCloud",
                        count = uiState.totalActiveNotesCount,
                        onClick = { onSelectFolder(null) }
                    )

                    IOSDivider()

                    // User / System Folders
                    uiState.folders.forEachIndexed { index, folder ->
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(folder.colorHex))
                        } catch (e: Exception) {
                            IOSYellow
                        }

                        val folderIcon = getFolderIcon(folder.iconName)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode && !folder.isSystem) {
                                IconButton(
                                    onClick = { onDeleteFolder(folder.id) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircle,
                                        contentDescription = "Delete Folder",
                                        tint = IOSRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                val folderNoteCount = uiState.notes.count { it.folderId == folder.id && !it.isDeleted }
                                IOSFolderRow(
                                    icon = folderIcon,
                                    iconColor = folderColor,
                                    title = folder.name,
                                    count = folderNoteCount,
                                    onClick = { onSelectFolder(folder) }
                                )
                            }
                        }

                        if (index < uiState.folders.size - 1) {
                            IOSDivider(startIndent = if (isEditMode && !folder.isSystem) 56.dp else 52.dp)
                        }
                    }
                }
            }

            // Section: "MORE" / Recently Deleted
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clickable(onClick = onOpenRecentlyDeleted)
                        .testTag("folder_row_Recently Deleted"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IOSYellowBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = IOSYellow,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Recently Deleted",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.deletedNotesCount} items",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = IOSLightTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = IOSLightTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog (iOS Style)
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "New Folder",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Enter a name for this folder.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = IOSLightTextSecondary,
                            fontSize = 14.sp
                        )
                    )

                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Folder Name", color = IOSLightTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IOSYellow,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_name_input")
                    )

                    // Color picker dots
                    Text(
                        text = "Color Tag",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = IOSLightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        folderColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onNewFolder(newFolderName, selectedIconName, selectedColorHex)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_folder_button")
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = IOSYellow,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = IOSLightTextSecondary
                        )
                    )
                }
            }
        )
    }
}

