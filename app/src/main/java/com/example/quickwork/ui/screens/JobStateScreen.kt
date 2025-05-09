package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.quickwork.data.models.JobState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobStateScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var jobList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var employeeStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var selectedJobId by remember { mutableStateOf<String?>(null) }
    var attendanceList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var receiveSalary by remember { mutableStateOf(false) }

    // Formatter for parsing dates and times
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Fetch user's jobs and states, update ENDED state if dateEnd is past
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                // Get user's jobList
                val userDoc = firestore.collection("users").document(userId).get().await()
                val jobIds = userDoc.get("jobList") as? List<String> ?: emptyList()

                // Fetch all jobs and employee states
                val fetchedJobs = mutableListOf<Map<String, Any>>()
                val fetchedStates = mutableMapOf<String, String>()
                for (jobId in jobIds) {
                    // Fetch job
                    val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                    if (jobDoc.exists()) {
                        jobDoc.data?.let { jobData ->
                            fetchedJobs.add(jobData)
                            // Fetch employee state
                            val employeeDoc = firestore.collection("jobs")
                                .document(jobId)
                                .collection("employees")
                                .document(userId)
                                .get()
                                .await()
                            var state = if (employeeDoc.exists()) {
                                employeeDoc.getString("jobState") ?: "UNKNOWN"
                            } else {
                                "NOT_APPLIED"
                            }

                            // Check if dateEnd is before today and state is not ENDED
                            val dateEndStr = jobData["dateEnd"] as? String
                            if (dateEndStr != null && state != JobState.ENDED.name) {
                                try {
                                    val dateEnd = LocalDate.parse(dateEndStr, dateFormatter)
                                    val today = LocalDate.now()
                                    if (dateEnd.isBefore(today)) {
                                        // Update jobState to ENDED in Firestore
                                        firestore.collection("jobs")
                                            .document(jobId)
                                            .collection("employees")
                                            .document(userId)
                                            .set(mapOf("jobState" to JobState.ENDED.name))
                                            .await()
                                        state = JobState.ENDED.name
                                        Log.d("JobStateScreen", "Updated job $jobId to ENDED")
                                    }
                                } catch (e: Exception) {
                                    Log.w("JobStateScreen", "Invalid dateEnd format for job $jobId", e)
                                }
                            }
                            fetchedStates[jobId] = state
                        }
                    }
                }
                jobList = fetchedJobs
                employeeStates = fetchedStates
            } catch (e: Exception) {
                Log.e("JobStateScreen", "Failed to load jobs or employee states", e)
            } finally {
                loading = false
            }
        } else {
            loading = false
        }
    }

    // Fetch attendance and receiveSalary when a job is selected
    LaunchedEffect(selectedJobId) {
        if (selectedJobId != null && userId != null) {
            try {
                // Fetch attendance
                val attendanceDocs = firestore.collection("jobs")
                    .document(selectedJobId!!)
                    .collection("employees")
                    .document(userId)
                    .collection("attendance")
                    .get()
                    .await()
                attendanceList = attendanceDocs.documents.mapNotNull { it.data }

                // Fetch receiveSalary
                val employeeDoc = firestore.collection("jobs")
                    .document(selectedJobId!!)
                    .collection("employees")
                    .document(userId)
                    .get()
                    .await()
                receiveSalary = employeeDoc.getBoolean("receiveSalary") ?: false
            } catch (e: Exception) {
                Log.e("JobScreen", "Failed to load attendance or receiveSalary", e)
                attendanceList = emptyList()
                receiveSalary = false
            }
        }
    }

    // Filter jobs by tab
    val applyingJobs = jobList.filter { employeeStates[it["id"] as? String] == JobState.APPLYING.name }
    val presentWorkingJobs = jobList.filter {
        val state = employeeStates[it["id"] as? String]
        state == JobState.PRESENT.name || state == JobState.WORKING.name
    }
    val endedJobs = jobList.filter { employeeStates[it["id"] as? String] == JobState.ENDED.name }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addJobScreen") },
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Job"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TabRow for navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ) {
                listOf("Applying", "Present & Working", "Ended").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedTabIndex == index) Color.White else Color(0xFFB0BEC5)
                            )
                        }
                    )
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val jobsToShow = when (selectedTabIndex) {
                    0 -> applyingJobs
                    1 -> presentWorkingJobs
                    2 -> endedJobs
                    else -> emptyList()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobsToShow) { job ->
                        val jobId = job["id"] as? String ?: ""
                        JobCard(
                            job = job,
                            state = employeeStates[jobId] ?: "UNKNOWN",
                            onClick = { selectedJobId = jobId }
                        )
                    }
                    if (jobsToShow.isEmpty()) {
                        item {
                            Text(
                                text = "No jobs found",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Attendance Dialog
            val selectedId = selectedJobId
            if (selectedId != null) {
                AttendanceDialog(
                    attendanceList = attendanceList,
                    job = jobList.find { it["id"] as? String == selectedJobId },
                    isEnded = employeeStates[selectedJobId] == JobState.ENDED.name,
                    receiveSalary = receiveSalary,
                    onClaimSalary = { stars, comment ->
                        if (userId != null && selectedId != null) {
                            val job = jobList.find { it["id"] as? String == selectedId }
                            val jobName = job?.get("name") as? String ?: "Unknown"
                            val employerId = job?.get("employerId") as? String ?: "system"
                            val ratingData = mapOf(
                                "stars" to stars,
                                "comment" to comment,
                                "jobName" to jobName,
                                "date" to LocalDate.now().format(dateFormatter),
                                "ratedId" to userId
                            )
                            // Save rating to employer's rated subcollection
                            firestore.collection("users")
                                .document(employerId)
                                .collection("rated")
                                .add(ratingData)
                                .addOnSuccessListener {
                                    // Update receiveSalary
                                    firestore.collection("jobs")
                                        .document(selectedId)
                                        .collection("employees")
                                        .document(userId)
                                        .update("receiveSalary", true)
                                        .addOnSuccessListener {
                                            receiveSalary = true
                                            Log.d("JobStateScreen", "Salary claimed and rating saved for job $selectedId")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("JobStateScreen", "Failed to claim salary", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("JobStateScreen", "Failed to save rating", e)
                                }
                        }
                    },
                    onDismiss = { selectedJobId = null }
                )
            }
        }
    }
}

@Composable
fun JobCard(job: Map<String, Any>, state: String, onClick: () -> Unit) {
    val name = job["name"] as? String ?: "N/A"
    val company = job["companyName"] as? String ?: "Unknown"
    val dateStart = job["dateStart"] as? String ?: "-"
    val dateEnd = job["dateEnd"] as? String ?: "-"
    val timeStart = job["workingHoursStart"] as? String ?: "-"
    val timeEnd = job["workingHoursEnd"] as? String ?: "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Company: $company",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Text(
                text = "Date: $dateStart → $dateEnd",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = "Time: $timeStart → $timeEnd",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "State: $state",
                style = MaterialTheme.typography.bodyMedium,
                color = when (state) {
                    "APPLYING" -> Color(0xFF4CAF50) // Green
                    "PRESENT" -> Color.Green
                    "WORKING" -> Color.Green
                    "ENDED" -> Color.Red
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceDialog(
    attendanceList: List<Map<String, Any>>,
    job: Map<String, Any>?,
    isEnded: Boolean,
    receiveSalary: Boolean,
    onClaimSalary: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Calculate salary for ended jobs
    val hourlyRate = (job?.get("salary") as? Long)?.toDouble() ?: 10.0
    var totalHours = 0.0
    var presentDays = 0
    var lateDays = 0
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showRatingDialog by remember { mutableStateOf(false) }

    if (isEnded && job != null) {
        val startTimeStr = job["workingHoursStart"] as? String
        val endTimeStr = job["workingHoursEnd"] as? String
        if (startTimeStr != null && endTimeStr != null) {
            try {
                val startTime = LocalTime.parse(startTimeStr, timeFormatter)
                val endTime = LocalTime.parse(endTimeStr, timeFormatter)
                val hoursPerDay = java.time.Duration.between(startTime, endTime).toHours().toDouble()
                attendanceList.forEach { attendance ->
                    when (attendance["status"] as? String) {
                        "PRESENT" -> {
                            totalHours += hoursPerDay
                            presentDays++
                        }
                        "LATE" -> {
                            totalHours += hoursPerDay * 0.5
                            lateDays++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AttendanceDialog", "Invalid time format", e)
            }
        }
    }
    val totalSalary = totalHours * hourlyRate

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Attendance Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (attendanceList.isEmpty()) {
                    Text(
                        text = "No attendance records found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(attendanceList) { attendance ->
                            val date = attendance["date"] as? String ?: "-"
                            val status = attendance["status"] as? String ?: "UNKNOWN"
                            AttendanceItem(date = date, status = status)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Salary details for ended jobs
                if (isEnded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Salary Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Present Days: $presentDays",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "Late Days: $lateDays",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "Total Hours: ${"%.2f".format(totalHours)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "Total Salary: $${"%.2f".format(totalSalary)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    if (!receiveSalary) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showRatingDialog = true },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Claim Salary")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Salary Claimed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Green,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Close")
                }
            }
        }
    }

    // Rating Dialog
    if (showRatingDialog) {
        RatingDialog(
            onSubmit = { stars, comment ->
                onClaimSalary(stars, comment)
                showRatingDialog = false
            },
            onCancel = { showRatingDialog = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RatingDialog(
    onSubmit: (Int, String) -> Unit,
    onCancel: () -> Unit
) {
    var stars by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rate Employee",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Star Rating
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "$i star",
                            tint = if (i <= stars) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { stars = i }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comment Input
                Text(
                    text = "Comment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    decorationBox = { innerTextField ->
                        if (comment.isEmpty()) {
                            Text(
                                text = "Enter your comment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (stars > 0) onSubmit(stars, comment) },
                        enabled = stars > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceItem(date: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = when (status) {
                    "PRESENT" -> Color(0xFF4CAF50) // Green
                    "LATE" -> Color(0xFFFF9800) // Orange
                    "ABSENT" -> Color.Red
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}