package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

data class EmployeeWithRating(
    val user: User,
    val averageRating: Double
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val employerId = currentUser?.uid ?: ""
    var employees by remember { mutableStateOf<List<EmployeeWithRating>>(emptyList()) }
    var filteredEmployees by remember { mutableStateOf<List<EmployeeWithRating>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedEmployee by remember { mutableStateOf<User?>(null) }
    var selectedEducation by remember { mutableStateOf<EducationLevel?>(null) }
    var selectedLanguage by remember { mutableStateOf<LanguageCertificate?>(null) }
    var educationExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch employees and their ratings
    LaunchedEffect(Unit) {
        try {
            // Fetch all users with userType == EMPLOYEE
            val userDocs = firestore.collection("users")
                .whereEqualTo("userType", UserType.EMPLOYEE)
                .get()
                .await()
            val employeeList = mutableListOf<EmployeeWithRating>()
            for (doc in userDocs) {
                try {
                    val user = doc.toObject(User::class.java)
                    // Fetch ratings for the user
                    val ratingDocs = firestore.collection("users")
                        .document(user.uid)
                        .collection("rated")
                        .get()
                        .await()
                    val ratings = ratingDocs.documents.mapNotNull { ratingDoc ->
                        try {
                            ratingDoc.toObject(Rating::class.java)
                        } catch (e: Exception) {
                            Log.w("HiringScreen", "Error parsing rating ${ratingDoc.id}", e)
                            null
                        }
                    }
                    val averageRating = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
                    employeeList.add(EmployeeWithRating(user, averageRating))
                } catch (e: Exception) {
                    Log.w("HiringScreen", "Error parsing user ${doc.id}", e)
                }
            }
            employees = employeeList.sortedBy { it.user.name }
            filteredEmployees = employees // Initialize filtered list
            isLoading = false
        } catch (e: Exception) {
            Log.e("HiringScreen", "Failed to load employees", e)
            isLoading = false
        }
    }

    // Apply filters
    LaunchedEffect(selectedEducation, selectedLanguage) {
        filteredEmployees = employees.filter { employee ->
            val matchesEducation = selectedEducation?.let { employee.user.education == it } ?: true
            val matchesLanguage = selectedLanguage?.let { employee.user.languageCertificate == it } ?: true
            matchesEducation && matchesLanguage
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Hire Employees",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Filter Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Education Filter
                ExposedDropdownMenuBox(
                    expanded = educationExpanded,
                    onExpandedChange = { educationExpanded = !educationExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedEducation?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "All Education",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = educationExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenMain,
                            unfocusedBorderColor = GrayText,
                            cursorColor = GreenMain
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = educationExpanded,
                        onDismissRequest = { educationExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Education") },
                            onClick = {
                                selectedEducation = null
                                educationExpanded = false
                            }
                        )
                        EducationLevel.values().forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.name.replace("_", " ").lowercase().capitalize()) },
                                onClick = {
                                    selectedEducation = level
                                    educationExpanded = false
                                }
                            )
                        }
                    }
                }

                // Language Certificate Filter
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedLanguage?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "All Certificates",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenMain,
                            unfocusedBorderColor = GrayText,
                            cursorColor = GreenMain
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Certificates") },
                            onClick = {
                                selectedLanguage = null
                                languageExpanded = false
                            }
                        )
                        LanguageCertificate.values().forEach { cert ->
                            DropdownMenuItem(
                                text = { Text(cert.name.replace("_", " ").lowercase().capitalize()) },
                                onClick = {
                                    selectedLanguage = cert
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Employee List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenMain)
                }
            } else if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No employees match the filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrayText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEmployees) { employee ->
                        EmployeeCard(
                            employee = employee,
                            onInviteClick = { selectedEmployee = employee.user }
                        )
                    }
                }
            }
        }

        // Job Selection Dialog
        selectedEmployee?.let { employee ->
            JobSelectionDialog(
                employerId = employerId,
                employee = employee,
                onDismiss = { selectedEmployee = null },
                onInvite = { job ->
                    coroutineScope.launch {
                        try {
                            // Add employee to job's employees subcollection
                            val employeeData = mapOf(
                                "id" to employee.uid,
                                "jobState" to JobState.INVITING.name,
                                "receiveSalary" to false
                            )
                            firestore.collection("jobs")
                                .document(job.id)
                                .collection("employees")
                                .document(employee.uid)
                                .set(employeeData)
                                .await()

                            // Create notification for the employee
                            val notificationId = UUID.randomUUID().toString()
                            val notification = Notification(
                                id = notificationId,
                                title = "Job Invitation",
                                content = "You have been invited to join '${job.name}' by ${currentUser?.displayName ?: "Employer"}.",
                                from = employerId,
                                isReaded = false,
                                timestamp = System.currentTimeMillis()
                            )
                            firestore.collection("users")
                                .document(employee.uid)
                                .collection("notifications")
                                .document(notificationId)
                                .set(notification)
                                .await()

                            Log.d("HiringScreen", "Successfully invited ${employee.name} to job ${job.id}")
                        } catch (e: Exception) {
                            Log.e("HiringScreen", "Failed to invite employee", e)
                        }
                        selectedEmployee = null
                    }
                }
            )
        }
    }
}

