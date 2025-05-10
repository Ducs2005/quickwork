package com.example.quickwork.ui.screens

import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.quickwork.ScanActivity
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

private val GreenMain = Color(0xFF4CAF50)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(WeekFields.ISO.firstDayOfWeek)) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }

    // Formatter for parsing and displaying dates
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    // Fetch user's working jobs
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                // Get jobList from user document
                val userDoc = firestore.collection("users").document(userId).get().await()
                val jobList = userDoc.get("jobList") as? List<String> ?: emptyList()

                // Fetch job details for jobs where user is WORKING
                val workingJobs = mutableListOf<Job>()
                for (jobId in jobList) {
                    try {
                        val employeeDoc = firestore.collection("jobs")
                            .document(jobId)
                            .collection("employees")
                            .document(userId)
                            .get()
                            .await()
                        val jobState = employeeDoc.getString("jobState")
                        if (jobState == "WORKING") {
                            val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                            if (jobDoc.exists()) {
                                workingJobs.add(
                                    Job(
                                        id = jobDoc.id,
                                        name = jobDoc.getString("name") ?: "",
                                        type = try {
                                            jobDoc.getString("type")?.let { enumValueOf<JobType>(it) } ?: JobType.PARTTIME
                                        } catch (e: IllegalArgumentException) {
                                            JobType.PARTTIME
                                        },
                                        employerId = jobDoc.getString("employerId") ?: "",
                                        detail = jobDoc.getString("detail") ?: "",
                                        imageUrl = jobDoc.getString("imageUrl") ?: "",
                                        salary = jobDoc.getLong("salary")?.toInt() ?: 0,
                                        insurance = jobDoc.getLong("insurance")?.toInt() ?: 0,
                                        dateUpload = jobDoc.getString("dateUpload") ?: "",
                                        workingHoursStart = jobDoc.getString("workingHoursStart") ?: "",
                                        workingHoursEnd = jobDoc.getString("workingHoursEnd") ?: "",
                                        dateStart = jobDoc.getString("dateStart") ?: "",
                                        dateEnd = jobDoc.getString("dateEnd") ?: "",
                                        employees = emptyList(),
                                        employeeRequired = jobDoc.getLong("employeeRequired")?.toInt() ?: 0,
                                        companyName = jobDoc.getString("companyName") ?: "Unknown",
                                        categoryIds = jobDoc.get("categoryIds") as? List<String> ?: emptyList(),
                                        attendanceCode = jobDoc.getString("attendanceCode")
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ScheduleScreen", "Error fetching job $jobId", e)
                    }
                }
                jobs = workingJobs
                isLoading = false
            } catch (e: Exception) {
                Log.e("ScheduleScreen", "Failed to load jobs", e)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Process QR scan result
    LaunchedEffect(scanResult, selectedJob) {
        if (scanResult != null && selectedJob != null) {
            if (scanResult == selectedJob!!.attendanceCode) {
                try {
                    val todayStr = LocalDate.now().format(dateFormatter)
                    val currentTime = LocalTime.now()
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val startTime = try {
                        LocalTime.parse(selectedJob!!.workingHoursStart, timeFormatter)
                    } catch (e: Exception) {
                        LocalTime.now()
                    }
                    val status = if (currentTime.isAfter(startTime)) "LATE" else "PRESENT"
                    val attendanceData = hashMapOf(
                        "date" to todayStr,
                        "status" to status
                    )
                    firestore.collection("jobs")
                        .document(selectedJob!!.id)
                        .collection("employees")
                        .document(userId!!)
                        .collection("attendance")
                        .document(todayStr)
                        .set(attendanceData)
                        .await()
                    scanFeedback = "Attendance marked as $status"
                    Log.d("ScheduleScreen", "Attendance updated for job ${selectedJob!!.id}: $status")
                } catch (e: Exception) {
                    scanFeedback = "Failed to mark attendance"
                    Log.e("ScheduleScreen", "Failed to update attendance", e)
                }
            } else {
                scanFeedback = "Invalid QR code"
                Log.w("ScheduleScreen", "Invalid QR code scanned: $scanResult")
            }
            scanResult = null
            selectedJob = null
        }
    }

    // Calculate week range
    val weekEnd = currentWeekStart.plusDays(6)
    val weekRangeText = "${currentWeekStart.format(displayFormatter)} - ${weekEnd.format(displayFormatter)}"

    Scaffold(
        topBar = { Header(navController) },

        bottomBar = { BottomNavigation(navController, currentScreen = "schedule") },

        ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            // Week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "Previous Week",
                        tint = GreenMain
                    )
                }
                Text(
                    text = weekRangeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Next Week",
                        tint = GreenMain
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenMain)
                }
            } else if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No jobs scheduled.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(7) { dayIndex ->
                        val currentDay = currentWeekStart.plusDays(dayIndex.toLong())
                        val dayJobs = jobs.filter { job ->
                            try {
                                val startDate = LocalDate.parse(job.dateStart, dateFormatter)
                                val endDate = LocalDate.parse(job.dateEnd, dateFormatter)
                                !currentDay.isBefore(startDate) && !currentDay.isAfter(endDate)
                            } catch (e: Exception) {
                                Log.w("ScheduleScreen", "Invalid date format for job ${job.id}", e)
                                false
                            }
                        }
                        DayScheduleItem(
                            day = currentDay,
                            jobs = dayJobs,
                            formatter = displayFormatter,
                            onTakeAttendance = { job ->
                                selectedJob = job
                            },
                            onScanResult = { result ->
                                scanResult = result
                            }
                        )
                    }
                }
            }
        }
    }

    if (scanFeedback != null) {
        FeedbackDialog(
            message = scanFeedback!!,
            onDismiss = { scanFeedback = null }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayScheduleItem(
    day: LocalDate,
    jobs: List<Job>,
    formatter: DateTimeFormatter,
    onTakeAttendance: (Job) -> Unit,
    onScanResult: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = day.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (jobs.isEmpty()) {
                Text(
                    text = "No jobs scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                jobs.forEach { job ->
                    JobItem(
                        job = job,
                        isToday = day == LocalDate.now(),
                        onTakeAttendance = { onTakeAttendance(job) },
                        onScanResult = onScanResult
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JobItem(
    job: Job,
    isToday: Boolean,
    onTakeAttendance: () -> Unit,
    onScanResult: (String?) -> Unit
) {
    val context = LocalContext.current

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentTime = LocalTime.now()
    val startTime = try {
        LocalTime.parse(job.workingHoursStart, timeFormatter)
    } catch (e: Exception) {
        LocalTime.now()
    }
    val isNearStart = isToday && currentTime.isAfter(startTime.minusMinutes(60)) && currentTime.isBefore(startTime)
    val hasStarted = isToday && !currentTime.isBefore(startTime)
    val showTakeAttendance = isToday && (isNearStart || hasStarted) && job.attendanceCode != null

    // Launcher for ScanActivity
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val qrResult = result.data?.getStringExtra("qr_result")
            if (qrResult != null) {
                onTakeAttendance() // Set selectedJob
                onScanResult(qrResult) // Set scanResult
            } else {
                Log.w("JobItem", "No QR code result received")
                onScanResult(null)
            }
        } else {
            Log.w("JobItem", "ScanActivity cancelled or failed")
            onScanResult(null)
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanLauncher.launch(android.content.Intent(context, ScanActivity::class.java))
        } else {
            Log.e("JobItem", "Camera permission denied")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasStarted -> GreenMain.copy(alpha = 0.1f) // Light green for started
                isNearStart -> GreenMain.copy(alpha = 0.05f) // Lighter green for near start
                else -> Color.White // Default
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                if (isNearStart || hasStarted) {
                    Text(
                        text = if (hasStarted) "Started" else "Starting Soon",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenMain,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Company: ${job.companyName}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Type: ${job.type.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Hours: ${job.workingHoursStart} - ${job.workingHoursEnd}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            if (showTakeAttendance) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Take Attendance", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    message: String,
    onDismiss: () -> Unit
) {
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.contains("Invalid") || message.contains("Failed")) Color.Red else GreenMain,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Close", fontSize = 12.sp)
                }
            }
        }
    }
}