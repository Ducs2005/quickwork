package com.example.quickwork.data.models

//data class MessageRoom(
//    val receiverId: String,
//    val receiverName: String,
//    val receiverAvatarUrl: String?, // Added to store avatar URL
//    val latestMessage: Message?
//)
//
//data class Message (
//    val id: String = "",
//    val content: String = "",
//    val date: String = "",
//    val isReaded: Boolean = false
//)
//data class ChatUiState(
//    val receiverName: String = "",
//    val messages: List<Pair<Message, Boolean>> = emptyList(),
//    val isLoading: Boolean = true,
//    val senderAvatarUrl: String? = null, // Added
//    val receiverAvatarUrl: String? = null // Added
//)


// Represents a message room in the chat list, containing receiver details and the latest message
data class MessageRoom(
    val receiverId: String, // ID of the receiver user
    val receiverName: String, // Name of the receiver user
    val receiverAvatarUrl: String?, // URL to the receiver's avatar image, nullable for fallback to default icon
    val latestMessage: Message? // Latest message in the conversation, nullable if no messages exist
)

// Represents a single chat message
data class Message(
    val id: String = "", // Unique ID of the message
    val content: String = "", // Text content of the message
    val date: String = "", // Date and time of the message (e.g., "yyyy-MM-dd'T'HH:mm:ss")
    val isReaded: Boolean = false // Whether the message has been read by the receiver
)

// Represents the UI state for the ChatScreen
data class ChatUiState(
    val receiverName: String = "", // Name of the receiver user
    val messages: List<Pair<Message, Boolean>> = emptyList(), // List of messages with a flag indicating if sent by the current user
    val isLoading: Boolean = true, // Whether the chat data is still loading
    val senderAvatarUrl: String? = null, // URL to the current user's avatar image, nullable for fallback
    val receiverAvatarUrl: String? = null // URL to the receiver's avatar image, nullable for fallback
)