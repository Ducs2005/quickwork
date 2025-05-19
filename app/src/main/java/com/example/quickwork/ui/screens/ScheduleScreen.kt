package com.example.quickwork.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.quickwork.ScanActivity
import com.example.quickwork.data.models.Job
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header
import com.example.quickwork.ui.viewmodels.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val GreenMain = Color(0xFF4CAF50)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentWeekStart by viewModel.currentWeekStart.collectAsState()
    val scanFeedback by viewModel.scanFeedback.collectAsState()

    // Formatter for parsing and displaying dates
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    // Calculate week range
    val weekEnd = currentWeekStart.plusDays(6)
    val weekRangeText = "${currentWeekStart.format(displayFormatter)} - ${weekEnd.format(displayFormatter)}"

    Scaffold(
        topBar = { Header(navController) },
        bottomBar = { BottomNavigation(navController, currentScreen = "schedule") }
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
                IconButton(onClick = { viewModel.setWeekStart(currentWeekStart.minusWeeks(1)) }) {
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
                IconButton(onClick = { viewModel.setWeekStart(currentWeekStart.plusWeeks(1)) }) {
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
                                false
                            }
                        }
                        DayScheduleItem(
                            day = currentDay,
                            jobs = dayJobs,
                            formatter = displayFormatter,
                            onTakeAttendance = { job, qrResult ->
                                viewModel.selectJobAndScan(job, qrResult)
                            }
                        )
                    }
                }
            }
        }
    }

    scanFeedback?.let {
        FeedbackDialog(
            message = it,
            onDismiss = { viewModel.clearFeedback() }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayScheduleItem(
    day: LocalDate,
    jobs: List<Job>,
    formatter: DateTimeFormatter,
    onTakeAttendance: (Job, String?) -> Unit
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
                        onTakeAttendance = { qrResult -> onTakeAttendance(job, qrResult) }
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
    onTakeAttendance: (String?) -> Unit
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
            onTakeAttendance(qrResult)
        } else {
            onTakeAttendance(null)
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanLauncher.launch(android.content.Intent(context, ScanActivity::class.java))
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