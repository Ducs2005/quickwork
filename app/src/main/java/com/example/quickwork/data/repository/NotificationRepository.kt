package com.example.quickwork.data.repository

import android.util.Log
import com.example.quickwork.data.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    suspend fun addNotification(userId: String, notification: Notification): Boolean {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notification.id)
                .set(notification)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to add notification for user $userId", e)
            false
        }
    }
    fun listenToNotifications(
        userId: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        if (userId.isEmpty()) {
            onError(Exception("No user logged in"))
            return null
        }

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("NotificationRepository", "Error listening to notifications", error)
                        onError(error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.let { docs ->
                        MainScope().launch {
                            val notifications = docs.mapNotNull { doc ->
                                try {
                                    Notification(
                                        id = doc.id,
                                        title = doc.getString("title") ?: "",
                                        content = doc.getString("content") ?: "",
                                        from = doc.getString("from") ?: "",
                                        isReaded = doc.getBoolean("isReaded") ?: false,
                                        timestamp = doc.getLong("timestamp") ?: 0L
                                    )
                                } catch (e: Exception) {
                                    Log.w("NotificationRepository", "Error parsing notification ${doc.id}", e)
                                    null
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onUpdate(notifications)
                            }
                        }
                    } ?: run {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to set up notifications listener", e)
            onError(e)
            null
        }
    }

    suspend fun markNotificationAsRead(userId: String, notificationId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("isReaded", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to mark notification $notificationId as read", e)
            false
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String, notifications: List<Notification>): Boolean {
        return try {
            val batch = firestore.batch()
            notifications.filter { !it.isReaded }.forEach { notification ->
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(notification.id)
                batch.update(docRef, "isReaded", true)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to mark all notifications as read", e)
            false
        }
    }

    suspend fun createNotification(
        employeeId: String,
        title: String,
        content: String,
        from: String
    ): Boolean {
        return try {
            val notification = hashMapOf(
                "title" to title,
                "content" to content,
                "from" to from,
                "isReaded" to false,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(employeeId)
                .collection("notifications")
                .add(notification)
                .await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to create notification for employee $employeeId", e)
            false
        }
    }
}