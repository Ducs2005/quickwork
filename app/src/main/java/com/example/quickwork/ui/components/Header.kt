package com.example.quickwork.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.quickwork.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()
    var unreadCount by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }

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

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            confirmButton = {
                TextButton(onClick = {
                    navController.navigate("jobSearchResult/${keyword.trim()}")
                    showSearch = false
                    keyword = ""
                }) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearch = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Search Job") },
            text = {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Enter keyword") },
                    singleLine = true
                )
            }
        )
    }

    TopAppBar(
        title = { /* Empty title */ },
        modifier = Modifier.background(Color.White),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
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
                    contentDescription = "JobsGO Logo",
                    modifier = Modifier.size(60.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search Icon"
                        )
                    }

                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(unreadCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate("notification") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notifications),
                                contentDescription = "Notifications Icon"
                            )
                        }
                    }
                }
            }
        }
    )
}
