package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.tooling.preview.Preview

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobManageScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("jobs")
                .whereEqualTo("employerId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    jobs = querySnapshot.documents.map { doc ->
                        Job(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = JobType.valueOf(doc.getString("type") ?: "PARTTIME"),
                            employerId = doc.getString("employerId") ?: "",
                            detail = doc.getString("detail") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            salary = doc.getLong("salary")?.toInt() ?: 0,
                            insurance = doc.getLong("insurance")?.toInt() ?: 0,
                            dateUpload = doc.getString("dateUpload") ?: "",
                            workingHoursStart = doc.getString("workingHoursStart") ?: "",
                            workingHoursEnd = doc.getString("workingHoursEnd") ?: "",
                            dateStart = doc.getString("dateStart") ?: "",
                            dateEnd = doc.getString("dateEnd") ?: "",
                            employees = emptyList(),
                            employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0,
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("JobManageScreen", "Failed to load jobs", e)
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Your Jobs") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addJobScreen") },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Job"
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(jobs) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                // Navigate to job detail or edit screen if needed
                                // navController.navigate("jobDetail/${job.id}")
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Display Job Image if available
                            if (job.imageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(job.imageUrl),
                                    contentDescription = "Job Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = job.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = job.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = job.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Salary: \$${job.salary}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Insurance: \$${job.insurance}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Working Hours: ${job.workingHoursStart} - ${job.workingHoursEnd}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Date: ${job.dateStart} to ${job.dateEnd}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (jobs.isEmpty()) {
                    item {
                        Text("No jobs found.", modifier = Modifier.padding(top = 20.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JobManageScreenPreview() {
    JobManageScreen(navController = NavController(LocalContext.current))
}