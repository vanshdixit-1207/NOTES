package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.model.ChecklistItem
import com.example.ui.components.IOSBackButton
import com.example.ui.components.formatIOSEditorDate
import com.example.ui.components.getFolderIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.NotesViewModel
import java.util.UUID

enum class TextStyleFormat {
    TITLE, HEADING, SUBHEADING, BODY, MONOSPACED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    folders: List<FolderEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var folderId by remember { mutableStateOf(1L) }
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }

    var showFormattingBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showNoteInfoDialog by remember { mutableStateOf(false) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var currentTextFormat by remember { mutableStateOf(TextStyleFormat.BODY) }

    val bodyFocusRequester = remember { FocusRequester() }

    // Load initial note
    LaunchedEffect(noteId) {
        val loadedNote = viewModel.getNoteById(noteId)
        if (loadedNote != null) {
            note = loadedNote
            title = loadedNote.title
            content = loadedNote.content
            isPinned = loadedNote.isPinned
            isLocked = loadedNote.isLocked
            folderId = loadedNote.folderId
            checklistItems = viewModel.parseChecklistJson(loadedNote.checklistsJson)
            isLoaded = true
        }
    }

    // Auto-save debounced helper
    fun saveNoteChanges() {
        if (!isLoaded || note == null) return
        val updatedChecklistsJson = viewModel.serializeChecklistJson(checklistItems)
        val updatedNote = note!!.copy(
            title = title,
            content = content,
            isPinned = isPinned,
            isLocked = isLocked,
            folderId = folderId,
            checklistsJson = updatedChecklistsJson,
            updatedAt = System.currentTimeMillis()
        )
        note = updatedNote
        viewModel.updateNote(updatedNote)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    val folderName = folders.firstOrNull { it.id == folderId }?.name ?: "Notes"
                    IOSBackButton(
                        label = folderName,
                        onClick = {
                            saveNoteChanges()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Lock toggle
                        IconButton(
                            onClick = {
                                isLocked = !isLocked
                                saveNoteChanges()
                            },
                            modifier = Modifier.testTag("editor_lock_toggle")
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                                contentDescription = "Lock Note",
                                tint = if (isLocked) IOSYellow else IOSLightTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Pin toggle
                        IconButton(
                            onClick = {
                                isPinned = !isPinned
                                saveNoteChanges()
                            },
                            modifier = Modifier.testTag("editor_pin_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (isPinned) IOSYellow else IOSLightTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    if (title.isNotBlank()) appendLine(title)
                                    if (content.isNotBlank()) appendLine(content)
                                    checklistItems.forEach { item ->
                                        appendLine(if (item.isChecked) "[✓] ${item.text}" else "[ ] ${item.text}")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    putExtra(Intent.EXTRA_TITLE, title.ifBlank { "Note" })
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.testTag("editor_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = IOSYellow,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // More menu
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("editor_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More",
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
                                    text = { Text("Move to Folder") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Folder, null, tint = IOSYellow)
                                    },
                                    onClick = {
                                        showMoveFolderDialog = true
                                        showMoreMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Note Info") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Info, null, tint = IOSYellow)
                                    },
                                    onClick = {
                                        showNoteInfoDialog = true
                                        showMoreMenu = false
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Delete Note", color = IOSRed) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, null, tint = IOSRed)
                                    },
                                    onClick = {
                                        viewModel.deleteNote(noteId)
                                        showMoreMenu = false
                                        onBack()
                                    },
                                    modifier = Modifier.testTag("editor_delete_note_button")
                                )
                            }
                        }

                        // "Done" button to clear focus
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                saveNoteChanges()
                            }
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = IOSYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // iOS Notes Formatting & Action Bar
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

                    // Secondary formatting drawer
                    AnimatedVisibility(
                        visible = showFormattingBar,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Style selector pills (Title, Heading, Subheading, Body, Monospaced)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextStyleFormat.values().forEach { format ->
                                    val isSelected = currentTextFormat == format
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) IOSYellow else MaterialTheme.colorScheme.surface)
                                            .clickable { currentTextFormat = format }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = format.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }

                            // Style toggles: Bold, Italic, Underline, Strikethrough
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                FilterChip(
                                    selected = isBold,
                                    onClick = { isBold = !isBold },
                                    label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IOSYellow,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = isItalic,
                                    onClick = { isItalic = !isItalic },
                                    label = { Text("I", fontStyle = FontStyle.Italic, fontSize = 16.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IOSYellow,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = isUnderline,
                                    onClick = { isUnderline = !isUnderline },
                                    label = { Text("U", textDecoration = TextDecoration.Underline, fontSize = 16.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IOSYellow,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = isStrikethrough,
                                    onClick = { isStrikethrough = !isStrikethrough },
                                    label = { Text("S", textDecoration = TextDecoration.LineThrough, fontSize = 16.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IOSYellow,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Main Bottom Bar Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checklist Toggle
                        IconButton(
                            onClick = {
                                val newItem = ChecklistItem(
                                    id = UUID.randomUUID().toString(),
                                    text = "",
                                    isChecked = false
                                )
                                checklistItems = checklistItems + newItem
                                saveNoteChanges()
                            },
                            modifier = Modifier.testTag("add_checklist_item_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = "Checklist",
                                tint = IOSYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Bullet list button
                        IconButton(
                            onClick = {
                                content = if (content.endsWith("\n") || content.isEmpty()) {
                                    "$content• "
                                } else {
                                    "$content\n• "
                                }
                                saveNoteChanges()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "Bullet List",
                                tint = IOSYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Numbered list button
                        IconButton(
                            onClick = {
                                val lineCount = content.lines().size
                                content = if (content.endsWith("\n") || content.isEmpty()) {
                                    "$content$lineCount. "
                                } else {
                                    "$content\n$lineCount. "
                                }
                                saveNoteChanges()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = "Numbered List",
                                tint = IOSYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Formatting 'Aa' Button
                        IconButton(
                            onClick = { showFormattingBar = !showFormattingBar },
                            modifier = Modifier.testTag("formatting_aa_button")
                        ) {
                            Text(
                                text = "Aa",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (showFormattingBar) IOSYellow else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                            )
                        }

                        // Lock / Privacy icon
                        IconButton(
                            onClick = {
                                isLocked = !isLocked
                                saveNoteChanges()
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                                contentDescription = "Privacy Lock",
                                tint = if (isLocked) IOSYellow else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
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
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Note Timestamp Header (Apple Notes Centered Footnote)
            item {
                Text(
                    text = formatIOSEditorDate(note?.updatedAt ?: System.currentTimeMillis()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IOSLightTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // Note Title Field (Large Bold SF Pro)
            item {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        saveNoteChanges()
                    },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(IOSYellow),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        color = IOSLightTextSecondary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                        .testTag("note_title_input")
                )
            }

            // Interactive Checklists Section
            if (checklistItems.isNotEmpty()) {
                itemsIndexed(checklistItems, key = { _, item -> item.id }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Circular iOS style checkbox
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (item.isChecked) IOSYellow else Color.Transparent)
                                .clickable {
                                    val updated = checklistItems.toMutableList()
                                    updated[index] = item.copy(isChecked = !item.isChecked)
                                    checklistItems = updated
                                    saveNoteChanges()
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .padding(1.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Circle,
                                        contentDescription = "Not completed",
                                        tint = IOSLightTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Checklist text input
                        BasicTextField(
                            value = item.text,
                            onValueChange = { newText ->
                                val updated = checklistItems.toMutableList()
                                updated[index] = item.copy(text = newText)
                                checklistItems = updated
                                saveNoteChanges()
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = if (item.isChecked) IOSLightTextSecondary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = 17.sp
                            ),
                            cursorBrush = SolidColor(IOSYellow),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    val newItem = ChecklistItem(id = UUID.randomUUID().toString(), text = "", isChecked = false)
                                    val updated = checklistItems.toMutableList()
                                    updated.add(index + 1, newItem)
                                    checklistItems = updated
                                    saveNoteChanges()
                                }
                            ),
                            decorationBox = { inner ->
                                Box(modifier = Modifier.weight(1f)) {
                                    if (item.text.isEmpty()) {
                                        Text("List Item", color = IOSLightTextSecondary.copy(alpha = 0.5f))
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("checklist_input_$index")
                        )

                        // Remove item button
                        IconButton(
                            onClick = {
                                val updated = checklistItems.toMutableList()
                                updated.removeAt(index)
                                checklistItems = updated
                                saveNoteChanges()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove item",
                                tint = IOSLightTextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Add checklist item button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newItem = ChecklistItem(id = UUID.randomUUID().toString(), text = "", isChecked = false)
                                checklistItems = checklistItems + newItem
                                saveNoteChanges()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IOSYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add checklist item",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = IOSYellow,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Note Body Content TextField (Rich Text styling)
            item {
                val computedStyle = when (currentTextFormat) {
                    TextStyleFormat.TITLE -> MaterialTheme.typography.displayMedium.copy(
                        fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 30.sp
                    )
                    TextStyleFormat.HEADING -> MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )
                    TextStyleFormat.SUBHEADING -> MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    )
                    TextStyleFormat.MONOSPACED -> MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    TextStyleFormat.BODY -> MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        textDecoration = when {
                            isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                            isUnderline -> TextDecoration.Underline
                            isStrikethrough -> TextDecoration.LineThrough
                            else -> TextDecoration.None
                        }
                    )
                }.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )

                BasicTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        saveNoteChanges()
                    },
                    textStyle = computedStyle,
                    cursorBrush = SolidColor(IOSYellow),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 280.dp)
                        ) {
                            if (content.isEmpty() && checklistItems.isEmpty()) {
                                Text(
                                    text = "Start typing here...",
                                    style = computedStyle.copy(
                                        color = IOSLightTextSecondary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(bodyFocusRequester)
                        .testTag("note_content_input")
                )
            }
        }
    }

    // Move Note to Folder Dialog
    if (showMoveFolderDialog) {
        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Move to Folder",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    folderId = folder.id
                                    saveNoteChanges()
                                    showMoveFolderDialog = false
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (folderId == folder.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Current folder",
                                    tint = IOSYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancel", color = IOSLightTextSecondary)
                }
            }
        )
    }

    // Note Info Dialog
    if (showNoteInfoDialog) {
        val wordCount = remember(content, title) {
            val text = "$title $content"
            if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        }
        val charCount = remember(content, title) {
            "$title$content".length
        }
        val currentFolder = folders.firstOrNull { it.id == folderId }?.name ?: "Notes"

        AlertDialog(
            onDismissRequest = { showNoteInfoDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Note Information",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Folder", color = IOSLightTextSecondary)
                        Text(currentFolder, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Words", color = IOSLightTextSecondary)
                        Text("$wordCount", fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Characters", color = IOSLightTextSecondary)
                        Text("$charCount", fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Checklist items", color = IOSLightTextSecondary)
                        Text("${checklistItems.size}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Modified", color = IOSLightTextSecondary)
                        Text(
                            formatIOSEditorDate(note?.updatedAt ?: System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNoteInfoDialog = false }) {
                    Text("OK", color = IOSYellow, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
