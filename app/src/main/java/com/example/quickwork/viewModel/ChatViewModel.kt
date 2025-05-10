package com.example.quickwork.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<Pair<Message, Boolean>> = emptyList(), // Pair of Message and isSent
    val receiverName: String = "Unknown",
    val error: String? = null,
    val senderAvatarUrl: String? = null, // URL to the current user's avatar image
    val receiverAvatarUrl: String? = null // URL to the receiver's avatar image
)

class ChatViewModel(
    private val userId: String,
    private val receiverId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sentListener: ListenerRegistration? = null
    private var receivedListener: ListenerRegistration? = null

    init {
        if (userId.isEmpty() || receiverId.isEmpty()) {
            _uiState.value = ChatUiState(
                isLoading = false,
                error = "Invalid user or receiver ID"
            )
        } else {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Fetch sender's avatar
                val senderDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                val senderAvatarUrl = senderDoc.getString("avatarUrl")

                // Fetch receiver's name and avatar
                val receiverDoc = firestore.collection("users")
                    .document(receiverId)
                    .get()
                    .await()
                val receiverName = receiverDoc.getString("name") ?: "Unknown"
                val receiverAvatarUrl = receiverDoc.getString("avatarUrl")

                // Set up real-time listeners
                sentListener = firestore.collection("users")
                    .document(userId)
                    .collection("messageRooms")
                    .document(receiverId)
                    .collection("messages")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ChatViewModel", "Error listening to sent messages", error)
                            _uiState.value = _uiState.value.copy(error = "Failed to load sent messages")
                            return@addSnapshotListener
                        }

                        viewModelScope.launch {
                            updateMessages(receiverName, senderAvatarUrl, receiverAvatarUrl)
                        }
                    }

                receivedListener = firestore.collection("users")
                    .document(receiverId)
                    .collection("messageRooms")
                    .document(userId)
                    .collection("messages")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ChatViewModel", "Error listening to received messages", error)
                            _uiState.value = _uiState.value.copy(error = "Failed to load received messages")
                            return@addSnapshotListener
                        }

                        viewModelScope.launch {
                            updateMessages(receiverName, senderAvatarUrl, receiverAvatarUrl)
                        }
                    }

                // Initial load
                updateMessages(receiverName, senderAvatarUrl, receiverAvatarUrl)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load data", e)
                _uiState.value = ChatUiState(
                    isLoading = false,
                    error = "Failed to load chat data",
                    senderAvatarUrl = null,
                    receiverAvatarUrl = null
                )
            }
        }
    }

    private suspend fun updateMessages(
        receiverName: String,
        senderAvatarUrl: String?,
        receiverAvatarUrl: String?
    ) {
        try {
            val allMessages = mutableListOf<Pair<Message, Boolean>>()

            // Fetch sent messages (isSent = true)
            val sentMessages = firestore.collection("users")
                .document(userId)
                .collection("messageRooms")
                .document(receiverId)
                .collection("messages")
                .get()
                .await()
            sentMessages.documents.forEach { doc ->
                try {
                    allMessages.add(
                        Pair(
                            Message(
                                id = doc.id,
                                content = doc.getString("content") ?: "",
                                date = doc.getString("date") ?: "",
                                isReaded = doc.getBoolean("isReaded") ?: false
                            ),
                            true // isSent
                        )
                    )
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Error parsing sent message ${doc.id}", e)
                }
            }

            // Fetch received messages (isSent = false)
            val receivedMessages = firestore.collection("users")
                .document(receiverId)
                .collection("messageRooms")
                .document(userId)
                .collection("messages")
                .get()
                .await()
            receivedMessages.documents.forEach { doc ->
                try {
                    allMessages.add(
                        Pair(
                            Message(
                                id = doc.id,
                                content = doc.getString("content") ?: "",
                                date = doc.getString("date") ?: "",
                                isReaded = doc.getBoolean("isReaded") ?: false
                            ),
                            false // isSent
                        )
                    )
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Error parsing received message ${doc.id}", e)
                }
            }

            _uiState.value = ChatUiState(
                isLoading = false,
                messages = allMessages.sortedBy { it.first.date },
                receiverName = receiverName,
                senderAvatarUrl = senderAvatarUrl,
                receiverAvatarUrl = receiverAvatarUrl
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to update messages", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load messages"
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val date = LocalDateTime.now().format(formatter)
                val messageData = hashMapOf(
                    "content" to content,
                    "date" to date,
                    "isReaded" to false
                )

                // Add to sender's message room
                firestore.collection("users")
                    .document(userId)
                    .collection("messageRooms")
                    .document(receiverId)
                    .set(hashMapOf("receiverId" to receiverId))
                    .await()

                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("messageRooms")
                    .document(receiverId)
                    .collection("messages")
                    .add(messageData)
                    .await()

                // Update message with its ID
                firestore.collection("users")
                    .document(userId)
                    .collection("messageRooms")
                    .document(receiverId)
                    .collection("messages")
                    .document(docRef.id)
                    .update("id", docRef.id)
                    .await()

                // Create receiver's message room
                firestore.collection("users")
                    .document(receiverId)
                    .collection("messageRooms")
                    .document(userId)
                    .set(hashMapOf("receiverId" to userId))
                    .await()

                Log.d("ChatViewModel", "Message sent with ID ${docRef.id}")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to send message", e)
                _uiState.value = _uiState.value.copy(error = "Failed to send message")
            }
        }
    }

    override fun onCleared() {
        sentListener?.remove()
        receivedListener?.remove()
        super.onCleared()
    }
}