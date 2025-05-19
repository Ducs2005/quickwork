package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.R
import com.example.quickwork.data.models.Message
import com.example.quickwork.ui.viewmodels.ChatUiState
import com.example.quickwork.ui.viewmodels.ChatViewModel
import com.example.quickwork.ui.viewmodels.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    receiverId: String,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
        receiverId = receiverId
    ))
) {
    val uiState by viewModel.uiState.collectAsState()
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to latest message when messages change
    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat with ${uiState.receiverName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
        containerColor = GreenLight
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenMain)
                }
            } else if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayText,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.first.id }) { (message, isSent) ->
                        MessageItem(
                            message = message,
                            isSent = isSent,
                            senderAvatarUrl = if (isSent) uiState.senderAvatarUrl else uiState.receiverAvatarUrl
                        )
                    }
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("Type a message", color = GrayText, fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = GreenMain,
                        unfocusedIndicatorColor = GrayText.copy(alpha = 0.5f),
                        cursorColor = GreenMain
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            viewModel.sendMessage(newMessage)
                            newMessage = ""
                        }
                    },
                    enabled = newMessage.isNotBlank()
                ) {
                    val tint by animateColorAsState(
                        targetValue = if (newMessage.isNotBlank()) GreenMain else GrayText,
                        label = "Send button tint"
                    )
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageItem(message: Message, isSent: Boolean, senderAvatarUrl: String?) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val displayDate = try {
        val dateTime = LocalDateTime.parse(message.date, dateFormatter)
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(dateTime.toLocalDate(), today)
        when {
            daysDiff == 0L -> "Today, ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            daysDiff == 1L -> "Yesterday, ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            else -> dateTime.format(displayFormatter)
        }
    } catch (e: Exception) {
        message.date
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for received messages (left side)
        if (!isSent) {
            Surface(
                shape = CircleShape,
                color = GreenMain.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                if (!senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = senderAvatarUrl,
                        contentDescription = "Receiver avatar",
                        modifier = Modifier
                            .size(40.dp)
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
                            .size(24.dp)
                            .padding(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(
                topStart = if (isSent) 12.dp else 0.dp,
                topEnd = if (isSent) 0.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSent) GreenMain.copy(alpha = 0.2f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        fontSize = 12.sp
                    )
                    if (isSent && message.isReaded) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Read",
                            tint = GreenMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Read",
                            tint = GreenMain,
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = (-8).dp)
                        )
                    }
                }
            }
        }
    }
}