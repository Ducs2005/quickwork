package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.Job
import com.example.quickwork.ui.viewmodels.JobDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

private val GreenMain = Color(0xFF4CAF50) // Primary green color
private val GreenLight = Color(0xFFE8F5E9) // Light green for backgrounds
private val GrayText = Color(0xFF616161) // Gray for secondary text

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    navController: NavHostController,
    jobId: String,
    viewModel: JobDetailViewModel = viewModel(factory = JobDetailViewModelFactory(jobId))
) {
    val job by viewModel.job.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasApplied by viewModel.hasApplied.collectAsState()
    val employerName by viewModel.employerName.collectAsState()
    val categoryNames by viewModel.categoryNames.collectAsState()
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Job Details",
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
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
                    color = GrayText,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val isFull = job!!.employees.size >= job!!.employeeRequired
            val isEmployer = userId == job!!.employerId
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Job Image
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        if (job!!.imageUrl.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(job!!.imageUrl),
                                contentDescription = "Job Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Image Available",
                                    color = GrayText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Job Title and Type
                    Text(
                        text = job!!.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Type: ${job!!.type.name.replace("_", " ").lowercase().capitalize()}",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories
                    if (categoryNames.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryNames.forEach { category ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = GreenMain.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        color = GreenMain,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Categories: None",
                            fontSize = 14.sp,
                            color = GrayText
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Employer
                    Text(
                        text = "Employer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = employerName,
                        fontSize = 14.sp,
                        color = GreenMain,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { navController.navigate("profile/${job!!.employerId}") }
                            .padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Employee Status
                    Text(
                        text = "Employees: ${job!!.employees.size}/${job!!.employeeRequired} applied",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isFull) Color.Red else Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Button
                    if (!hasApplied && !isFull && !isEmployer) {
                        Button(
                            onClick = { viewModel.applyForJob() },
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
                                text = "Apply for Job",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (hasApplied) {
                        Text(
                            text = "You have already applied",
                            color = GreenMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (isFull) {
                        Text(
                            text = "Job is full",
                            color = Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (isEmployer) {
                        Text(
                            text = "You are the employer",
                            color = GrayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Job Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Salary
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Salary",
                                    tint = GreenMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Salary: $${job!!.salary}/hour",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }

                            // Working Hours
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Working Hours",
                                    tint = GreenMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Working Hours: ${job!!.workingHoursStart} - ${job!!.workingHoursEnd}",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Duration
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = "Duration",
                                    tint = GreenMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Duration: ${
                                        try {
                                            LocalDate.parse(job!!.dateStart, dateFormatter).format(displayFormatter)
                                        } catch (e: Exception) {
                                            job!!.dateStart
                                        }
                                    } to ${
                                        try {
                                            LocalDate.parse(job!!.dateEnd, dateFormatter).format(displayFormatter)
                                        } catch (e: Exception) {
                                            job!!.dateEnd
                                        }
                                    }",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Date Uploaded
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = "Uploaded",
                                    tint = GreenMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Uploaded: ${
                                        try {
                                            LocalDate.parse(job!!.dateUpload, dateFormatter).format(displayFormatter)
                                        } catch (e: Exception) {
                                            job!!.dateUpload
                                        }
                                    }",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                        text = "Description",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = job!!.detail.ifEmpty { "No description provided" },
                        fontSize = 14.sp,
                        color = GrayText,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (job!!.detail.length > 100) {
                        Text(
                            text = if (isDescriptionExpanded) "Show less" else "Read more",
                            fontSize = 14.sp,
                            color = GreenMain,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                .padding(vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}



class JobDetailViewModelFactory(private val jobId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobDetailViewModel::class.java)) {
            return JobDetailViewModel(jobId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}