@Composable
fun EmployeeCard(
    employee: EmployeeWithRating,
    onInviteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Could navigate to ProfileScreen if needed */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                if (!employee.user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = employee.user.avatarUrl,
                        contentDescription = "${employee.user.name}'s avatar",
                        modifier = Modifier
                            .size(48.dp)
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
                            .size(32.dp)
                            .padding(8.dp)
                    )
                }
            }

            // Employee Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = employee.user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Education: ${employee.user.education.name.replace("_", " ").lowercase().capitalize()}",
                    fontSize = 14.sp,
                    color = GrayText
                )
                Text(
                    text = "Language: ${employee.user.languageCertificate.name.replace("_", " ").lowercase().capitalize()}",
                    fontSize = 14.sp,
                    color = GrayText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Average Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (employee.averageRating == 0.0) "No ratings"
                        else "${"%.1f".format(employee.averageRating)} / 5",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                }
            }

            // Invite Button
            IconButton(
                onClick = onInviteClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(GreenMain, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Invite",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class JobWithEmployeeCount(
    val job: Job,
    val employeeCount: Int
)

@Composable
fun JobSelectionDialog(
    employerId: String,
    employee: User,
    onDismiss: () -> Unit,
    onInvite: (Job) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var jobs by remember { mutableStateOf<List<JobWithEmployeeCount>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val currentDate = LocalDate.now()

    // Fetch employer's jobs and filter by dateEnd
    LaunchedEffect(employerId) {
        try {
            val jobDocs = firestore.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            val jobList = mutableListOf<JobWithEmployeeCount>()
            for (doc in jobDocs) {
                try {
                    val job = doc.toObject(Job::class.java)
                    // Filter jobs where dateEnd is today or in the future
                    val dateEnd = try {
                        LocalDate.parse(job?.dateEnd, dateFormatter)
                    } catch (e: Exception) {
                        Log.w("JobSelectionDialog", "Invalid dateEnd format for job ${doc.id}", e)
                        null
                    }
                    if (dateEnd != null && !dateEnd.isBefore(currentDate)) {
                        // Fetch employee count from employees subcollection
                        val employeeDocs = firestore.collection("jobs")
                            .document(doc.id)
                            .collection("employees")
                            .get()
                            .await()
                        val employeeCount = employeeDocs.size()
                        jobList.add(JobWithEmployeeCount(job, employeeCount))
                    }
                } catch (e: Exception) {
                    Log.w("JobSelectionDialog", "Error parsing job ${doc.id}", e)
                }
            }
            jobs = jobList
            isLoading = false
        } catch (e: Exception) {
            Log.e("JobSelectionDialog", "Failed to load jobs", e)
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Invite ${employee.name} to Job",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenMain)
                    }
                } else if (jobs.isEmpty()) {
                    Text(
                        text = "No active jobs available",
                        fontSize = 14.sp,
                        color = GrayText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(jobs) { jobWithCount ->
                            JobItem(
                                job = jobWithCount.job,
                                employeeCount = jobWithCount.employeeCount,
                                onInvite = { onInvite(jobWithCount.job) }
                            )
                        }
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun JobItem(
    job: Job,
    employeeCount: Int,
    onInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GreenMain.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = job.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "$employeeCount / ${job.employeeRequired} employees",
                    fontSize = 14.sp,
                    color = GrayText
                )
            }
            Button(
                onClick = onInvite,
                modifier = Modifier
                    .height(36.dp)
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenMain,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Invite",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}