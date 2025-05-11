package com.example.quickwork.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickwork.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController

private val GreenMain = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Header(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()
    var unreadCount by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(userId) {
        if (userId != null) {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isReaded", false)
                .get()
                .await()
            unreadCount = snapshot.size()
        }
    }

    TopAppBar(
        title = {},
        modifier = Modifier
            .height(64.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.6f)
                    )
                )
            )
            .shadow(4.dp, clip = false)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_jobsgo),
                    contentDescription = "QuickWork Logo",
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = showSearch,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 2 } togetherWith
                                    fadeOut() + slideOutHorizontally { it / 2 })
                                .using(SizeTransform(clip = false))
                        },
                        label = "SearchFieldAnimation"
                    ) { isSearchActive ->
                        if (isSearchActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(200.dp)
                            ) {
                                TextField(
                                    value = keyword,
                                    onValueChange = { keyword = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    placeholder = { Text("Search jobs", fontSize = 16.sp) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = GreenMain
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (keyword.isNotBlank()) {
                                                navController.navigate("jobSearchResult/${keyword.trim()}/")
                                                keyword = ""
                                                showSearch = false
                                            }
                                        }
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                                )
                                IconButton(
                                    onClick = {
                                        keyword = ""
                                        showSearch = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_clear),
                                        contentDescription = "Clear Search",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { showSearch = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Search Icon",
                                    tint = GreenMain,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = GreenMain,
                                    modifier = Modifier
                                        .scale(pulseScale)
                                        .offset(x = (-4).dp, y = 4.dp)
                                ) {
                                    Text(
                                        unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { navController.navigate("notification") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notifications),
                                contentDescription = "Notifications Icon",
                                tint = GreenMain,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}