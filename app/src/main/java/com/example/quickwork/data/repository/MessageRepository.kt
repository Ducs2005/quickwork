package com.example.quickwork.data.repository

import android.util.Log
import com.example.quickwork.data.models.Message
import com.example.quickwork.data.models.MessageRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MessageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserRole(userId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("userType") ?: "employee"
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to fetch user role", e)
            "employee"
        }
    }

    fun listenToMessageRooms(
        userId: String,
        onUpdate: (List<MessageRoom>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        if (userId.isEmpty()) {
            onError(Exception("No user logged in"))
            return null
        }

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("messageRooms")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageRepository", "Error listening to message rooms", error)
                        onError(error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.let { docs ->
                        MainScope().launch {
                            val rooms = mutableListOf<MessageRoom>()
                            for (doc in docs) {
                                val receiverId = doc.id
                                try {
                                    val userDoc = firestore.collection("users")
                                        .document(receiverId)
                                        .get()
                                        .await()
                                    val receiverName = userDoc.getString("name") ?: "Unknown"
                                    val receiverAvatarUrl = userDoc.getString("avatarUrl")

                                    val messages = firestore.collection("users")
                                        .document(userId)
                                        .collection("messageRooms")
                                        .document(receiverId)
                                        .collection("messages")
                                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get()
                                        .await()
                                    val latestMessage = messages.documents.firstOrNull()?.let { msgDoc ->
                                        Message(
                                            id = msgDoc.id,
                                            content = msgDoc.getString("content") ?: "",
                                            date = msgDoc.getString("date") ?: "",
                                            isReaded = msgDoc.getBoolean("isReaded") ?: false
                                        )
                                    }
                                    rooms.add(MessageRoom(receiverId, receiverName, receiverAvatarUrl, latestMessage))
                                } catch (e: Exception) {
                                    Log.w("MessageRepository", "Error processing room $receiverId", e)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onUpdate(rooms.sortedByDescending { it.latestMessage?.date })
                            }
                        }
                    } ?: run {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to set up listener", e)
            onError(e)
            null
        }
    }

    suspend fun getReceiverInfo(receiverId: String): Pair<String, String?> {
        return try {
            val userDoc = firestore.collection("users").document(receiverId).get().await()
            val name = userDoc.getString("name") ?: "Unknown"
            val avatarUrl = userDoc.getString("avatarUrl")
            name to avatarUrl
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to fetch receiver info for $receiverId", e)
            "Unknown" to null
        }
    }

    suspend fun getSenderAvatarUrl(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("avatarUrl")
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to fetch sender avatar for $userId", e)
            null
        }
    }

    fun listenToMessages(
        userId: String,
        receiverId: String,
        onUpdate: (List<Pair<Message, Boolean>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        if (userId.isEmpty() || receiverId.isEmpty()) {
            onError(Exception("Invalid user or receiver ID"))
            return null
        }

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("messageRooms")
                .document(receiverId)
                .collection("messages")
                .orderBy("date")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageRepository", "Error listening to messages", error)
                        onError(error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.let { docs ->
                        MainScope().launch {
                            val messages = docs.mapNotNull { doc ->
                                try {
                                    val message = Message(
                                        id = doc.id,
                                        content = doc.getString("content") ?: "",
                                        date = doc.getString("date") ?: "",
                                        isReaded = doc.getBoolean("isReaded") ?: false
                                    )
                                    val isSent = doc.getString("senderId") == userId
                                    message to isSent
                                } catch (e: Exception) {
                                    Log.w("MessageRepository", "Error parsing message ${doc.id}", e)
                                    null
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onUpdate(messages)
                            }
                        }
                    } ?: run {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to set up messages listener", e)
            onError(e)
            null
        }
    }

    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String,
        date: String
    ): Boolean {
        return try {
            val messageData = mapOf(
                "senderId" to senderId,
                "content" to content,
                "date" to date,
                "isReaded" to false
            )

            // Send message to sender's message room
            firestore.collection("users")
                .document(senderId)
                .collection("messageRooms")
                .document(receiverId)
                .collection("messages")
                .add(messageData)
                .await()

            // Send message to receiver's message room
            firestore.collection("users")
                .document(receiverId)
                .collection("messageRooms")
                .document(senderId)
                .collection("messages")
                .add(messageData)
                .await()

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to send message from $senderId to $receiverId", e)
            false
        }
    }
}