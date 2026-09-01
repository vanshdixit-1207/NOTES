package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotes()
    val totalActiveNotesCount: Flow<Int> = noteDao.getActiveNotesCount()
    val deletedNotesCount: Flow<Int> = noteDao.getDeletedNotesCount()

    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>> =
        noteDao.getNotesByFolder(folderId)

    fun getFolderNotesCount(folderId: Long): Flow<Int> =
        noteDao.getFolderNotesCount(folderId)

    fun searchNotes(query: String): Flow<List<NoteEntity>> =
        noteDao.searchNotes(query)

    fun observeNote(id: Long): Flow<NoteEntity?> =
        noteDao.observeNoteById(id)

    suspend fun getNote(id: Long): NoteEntity? =
        noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long =
        noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) =
        noteDao.updateNote(note)

    suspend fun moveToTrash(id: Long) =
        noteDao.moveToTrash(id)

    suspend fun restoreFromTrash(id: Long) =
        noteDao.restoreFromTrash(id)

    suspend fun permanentlyDelete(id: Long) =
        noteDao.permanentlyDelete(id)

    suspend fun emptyTrash() =
        noteDao.emptyTrash()

    suspend fun togglePin(id: Long) =
        noteDao.togglePin(id)

    suspend fun toggleLock(id: Long) =
        noteDao.toggleLock(id)

    suspend fun insertFolder(folder: FolderEntity): Long =
        folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) =
        folderDao.updateFolder(folder)

    suspend fun deleteFolder(id: Long) =
        folderDao.deleteFolder(id)

    suspend fun getFolder(id: Long): FolderEntity? =
        folderDao.getFolderById(id)
}
