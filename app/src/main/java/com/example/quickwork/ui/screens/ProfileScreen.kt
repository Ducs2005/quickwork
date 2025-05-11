package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userId: String?, navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var user by remember { mutableStateOf<User?>(null) }
    var ratings by remember { mutableStateOf<List<Rating>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var editedUser by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Determine effective user ID (current user if userId is null/empty)
    val effectiveUserId = userId.takeIf { !it.isNullOrEmpty() } ?: auth.currentUser?.uid
    val isCurrentUser = effectiveUserId == auth.currentUser?.uid
    val context = LocalContext.current

    // Calculate average rating
    val averageRating by remember(ratings) {
        derivedStateOf {
            if (ratings.isEmpty()) 0.0
            else ratings.map { it.stars }.average()
        }
    }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Image picker for avatar
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Fetch user data and ratings
    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId == null) {
            loading = false
            errorMessage = "No user logged in"
            return@LaunchedEffect
        }
        try {
            // Fetch user data
            val userDoc = firestore.collection("users").document(effectiveUserId).get().await()
            user = userDoc.toObject(User::class.java)?.copy(uid = effectiveUserId)
            if (isCurrentUser && user != null) {
                editedUser = user // Initialize editable user
            }

            // Fetch ratings
            val ratingDocs = firestore.collection("users")
                .document(effectiveUserId)
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
            errorMessage = "Failed to load profile: ${e.message}"
        } finally {
            loading = false
        }
    }

    // Upload avatar to Cloudinary when selected
    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null && effectiveUserId != null) {
            try {
                var downloadUrl = ""
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                            // Switch to IO thread for network operation
                            loading = true
                            withContext(Dispatchers.IO) {
                            // Perform your network operation here
                            // Example: uploading to Cloudinary
                            downloadUrl = uploadImageToCloudinary(context, selectedImageUri!!).toString()
                            if (downloadUrl != null) {
                            editedUser = editedUser?.copy(avatarUrl = downloadUrl)
                            // Update Firestore immediately
                            firestore.collection("users").document(effectiveUserId)
                                .update("avatarUrl", downloadUrl).await()
                            user = user?.copy(avatarUrl = downloadUrl)
                            loading = false

                            } else {
                            errorMessage = "Failed to upload avatar"
                        }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()  // Handle exceptions here
                    }
                }


            } catch (e: Exception) {
                errorMessage = "Failed to upload avatar: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        user?.name ?: if (isCurrentUser) "My Profile" else "Profile",
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
                actions = {
                    if (isCurrentUser && !isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White
                            )
                        }
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
                        text = errorMessage ?: "User not found",
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
                                            .then(
                                                if (isCurrentUser && isEditing) {
                                                    Modifier.clickable { imagePicker.launch("image/*") }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        if (!user!!.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user!!.avatarUrl,
                                                contentDescription = "${user!!.name}'s avatar",
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                                placeholder = null
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
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editedUser?.name ?: "",
                                            onValueChange = { editedUser = editedUser?.copy(name = it) },
                                            label = { Text("Name") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = user!!.name,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
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
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editedUser?.phone ?: "",
                                            onValueChange = { editedUser = editedUser?.copy(phone = it) },
                                            label = { Text("Phone") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = "Phone: ${user!!.phone}",
                                            fontSize = 14.sp,
                                            color = GrayText
                                        )
                                    }
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
                                        if (isEditing) {
                                            EducationLevelDropdown(
                                                selectedLevel = editedUser?.education ?: EducationLevel.NONE,
                                                onLevelChange = { editedUser = editedUser?.copy(education = it) }
                                            )
                                        } else {
                                            Text(
                                                text = "Education: ${user!!.education}",
                                                fontSize = 14.sp,
                                                color = GrayText
                                            )
                                        }
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
                                        if (isEditing) {
                                            LanguageCertificateDropdown(
                                                selectedCertificate = editedUser?.languageCertificate ?: LanguageCertificate.NONE,
                                                onCertificateChange = { editedUser = editedUser?.copy(languageCertificate = it) }
                                            )
                                        } else {
                                            Text(
                                                text = "Language Certificate: ${user!!.languageCertificate}",
                                                fontSize = 14.sp,
                                                color = GrayText
                                            )
                                        }
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
                                        if (isEditing) {
                                            OutlinedTextField(
                                                value = editedUser?.companyName ?: "",
                                                onValueChange = { editedUser = editedUser?.copy(companyName = it) },
                                                label = { Text("Company Name") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = "Company: ${user!!.companyName}",
                                                fontSize = 14.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                }

                                // Edit/Save/Cancel Buttons
                                if (isEditing) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    errorMessage?.let {
                                        Text(
                                            text = it,
                                            color = Color.Red,
                                            fontSize = 14.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(
                                            onClick = {
                                                isEditing = false
                                                editedUser = user // Reset to original
                                                selectedImageUri = null
                                                errorMessage = null
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .padding(end = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Gray,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Cancel", fontSize = 16.sp)
                                        }
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        // Validate
                                                        if (editedUser?.name?.isBlank() == true) {
                                                            errorMessage = "Name is required"
                                                            return@launch
                                                        }
                                                        if (editedUser?.phone?.isBlank() == true) {
                                                            errorMessage = "Phone is required"
                                                            return@launch
                                                        }
                                                        if (user!!.userType == UserType.EMPLOYER && editedUser?.companyName?.isBlank() == true) {
                                                            errorMessage = "Company Name is required"
                                                            return@launch
                                                        }

                                                        // Update Firebase Auth profile
                                                        auth.currentUser?.updateProfile(
                                                            UserProfileChangeRequest.Builder()
                                                                .setDisplayName(editedUser?.name)
                                                                .setPhotoUri(editedUser?.avatarUrl?.let { Uri.parse(it) })
                                                                .build()
                                                        )?.await()

                                                        // Update Firestore
                                                        firestore.collection("users").document(effectiveUserId!!)
                                                            .set(editedUser!!).await()

                                                        // Update UI
                                                        user = editedUser
                                                        isEditing = false
                                                        errorMessage = null
                                                    } catch (e: Exception) {
                                                        errorMessage = "Failed to save changes: ${e.message}"
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .padding(start = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GreenMain,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Save", fontSize = 16.sp)
                                        }
                                    }
                                } else if (!isCurrentUser) {
                                    // Contact Button for other users
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { navController.navigate("chat/$effectiveUserId") },
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
//
//@Composable
//fun EducationLevelDropdown(
//    selectedLevel: EducationLevel,
//    onLevelChange: (EducationLevel) -> Unit
//) {
//    var expanded by remember { mutableStateOf(false) }
//    Box {
//        OutlinedTextField(
//            value = selectedLevel.name.replace("_", " ").lowercase().capitalize(),
//            onValueChange = {},
//            label = { Text("Education Level") },
//            shape = RoundedCornerShape(8.dp),
//            modifier = Modifier.fillMaxWidth(),
//            readOnly = true,
//            trailingIcon = {
//                IconButton(onClick = { expanded = !expanded }) {
//                    Icon(
//                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
//                        contentDescription = "Toggle dropdown"
//                    )
//                }
//            }
//        )
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            EducationLevel.values().forEach { level ->
//                DropdownMenuItem(
//                    text = { Text(level.name.replace("_", " ").lowercase().capitalize()) },
//                    onClick = {
//                        onLevelChange(level)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun LanguageCertificateDropdown(
//    selectedCertificate: LanguageCertificate,
//    onCertificateChange: (LanguageCertificate) -> Unit
//) {
//    var expanded by remember { mutableStateOf(false) }
//    Box {
//        OutlinedTextField(
//            value = selectedCertificate.name.replace("_", " ").lowercase().capitalize(),
//            onValueChange = {},
//            label = { Text("Language Certificate") },
//            shape = RoundedCornerShape(8.dp),
//            modifier = Modifier.fillMaxWidth(),
//            readOnly = true,
//            trailingIcon = {
//                IconButton(onClick = { expanded = !expanded }) {
//                    Icon(
//                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
//                        contentDescription = "Toggle dropdown"
//                    )
//                }
//            }
//        )
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            LanguageCertificate.values().forEach { cert ->
//                DropdownMenuItem(
//                    text = { Text(cert.name.replace("_", " ").lowercase().capitalize()) },
//                    onClick = {
//                        onCertificateChange(cert)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}