package com.example.quickwork.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch notifications from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val querySnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                notifications = querySnapshot.documents.mapNotNull { doc ->
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
                        Log.w("NotificationScreen", "Error parsing notification ${doc.id}", e)
                        null
                    }
                }
                isLoading = false
            } catch (e: Exception) {
                Log.e("NotificationScreen", "Failed to load notifications", e)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notifications found.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            if (!notification.isReaded) {
                                // Mark notification as read
                                firestore.collection("users")
                                    .document(userId!!)
                                    .collection("notifications")
                                    .document(notification.id)
                                    .update("isReaded", true)
                                    .addOnSuccessListener {
                                        // Update local state
                                        notifications = notifications.map {
                                            if (it.id == notification.id) it.copy(isReaded = true)
                                            else it
                                        }
                                        Log.d("NotificationScreen", "Notification ${notification.id} marked as read")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("NotificationScreen", "Failed to mark notification ${notification.id} as read", e)
                                    }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isReaded) Color.White else Color(0xFFFFF9C4) // Light yellow for unread
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "From: ${notification.from}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${if (notification.isReaded) "Read" else "Unread"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (notification.isReaded) Color.Gray else Color(0xFF1976D2)
            )
        }
    }
}