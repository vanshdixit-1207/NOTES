package com.example.data.model

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false
)
