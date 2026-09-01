package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.model.ChecklistItem
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NotesViewMode {
    LIST, GRID
}

enum class NotesSortOrder {
    DATE_EDITED, DATE_CREATED, TITLE
}

data class NotesUiState(
    val folders: List<FolderEntity> = emptyList(),
    val currentFolder: FolderEntity? = null, // null = "All iCloud"
    val isRecentlyDeletedView: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val deletedNotes: List<NoteEntity> = emptyList(),
    val totalActiveNotesCount: Int = 0,
    val deletedNotesCount: Int = 0,
    val searchQuery: String = "",
    val searchFilter: String = "All",
    val viewMode: NotesViewMode = NotesViewMode.LIST,
    val sortOrder: NotesSortOrder = NotesSortOrder.DATE_EDITED,
    val isUnlockedSession: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        // Observe folders
        viewModelScope.launch {
            repository.allFolders.collect { foldersList ->
                _uiState.update { it.copy(folders = foldersList) }
            }
        }

        // Observe total active notes count
        viewModelScope.launch {
            repository.totalActiveNotesCount.collect { count ->
                _uiState.update { it.copy(totalActiveNotesCount = count) }
            }
        }

        // Observe deleted notes count
        viewModelScope.launch {
            repository.deletedNotesCount.collect { count ->
                _uiState.update { it.copy(deletedNotesCount = count) }
            }
        }

        // Observe deleted notes
        viewModelScope.launch {
            repository.deletedNotes.collect { list ->
                _uiState.update { it.copy(deletedNotes = list) }
            }
        }

        // Reactively load notes based on current folder and search
        viewModelScope.launch {
            combine(
                _uiState.map { it.currentFolder }.distinctUntilChanged(),
                _uiState.map { it.isRecentlyDeletedView }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged(),
                _uiState.map { it.searchFilter }.distinctUntilChanged(),
                _uiState.map { it.sortOrder }.distinctUntilChanged()
            ) { folder, isDeleted, query, filter, sort ->
                when {
                    isDeleted -> repository.deletedNotes
                    query.isNotBlank() -> repository.searchNotes(query)
                    folder == null -> repository.allActiveNotes
                    else -> repository.getNotesByFolder(folder.id)
                }
            }.flatMapLatest { flow -> flow }
            .collect { rawNotes ->
                val query = _uiState.value.searchQuery
                val filter = _uiState.value.searchFilter
                val sort = _uiState.value.sortOrder

                var filtered = rawNotes
                if (filter != "All") {
                    filtered = when (filter) {
                        "Pinned" -> filtered.filter { it.isPinned }
                        "Checklists" -> filtered.filter { it.checklistsJson.isNotBlank() && it.checklistsJson != "[]" }
                        "Locked" -> filtered.filter { it.isLocked }
                        else -> filtered
                    }
                }

                val sorted = when (sort) {
                    NotesSortOrder.DATE_EDITED -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt }
                    )
                    NotesSortOrder.DATE_CREATED -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAt }
                    )
                    NotesSortOrder.TITLE -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase() }
                    )
                }

                _uiState.update { it.copy(notes = sorted) }
            }
        }
    }

    fun selectFolder(folder: FolderEntity?) {
        _uiState.update {
            it.copy(
                currentFolder = folder,
                isRecentlyDeletedView = false,
                searchQuery = "",
                isSelectionMode = false,
                selectedNoteIds = emptySet()
            )
        }
    }

    fun openRecentlyDeleted() {
        _uiState.update {
            it.copy(
                isRecentlyDeletedView = true,
                currentFolder = null,
                searchQuery = "",
                isSelectionMode = false,
                selectedNoteIds = emptySet()
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchFilterChange(filter: String) {
        _uiState.update { it.copy(searchFilter = filter) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == NotesViewMode.LIST) NotesViewMode.GRID else NotesViewMode.LIST)
        }
    }

    fun setSortOrder(sortOrder: NotesSortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun toggleSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedNoteIds = emptySet()
            )
        }
    }

    fun toggleNoteSelected(noteId: Long) {
        _uiState.update {
            val current = it.selectedNoteIds
            val updated = if (current.contains(noteId)) current - noteId else current + noteId
            it.copy(selectedNoteIds = updated)
        }
    }

    fun deleteSelectedNotes() {
        val ids = _uiState.value.selectedNoteIds
        viewModelScope.launch {
            ids.forEach { id ->
                if (_uiState.value.isRecentlyDeletedView) {
                    repository.permanentlyDelete(id)
                } else {
                    repository.moveToTrash(id)
                }
            }
            _uiState.update { it.copy(selectedNoteIds = emptySet(), isSelectionMode = false) }
        }
    }

    fun moveSelectedNotesToFolder(folderId: Long) {
        val ids = _uiState.value.selectedNoteIds
        viewModelScope.launch {
            ids.forEach { id ->
                val note = repository.getNote(id)
                if (note != null) {
                    repository.updateNote(note.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
                }
            }
            _uiState.update { it.copy(selectedNoteIds = emptySet(), isSelectionMode = false) }
        }
    }

    suspend fun createNewNote(
        folderId: Long? = null,
        initialTitle: String = "",
        initialContent: String = "",
        withChecklist: Boolean = false
    ): Long {
        val targetFolderId = folderId ?: _uiState.value.currentFolder?.id ?: 1L
        val initialChecklistJson = if (withChecklist) {
            """[{"id":"${UUID.randomUUID()}","text":"","isChecked":false}]"""
        } else ""

        val newNote = NoteEntity(
            title = initialTitle,
            content = initialContent,
            folderId = targetFolderId,
            checklistsJson = initialChecklistJson,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertNote(newNote)
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun toggleLock(noteId: Long) {
        viewModelScope.launch {
            repository.toggleLock(noteId)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
        }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.permanentlyDelete(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun createFolder(name: String, iconName: String = "folder", colorHex: String = "#E3A108") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = _uiState.value.folders.size
            repository.insertFolder(
                FolderEntity(
                    name = name.trim(),
                    iconName = iconName,
                    colorHex = colorHex,
                    orderIndex = count,
                    isSystem = false
                )
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_uiState.value.currentFolder?.id == folderId) {
                _uiState.update { it.copy(currentFolder = null) }
            }
        }
    }

    fun unlockSession() {
        _uiState.update { it.copy(isUnlockedSession = true) }
    }

    suspend fun getNoteById(id: Long): NoteEntity? = repository.getNote(id)

    fun observeNoteById(id: Long): Flow<NoteEntity?> = repository.observeNote(id)

    // Checklists serialization helpers
    fun parseChecklistJson(json: String): List<ChecklistItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<ChecklistItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeChecklistJson(items: List<ChecklistItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("isChecked", item.isChecked)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}

class NotesViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
