package com.example.quickwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.MessageRoom
import com.example.quickwork.data.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessageRoomsViewModel(
    private val messageRepository: MessageRepository = MessageRepository()
) : ViewModel() {
    private val _messageRooms = MutableStateFlow<List<MessageRoom>>(emptyList())
    val messageRooms: StateFlow<List<MessageRoom>> = _messageRooms

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _userRole = MutableStateFlow("employee")
    val userRole: StateFlow<String> = _userRole

    private var listenerRegistration: ListenerRegistration? = null

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        initialize()
    }

    private fun initialize() {
        if (userId != null) {
            viewModelScope.launch {
                _userRole.value = messageRepository.getUserRole(userId)
                listenToMessageRooms()
            }
        } else {
            _isLoading.value = false
        }
    }

    private fun listenToMessageRooms() {
        if (userId != null) {
            listenerRegistration = messageRepository.listenToMessageRooms(
                userId = userId,
                onUpdate = { rooms ->
                    _messageRooms.value = rooms
                    _isLoading.value = false
                },
                onError = {
                    _isLoading.value = false
                }
            )
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}