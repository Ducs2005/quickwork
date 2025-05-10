package com.example.quickwork.data.models


data class MessageRoom(
    val receiverId: String,
    val receiverName: String,
    val latestMessage: Message? // Latest sent message
)

data class Message (
    val id: String = "",
    val content: String = "",
    val date: String = "",
    val isReaded: Boolean = false
)