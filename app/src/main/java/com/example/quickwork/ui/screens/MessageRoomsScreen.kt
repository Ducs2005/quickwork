package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.R
import com.example.quickwork.data.models.MessageRoom
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.viewmodels.MessageRoomsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRoomsScreen(
    navController: NavController,
    viewModel: MessageRoomsViewModel = viewModel()
) {
    val messageRooms by viewModel.messageRooms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Messages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenMain,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        bottomBar = {
            if (userRole.equals("employee", ignoreCase = true)) {
                BottomNavigation(
                    navController,
                    currentScreen = "chatRoom"
                )
            } else {
                ReusableBottomNavBar(navController = navController)
            }
        },
        containerColor = GreenLight
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else if (messageRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No conversations found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayText,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messageRooms) { room ->
                    MessageRoomItem(
                        room = room,
                        onClick = { navController.navigate("chat/${room.receiverId}") }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageRoomItem(room: MessageRoom, onClick: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM dd")
    val isUnread = room.latestMessage?.isReaded == false

    // Format timestamp
    val displayDate = room.latestMessage?.date?.let { dateStr ->
        try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(date, today)
            when {
                daysDiff == 0L -> "Today"
                daysDiff == 1L -> "Yesterday"
                else -> date.format(displayFormatter)
            }
        } catch (e: Exception) {
            dateStr
        }
    } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = GreenMain.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                if (!room.receiverAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = room.receiverAvatarUrl,
                        contentDescription = "${room.receiverName}'s avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_default_avatar),
                        error = painterResource(id = R.drawable.ic_default_avatar)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default avatar",
                        tint = GreenMain,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(8.dp)
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.receiverName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.latestMessage?.content ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayText,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = GreenMain,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }
                }
            }
        }
    }
}