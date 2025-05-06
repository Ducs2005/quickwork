package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.AttendanceStatus
import com.example.quickwork.data.models.Employee
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.DailyAttendance

import com.example.quickwork.data.models.JobState
import com.example.quickwork.data.models.JobType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(navController: NavHostController, jobId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    var job by remember { mutableStateOf<Job?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasApplied by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf("") }

    LaunchedEffect(jobId) {
        firestore.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val baseJob = Job(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        type = JobType.valueOf(document.getString("type") ?: "PARTTIME"),
                        employerId = document.getString("employerId") ?: "",
                        detail = document.getString("detail") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        salary = document.getLong("salary")?.toInt() ?: 0,
                        insurance = document.getLong("insurance")?.toInt() ?: 0,
                        dateUpload = document.getString("dateUpload") ?: "",
                        workingHoursStart = document.getString("workingHoursStart") ?: "",
                        workingHoursEnd = document.getString("workingHoursEnd") ?: "",
                        dateStart = document.getString("dateStart") ?: "",
                        dateEnd = document.getString("dateEnd") ?: "",
                        employees = emptyList(), // We'll load from subcollection
                        employeeRequired = document.getLong("employeeRequired")?.toInt() ?: 0
                    )

                    // Fetch company name from user document
                    firestore.collection("users")
                        .document(baseJob.employerId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            companyName = userDoc.getString("companyName") ?: "Unknown Company"
                        }

                    // Fetch employees from subcollection
                    firestore.collection("jobs")
                        .document(jobId)
                        .collection("employees")
                        .get()
                        .addOnSuccessListener { employeeDocs ->
                            val employeeList = employeeDocs.map { empDoc ->
                                val attendanceList = (empDoc["attendance"] as? List<Map<String, Any>>)?.map { att ->
                                    DailyAttendance(
                                        date = att["date"] as? String ?: "",
                                        status = AttendanceStatus.valueOf(att["status"] as? String ?: "ABSENT")
                                    )
                                } ?: emptyList()

                                Employee(
                                    id = empDoc.getString("id") ?: "",
                                    jobState = JobState.valueOf(empDoc.getString("jobState") ?: "APPLYING"),
                                    attendance = attendanceList
                                )
                            }

                            job = baseJob.copy(employees = employeeList)
                            hasApplied = employeeList.any { it.id == userId }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            Log.e("JobDetailScreen", "Failed to load employees", it)
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                Log.e("JobDetailScreen", "Failed to load job", it)
                isLoading = false
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun applyForJob() {
        if (userId != null && job != null) {
            val jobId = job!!.id
            val userRef = firestore.collection("users").document(userId)
            val jobRef = firestore.collection("jobs").document(jobId)
            val jobEmployeesRef = jobRef.collection("employees").document(userId)
            val attendanceRef = jobEmployeesRef.collection("attendance")

            // 1. Update the user document with jobId
            userRef.update("jobList", FieldValue.arrayUnion(jobId))

            // 2. Save basic employee data
            val employeeData = mapOf(
                "id" to userId,
                "jobState" to JobState.APPLYING.name
            )

            jobEmployeesRef.set(employeeData)
                .addOnSuccessListener {
                    // 3. Generate attendance data and use batch write
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val startDate = LocalDate.parse(job!!.dateStart, formatter)
                    val endDate = LocalDate.parse(job!!.dateEnd, formatter)

                    val batch = firestore.batch()
                    var date = startDate
                    while (!date.isAfter(endDate)) {
                        val dateStr = date.format(formatter)
                        val attendanceData = mapOf(
                            "date" to dateStr,
                            "status" to AttendanceStatus.ABSENT.name
                        )
                        Log.d("date", "a $dateStr")
                        val attendanceDoc = attendanceRef.document(dateStr)
                        batch.set(attendanceDoc, attendanceData)
                        date = date.plusDays(1)
                    }

                    // 4. Commit all attendance records
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("JobDetailScreen", "Successfully saved attendance records.")

                            // 5. Update local UI
                            job = job!!.copy(
                                employees = job!!.employees + Employee(
                                    id = userId,
                                    jobState = JobState.APPLYING,
                                    attendance = emptyList() // You can reload if needed
                                )
                            )
                            hasApplied = true
                        }
                        .addOnFailureListener { e ->
                            Log.e("JobDetailScreen", "Failed to save attendance records", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("JobDetailScreen", "Failed to apply for job", e)
                }
        }


    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },

    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center

            ) {
                CircularProgressIndicator()
            }
        } else if (job == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Job not found",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            val isFull = job!!.employees.size >= job!!.employeeRequired
            val isEmployer = userId == job!!.employerId

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                item {
                    // Job Image
                    if (job!!.imageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(job!!.imageUrl),
                            contentDescription = "Job Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Image",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = job!!.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )

                    Text(
                        text = "Type: ${job!!.type.name}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Employee Status
                    Text(
                        text = "Employees: ${job!!.employees.size}/${job!!.employeeRequired} applied",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isFull) Color.Red else Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Button
                    //if (!isEmployer && !hasApplied && !isFull) {
                    if ( !hasApplied && !isFull) { //test

                            Log.e("if result", " result")
                        Button(
                            onClick = { applyForJob() },
                            modifier = Modifier
                                .fillMaxWidth()

                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Apply for Job",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (hasApplied) {
                        Text(
                            text = "You have already applied",
                            color = Color.Green,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (isFull) {
                        Text(
                            text = "Job is full",
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }


                    // Salary and Insurance
                    Text(
                        text = "Salary: $${job!!.salary}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Insurance: $${job!!.insurance}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Working Hours
                    Text(
                        text = "Working Hours: ${job!!.workingHoursStart} - ${job!!.workingHoursEnd}",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Range
                    Text(
                        text = "Duration: ${job!!.dateStart} to ${job!!.dateEnd}",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Uploaded
                    Text(
                        text = "Uploaded: ${job!!.dateUpload}",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Company: $companyName",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Job Detail
                    Text(
                        text = "Description",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = job!!.detail.ifEmpty { "No description provided" },
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))


                }

            }
        }
    }
}