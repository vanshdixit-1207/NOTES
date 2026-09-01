package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val folderId: Long = 1, // Default "Notes" folder
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isDeleted: Boolean = false,
    val colorTag: String = "",
    val checklistsJson: String = "", // JSON list of ChecklistItem
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
