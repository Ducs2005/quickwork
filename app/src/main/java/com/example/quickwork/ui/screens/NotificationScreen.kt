package com.example.quickwork.ui.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val GreenMain = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

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

    // Count unread notifications
    val unreadCount = notifications.count { !it.isReaded }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val batch = firestore.batch()
                                    notifications.filter { !it.isReaded }.forEach { notification ->
                                        val docRef = firestore.collection("users")
                                            .document(userId!!)
                                            .collection("notifications")
                                            .document(notification.id)
                                        batch.update(docRef, "isReaded", true)
                                    }
                                    batch.commit().await()
                                    notifications = notifications.map { it.copy(isReaded = true) }
                                    Log.d("NotificationScreen", "All notifications marked as read")
                                } catch (e: Exception) {
                                    Log.e("NotificationScreen", "Failed to mark all notifications as read", e)
                                }
                            }
                        }) {
                            Text(
                                text = "Mark All Read",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenMain,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.7f)
                )
            )
        )
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GreenMain,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            notifications.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No notifications",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check back later for updates!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                if (!notification.isReaded && userId != null) {
                                    coroutineScope.launch {
                                        try {
                                            firestore.collection("users")
                                                .document(userId)
                                                .collection("notifications")
                                                .document(notification.id)
                                                .update("isReaded", true)
                                                .await()
                                            notifications = notifications.map {
                                                if (it.id == notification.id) it.copy(isReaded = true)
                                                else it
                                            }
                                            Log.d("NotificationScreen", "Notification ${notification.id} marked as read")
                                        } catch (e: Exception) {
                                            Log.e("NotificationScreen", "Failed to mark notification ${notification.id} as read", e)
                                        }
                                    }
                                }
                            }
                        )
                        Divider(
                            color = Color.Gray.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "View notification details"
            )
            .scale(scale)
            .animateContentSize()
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isReaded) Color.White else Color(0xFFF1F8E9) // Light green for unread
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (notification.isReaded) Icons.Default.Notifications else Icons.Default.NotificationsActive,
                contentDescription = if (notification.isReaded) "Read notification" else "Unread notification",
                tint = if (notification.isReaded) Color.Gray else GreenMain,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "From: ${notification.from}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Status: ${if (notification.isReaded) "Read" else "Unread"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notification.isReaded) Color.Gray else GreenMain,
                    fontSize = 12.sp
                )
            }
        }
    }
}