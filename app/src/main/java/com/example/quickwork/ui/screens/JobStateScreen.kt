package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.quickwork.data.models.AttendanceStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                // 1. Get user's jobList
                val userDoc = firestore.collection("users").document(userId).get().await()
                val jobIds = userDoc.get("jobList") as? List<String> ?: emptyList()

                // 2. Fetch all jobs and employee states
                val fetchedJobs = mutableListOf<Map<String, Any>>()
                val fetchedStates = mutableMapOf<String, String>()
                for (jobId in jobIds) {
                    // Fetch job
                    val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                    if (jobDoc.exists()) {
                        jobDoc.data?.let {
                            fetchedJobs.add(it)
                            // Fetch employee state from employees subcollection
                            val employeeDoc = firestore.collection("jobs")
                                .document(jobId)
                                .collection("employees")
                                .document(userId)
                                .get()
                                .await()
                            if (employeeDoc.exists()) {
                                val state = employeeDoc.getString("jobState") ?: "UNKNOWN"
                                fetchedStates[jobId] = state
                            } else {
                                fetchedStates[jobId] = "NOT_APPLIED"
                            }
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

    // Fetch attendance when a job is selected
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
            } catch (e: Exception) {
                Log.e("JobScreen", "Failed to load attendance", e)
                attendanceList = emptyList()
            }
        }
    }

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
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                items(jobList) { job ->
                    val jobId = job["id"] as? String ?: ""
                    JobCard(
                        job = job,
                        state = employeeStates[jobId] ?: "UNKNOWN",
                        onClick = { selectedJobId = jobId }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (jobList.isEmpty()) {
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
        if (selectedJobId != null) {
            AttendanceDialog(
                attendanceList = attendanceList,
                onDismiss = { selectedJobId = null }
            )
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
                    "WAITING" -> Color(0xFFFF9800) // Orange
                    "WORKING" -> Color.Green
                    "ENDED" -> Color.Red
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AttendanceDialog(attendanceList: List<Map<String, Any>>, onDismiss: () -> Unit) {
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