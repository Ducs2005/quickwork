package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MessageRoom(
    val receiverId: String,
    val receiverName: String,
    val latestMessage: Message? // Latest sent message
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRoomsScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()
    var messageRooms by remember { mutableStateOf<List<MessageRoom>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // Set up real-time listener
            listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("messageRooms")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageRoomsScreen", "Error listening to message rooms", error)
                        isLoading = false
                        return@addSnapshotListener
                    }

                    coroutineScope.launch {
                        val rooms = mutableListOf<MessageRoom>()
                        snapshot?.documents?.forEach { doc ->
                            val receiverId = doc.id
                            try {
                                // Fetch receiver's name
                                val userDoc = firestore.collection("users")
                                    .document(receiverId)
                                    .get()
                                    .await()
                                val receiverName = userDoc.getString("name") ?: "Unknown"

                                // Fetch latest sent message
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
                                        isReaded = msgDoc.getBoolean("isReaded") ?: false,

                                    )
                                }

                                rooms.add(MessageRoom(receiverId, receiverName, latestMessage))
                            } catch (e: Exception) {
                                Log.w("MessageRoomsScreen", "Error processing room $receiverId", e)
                            }
                        }
                        messageRooms = rooms.sortedByDescending { it.latestMessage?.date }
                        isLoading = false
                    }
                }
        } catch (e: Exception) {
            Log.e("MessageRoomsScreen", "Failed to set up listener", e)
            isLoading = false
        }
    }

    // Clean up listener on dispose
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
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
        } else if (messageRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No message rooms found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(messageRooms) { room ->
                    MessageRoomItem(
                        room = room,
                        onClick = { navController.navigate("chat/${room.receiverId}") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MessageRoomItem(room: MessageRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = room.receiverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = room.latestMessage?.content ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = room.latestMessage?.date ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}