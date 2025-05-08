package com.example.quickwork.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

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
                                        }
                                        ,
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
                                        employees = emptyList(), // Not needed for schedule
                                        employeeRequired = jobDoc.getLong("employeeRequired")?.toInt() ?: 0,
                                        companyName = jobDoc.getString("companyName") ?: "Unknown"
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

    // Calculate week range
    val weekEnd = currentWeekStart.plusDays(6)
    val weekRangeText = "${currentWeekStart.format(displayFormatter)} - ${weekEnd.format(displayFormatter)}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
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
                        tint = Color(0xFF1976D2)
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
                        tint = Color(0xFF1976D2)
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                    // Generate items for each day of the week
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
                            formatter = displayFormatter
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayScheduleItem(day: LocalDate, jobs: List<Job>, formatter: DateTimeFormatter) {
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
                    JobItem(job = job)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun JobItem(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = job.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
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
        }
    }
}