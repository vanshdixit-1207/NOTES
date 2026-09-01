package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NotesDatabase
import com.example.data.repository.NotesRepository
import com.example.ui.components.IOSPasscodeDialog
import com.example.ui.screens.*
import com.example.ui.theme.IOSNotesTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.NotesViewModelFactory
import kotlinx.coroutines.launch

sealed class Screen {
    object Folders : Screen()
    object NotesList : Screen()
    data class NoteEditor(val noteId: Long) : Screen()
    object RecentlyDeleted : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: NotesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = NotesDatabase.getDatabase(applicationContext, lifecycleScope)
        repository = NotesRepository(database.noteDao(), database.folderDao())

        setContent {
            IOSNotesTheme {
                val viewModel: NotesViewModel = viewModel(
                    factory = NotesViewModelFactory(repository)
                )

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.NotesList) }
                var lockedNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }
                val scope = rememberCoroutineScope()

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { target ->
                        when (target) {
                            is Screen.Folders -> {
                                FoldersScreen(
                                    uiState = uiState,
                                    onSelectFolder = { folder ->
                                        viewModel.selectFolder(folder)
                                        currentScreen = Screen.NotesList
                                    },
                                    onOpenRecentlyDeleted = {
                                        viewModel.openRecentlyDeleted()
                                        currentScreen = Screen.RecentlyDeleted
                                    },
                                    onNewFolder = { name, icon, color ->
                                        viewModel.createFolder(name, icon, color)
                                    },
                                    onDeleteFolder = { folderId ->
                                        viewModel.deleteFolder(folderId)
                                    },
                                    onNewNote = {
                                        scope.launch {
                                            val newId = viewModel.createNewNote()
                                            currentScreen = Screen.NoteEditor(newId)
                                        }
                                    },
                                    onSearchChange = { query ->
                                        viewModel.onSearchQueryChange(query)
                                        if (query.isNotEmpty()) {
                                            currentScreen = Screen.NotesList
                                        }
                                    },
                                    onNoteClick = { noteId ->
                                        currentScreen = Screen.NoteEditor(noteId)
                                    }
                                )
                            }

                            is Screen.NotesList -> {
                                NotesListScreen(
                                    uiState = uiState,
                                    onBackToFolders = {
                                        currentScreen = Screen.Folders
                                    },
                                    onNoteClick = { note ->
                                        if (note.isLocked && !uiState.isUnlockedSession) {
                                            lockedNoteTarget = note
                                        } else {
                                            currentScreen = Screen.NoteEditor(note.id)
                                        }
                                    },
                                    onNewNote = { withChecklist ->
                                        scope.launch {
                                            val newId = viewModel.createNewNote(withChecklist = withChecklist)
                                            currentScreen = Screen.NoteEditor(newId)
                                        }
                                    },
                                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                                    onFilterSelect = { viewModel.onSearchFilterChange(it) },
                                    onTogglePin = { viewModel.togglePin(it) },
                                    onToggleLock = { viewModel.toggleLock(it) },
                                    onDeleteNote = { viewModel.deleteNote(it) },
                                    onToggleViewMode = { viewModel.toggleViewMode() },
                                    onSetSortOrder = { viewModel.setSortOrder(it) },
                                    onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                                    onToggleNoteSelected = { viewModel.toggleNoteSelected(it) },
                                    onDeleteSelectedNotes = { viewModel.deleteSelectedNotes() },
                                    onMoveSelectedNotes = { folderId ->
                                        viewModel.moveSelectedNotesToFolder(folderId)
                                    }
                                )
                            }

                            is Screen.NoteEditor -> {
                                NoteEditorScreen(
                                    noteId = target.noteId,
                                    viewModel = viewModel,
                                    folders = uiState.folders,
                                    onBack = {
                                        currentScreen = Screen.NotesList
                                    }
                                )
                            }

                            is Screen.RecentlyDeleted -> {
                                RecentlyDeletedScreen(
                                    uiState = uiState,
                                    onBack = {
                                        currentScreen = Screen.Folders
                                    },
                                    onRestoreNote = { viewModel.restoreNote(it) },
                                    onPermanentlyDelete = { viewModel.permanentlyDeleteNote(it) },
                                    onEmptyTrash = { viewModel.emptyTrash() }
                                )
                            }
                        }
                    }

                    // Passcode dialog for locked notes
                    if (lockedNoteTarget != null) {
                        IOSPasscodeDialog(
                            onDismiss = { lockedNoteTarget = null },
                            onSuccess = {
                                viewModel.unlockSession()
                                val targetNoteId = lockedNoteTarget!!.id
                                lockedNoteTarget = null
                                currentScreen = Screen.NoteEditor(targetNoteId)
                            }
                        )
                    }
                }
            }
        }
    }
}
