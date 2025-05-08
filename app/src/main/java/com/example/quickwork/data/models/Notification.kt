package com.example.quickwork.data.models

// Data class for notifications
data class Notification(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val from: String = "",
    val isReaded: Boolean = false,
    val timestamp: Long = 0L
)