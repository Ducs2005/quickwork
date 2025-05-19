package com.example.quickwork.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Message
import com.example.quickwork.data.repository.MessageRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ChatUiState(
    val messages: List<Pair<Message, Boolean>> = emptyList(),
    val isLoading: Boolean = true,
    val receiverName: String = "Unknown",
    val senderAvatarUrl: String? = null,
    val receiverAvatarUrl: String? = null
)

class ChatViewModel(
    private val userId: String,
    private val receiverId: String,
    private val messageRepository: MessageRepository = MessageRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Fetch receiver info
            val (receiverName, receiverAvatarUrl) = messageRepository.getReceiverInfo(receiverId)
            // Fetch sender avatar
            val senderAvatarUrl = messageRepository.getSenderAvatarUrl(userId)
            _uiState.value = _uiState.value.copy(
                receiverName = receiverName,
                senderAvatarUrl = senderAvatarUrl,
                receiverAvatarUrl = receiverAvatarUrl
            )
            // Listen to messages
            listenToMessages()
        }
    }

    private fun listenToMessages() {
        listenerRegistration = messageRepository.listenToMessages(
            userId = userId,
            receiverId = receiverId,
            onUpdate = { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false
                )
            },
            onError = {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(content: String) {
        viewModelScope.launch {
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            messageRepository.sendMessage(
                senderId = userId,
                receiverId = receiverId,
                content = content,
                date = date
            )
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}

class ChatViewModelFactory(
    private val userId: String,
    private val receiverId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(userId, receiverId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}