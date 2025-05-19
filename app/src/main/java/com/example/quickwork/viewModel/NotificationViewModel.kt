package com.example.quickwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Notification
import com.example.quickwork.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val unreadCount: Int = 0
)

class NotificationViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        initialize()
    }

    private fun initialize() {
        if (userId != null) {
            listenToNotifications()
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun listenToNotifications() {
        if (userId != null) {
            listenerRegistration = notificationRepository.listenToNotifications(
                userId = userId,
                onUpdate = { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isLoading = false,
                        unreadCount = notifications.count { !it.isReaded }
                    )
                },
                onError = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        if (userId != null) {
            viewModelScope.launch {
                val success = notificationRepository.markNotificationAsRead(userId, notificationId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications.map {
                            if (it.id == notificationId) it.copy(isReaded = true)
                            else it
                        },
                        unreadCount = _uiState.value.notifications.count { !it.isReaded && it.id != notificationId }
                    )
                }
            }
        }
    }

    fun markAllNotificationsAsRead() {
        if (userId != null) {
            viewModelScope.launch {
                val success = notificationRepository.markAllNotificationsAsRead(userId, _uiState.value.notifications)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications.map { it.copy(isReaded = true) },
                        unreadCount = 0
                    )
                }
            }
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}