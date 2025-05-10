package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.data.models.Rating
import com.example.quickwork.data.models.User
import com.example.quickwork.data.models.UserType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter


private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        user?.name ?: "Profile",
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
        containerColor = GreenLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenMain)
                }
            } else if (user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "User not found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Red
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // User Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar and Name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GreenMain.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!user!!.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user!!.avatarUrl,
                                                contentDescription = "${user!!.name}'s avatar",
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                                placeholder = null,

                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Default avatar",
                                                tint = GreenMain,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = user!!.name,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                // User Details
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User Type",
                                        tint = GreenMain,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Type: ${user!!.userType.name.replace("_", " ").lowercase().capitalize()}",
                                        fontSize = 14.sp,
                                        color = GrayText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = GreenMain,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Email: ${user!!.email}",
                                        fontSize = 14.sp,
                                        color = GrayText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        tint = GreenMain,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Phone: ${user!!.phone}",
                                        fontSize = 14.sp,
                                        color = GrayText
                                    )
                                }

                                // Employee-specific fields
                                if (user!!.userType == UserType.EMPLOYEE) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Employee Details",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Education",
                                            tint = GreenMain,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Education: ${user!!.education}",
                                            fontSize = 14.sp,
                                            color = GrayText
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Language Certificate",
                                            tint = GreenMain,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Language Certificate: ${user!!.languageCertificate}",
                                            fontSize = 14.sp,
                                            color = GrayText
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Work,
                                            contentDescription = "Jobs",
                                            tint = GreenMain,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Jobs: ${user!!.jobList.joinToString(", ") { it.ifEmpty { "None" } }}",
                                            fontSize = 14.sp,
                                            color = GrayText
                                        )
                                    }
                                }

                                // Employer-specific fields
                                if (user!!.userType == UserType.EMPLOYER) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Employer Details",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = "Company",
                                            tint = GreenMain,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Company: ${user!!.companyName}",
                                            fontSize = 14.sp,
                                            color = GrayText
                                        )
                                    }
                                }

                                // Contact Button
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.navigate("chat/$userId") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(4.dp, RoundedCornerShape(8.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenMain,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Contact",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Ratings Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Ratings",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                // Average Rating
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Average Rating",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (ratings.isEmpty()) "No ratings yet"
                                        else "Average: ${"%.1f".format(averageRating)} / 5",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                }
                                // Ratings List or No Ratings Message
                                if (ratings.isEmpty()) {
                                    Text(
                                        text = "No ratings available",
                                        fontSize = 14.sp,
                                        color = GrayText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
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
    }
}

@Composable
fun RatingItem(rating: Rating) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val displayDate = try {
        LocalDate.parse(rating.date, dateFormatter).format(displayFormatter)
    } catch (e: Exception) {
        rating.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GreenMain.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rating.jobName.ifEmpty { "Unknown Job" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = displayDate,
                    fontSize = 12.sp,
                    color = GrayText
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "$i star",
                        tint = if (i <= rating.stars) Color(0xFFFFD700) else GrayText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = rating.comment.ifEmpty { "No comment provided" },
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 20.sp
            )
            Text(
                text = "Rated by: ${rating.ratedId}",
                fontSize = 12.sp,
                color = GrayText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}