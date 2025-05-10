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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.quickwork.data.models.JobState
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

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
    var selectedJobId by remember  { mutableStateOf<String?>(null) }
    var attendanceList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var receiveSalary by remember { mutableStateOf(false) }

    // Formatter for parsing dates and times
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val displayDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy") // e.g., Jan 01, 2025

    // Fetch user's jobs and states, update ENDED state if dateEnd is past
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val jobIds = userDoc.get("jobList") as? List<String> ?: emptyList()
                val fetchedJobs = mutableListOf<Map<String, Any>>()
                val fetchedStates = mutableMapOf<String, String>()
                for (jobId in jobIds) {
                    val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                    if (jobDoc.exists()) {
                        jobDoc.data?.let { jobData ->
                            fetchedJobs.add(jobData)
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
                            val dateEndStr = jobData["dateEnd"] as? String
                            if (dateEndStr != null && state != JobState.ENDED.name) {
                                try {
                                    val dateEnd = LocalDate.parse(dateEndStr, dateFormatter)
                                    val today = LocalDate.now()
                                    if (dateEnd.isBefore(today)) {
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
                val attendanceDocs = firestore.collection("jobs")
                    .document(selectedJobId!!)
                    .collection("employees")
                    .document(userId)
                    .collection("attendance")
                    .get()
                    .await()
                attendanceList = attendanceDocs.documents.mapNotNull { it.data }
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
        topBar = { Header(navController) },
        bottomBar = { BottomNavigation(navController, currentScreen = "jobState") },
        containerColor = GreenLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = GreenMain,
                contentColor = Color.White,
                modifier = Modifier.shadow(4.dp)
            ) {
                listOf("Applying", "Present & Working", "Ended").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTabIndex == index) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
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
                    CircularProgressIndicator(color = GreenMain)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobsToShow) { job ->
                        val jobId = job["id"] as? String ?: ""
                        JobCard(
                            job = job,
                            state = employeeStates[jobId] ?: "UNKNOWN",
                            onClick = { selectedJobId = jobId },
                            dateFormatter = displayDateFormatter
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
                                color = GrayText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            val selectedId = selectedJobId
            if (selectedId != null) {
                AttendanceDialog(
                    attendanceList = attendanceList,
                    job = jobList.find { it["id"] as? String == selectedJobId },
                    isEnded = employeeStates[selectedJobId] == JobState.ENDED.name,
                    receiveSalary = receiveSalary,
                    dateFormatter = displayDateFormatter,
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
                            firestore.collection("users")
                                .document(employerId)
                                .collection("rated")
                                .add(ratingData)
                                .addOnSuccessListener {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JobCard(job: Map<String, Any>, state: String, onClick: () -> Unit, dateFormatter: DateTimeFormatter) {
    val name = job["name"] as? String ?: "N/A"
    val company = job["companyName"] as? String ?: "Unknown"
    val dateStartStr = job["dateStart"] as? String ?: "-"
    val dateEndStr = job["dateEnd"] as? String ?: "-"
    val timeStart = job["workingHoursStart"] as? String ?: "-"
    val timeEnd = job["workingHoursEnd"] as? String ?: "-"

    // Format dates
    val dateStart = try {
        LocalDate.parse(dateStartStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(dateFormatter)
    } catch (e: Exception) {
        dateStartStr
    }
    val dateEnd = try {
        LocalDate.parse(dateEndStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(dateFormatter)
    } catch (e: Exception) {
        dateEndStr
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 18.sp
            )
            Text(
                text = "Company: $company",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                fontSize = 14.sp
            )
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//
//            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    tint = GreenMain,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStart → $dateEnd",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    fontSize = 13.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = GreenMain,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$timeStart → $timeEnd",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    fontSize = 13.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (state) {
                    "APPLYING" -> GreenMain.copy(alpha = 0.1f)
                    "PRESENT", "WORKING" -> GreenMain.copy(alpha = 0.2f)
                    "ENDED" -> Color.Red.copy(alpha = 0.1f)
                    else -> Color.Gray.copy(alpha = 0.1f)
                },
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = state.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (state) {
                        "APPLYING" -> GreenMain
                        "PRESENT", "WORKING" -> GreenMain
                        "ENDED" -> Color.Red
                        else -> GrayText
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
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
    dateFormatter: DateTimeFormatter,
    onClaimSalary: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
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
                    color = Color.Black,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (attendanceList.isEmpty()) {
                    Text(
                        text = "No attendance records found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attendanceList) { attendance ->
                            val dateStr = attendance["date"] as? String ?: "-"
                            val status = attendance["status"] as? String ?: "UNKNOWN"
                            val date = try {
                                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(dateFormatter)
                            } catch (e: Exception) {
                                dateStr
                            }
                            AttendanceItem(date = date, status = status)
                        }
                    }
                }

                if (isEnded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Salary Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Present Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$presentDays",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Late Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$lateDays",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Hours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "%.2f".format(totalHours),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Salary",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$${"%.2f".format(totalSalary)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GreenMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (!receiveSalary) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showRatingDialog = true },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenMain,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Claim Salary", fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GreenMain.copy(alpha = 0.1f),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Salary Claimed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GreenMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Close",
                        color = GreenMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Rate Employer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "$i star",
                            tint = if (i <= stars) Color(0xFFFFD700) else GrayText,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { stars = i }
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Black,
                            fontSize = 14.sp
                        ),
                        decorationBox = { innerTextField ->
                            if (comment.isEmpty()) {
                                Text(
                                    text = "Enter your feedback (optional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrayText,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Cancel",
                            color = GrayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (stars > 0) onSubmit(stars, comment) },
                        enabled = stars > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenMain,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Submit", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceItem(date: String, status: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (status) {
            "PRESENT" -> GreenMain.copy(alpha = 0.1f)
            "LATE" -> Color(0xFFFF9800).copy(alpha = 0.1f)
            "ABSENT" -> Color.Red.copy(alpha = 0.1f)
            else -> GrayText.copy(alpha = 0.1f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (status) {
                        "PRESENT" -> Icons.Default.CheckCircle
                        "LATE" -> Icons.Default.Warning
                        "ABSENT" -> Icons.Default.Cancel
                        else -> Icons.Default.Info
                    },
                    contentDescription = status,
                    tint = when (status) {
                        "PRESENT" -> GreenMain
                        "LATE" -> Color(0xFFFF9800)
                        "ABSENT" -> Color.Red
                        else -> GrayText
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
            Text(
                text = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = when (status) {
                    "PRESENT" -> GreenMain
                    "LATE" -> Color(0xFFFF9800)
                    "ABSENT" -> Color.Red
                    else -> GrayText
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}