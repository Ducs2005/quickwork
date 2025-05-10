package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.User
import com.example.quickwork.data.models.UserType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Rating(
    val stars: Int = 0,
    val comment: String = "",
    val jobName: String = "",
    val date: String = "",
    val ratedId: String = ""
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userId: String, navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var user by remember { mutableStateOf<User?>(null) }
    var ratings by remember { mutableStateOf<List<Rating>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Calculate average rating
    val averageRating by remember(ratings) {
        derivedStateOf {
            if (ratings.isEmpty()) 0.0
            else ratings.map { it.stars }.average()
        }
    }

    // Fetch user data and ratings
    LaunchedEffect(userId) {
        try {
            // Fetch user data
            val userDoc = firestore.collection("users").document(userId).get().await()
            user = userDoc.toObject(User::class.java)

            // Fetch ratings
            val ratingDocs = firestore.collection("users")
                .document(userId)
                .collection("rated")
                .get()
                .await()
            ratings = ratingDocs.documents.mapNotNull { doc ->
                try {
                    Rating(
                        stars = doc.getLong("stars")?.toInt() ?: 0,
                        comment = doc.getString("comment") ?: "",
                        jobName = doc.getString("jobName") ?: "",
                        date = doc.getString("date") ?: "",
                        ratedId = doc.getString("ratedId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("ProfileScreen", "Error parsing rating ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Failed to load user data or ratings", e)
        } finally {
            loading = false
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (user == null) {
                Text(
                    text = "User not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // User Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = user!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Type: ${user!!.userType}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Email: ${user!!.email}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Phone: ${user!!.phone}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray
                        )

                        // Employee-specific fields
                        if (user!!.userType == UserType.EMPLOYEE) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Employee Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Education: ${user!!.education}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Language Certificate: ${user!!.languageCertificate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Jobs: ${user!!.jobList.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }

                        // Employer-specific fields
                        if (user!!.userType == UserType.EMPLOYER) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Employer Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Company: ${user!!.companyName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }

                        // Contact Button
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate("chat/$userId") },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Contact", fontSize = 14.sp)
                        }
                    }
                }

                // Ratings Card
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Ratings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Average Rating
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Average Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (ratings.isEmpty()) "No ratings available"
                                else "Average: ${"%.1f".format(averageRating)} / 5",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Ratings List or No Ratings Message
                        if (ratings.isEmpty()) {
                            Text(
                                text = "No ratings available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ratings) { rating ->
                                    RatingItem(rating)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingItem(rating: Rating) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = rating.jobName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = rating.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "$i star",
                        tint = if (i <= rating.stars) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rating.comment.ifEmpty { "No comment" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rated by: ${rating.ratedId}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}