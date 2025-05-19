package com.example.quickwork.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.quickwork.data.models.EducationLevel
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.LanguageCertificate
import com.example.quickwork.viewModel.EmployeeWithRating
import com.example.quickwork.viewModel.HiringViewModel
import com.example.quickwork.viewModel.JobWithEmployeeCount

private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiringScreen(navController: NavController, viewModel: HiringViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var educationExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

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
        bottomBar = {
            ReusableBottomNavBar(navController = navController)
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
                        value = uiState.selectedEducation?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "All Education",
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
                                viewModel.updateEducationFilter(null)
                                educationExpanded = false
                            }
                        )
                        EducationLevel.values().forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.name.replace("_", " ").lowercase().capitalize()) },
                                onClick = {
                                    viewModel.updateEducationFilter(level)
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
                        value = uiState.selectedLanguage?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "All Certificates",
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
                                viewModel.updateLanguageFilter(null)
                                languageExpanded = false
                            }
                        )
                        LanguageCertificate.values().forEach { cert ->
                            DropdownMenuItem(
                                text = { Text(cert.name.replace("_", " ").lowercase().capitalize()) },
                                onClick = {
                                    viewModel.updateLanguageFilter(cert)
                                    educationExpanded = false
                                })
                        }
                    }
                }
            }
        }

        // Employee List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else if (uiState.filteredEmployees.isEmpty()) {
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
                items(uiState.filteredEmployees) { employee ->
                    EmployeeCard(
                        employee = employee,
                        onInviteClick = { viewModel.selectEmployee(employee.user) }
                    )
                }
            }
        }

        // Error Message
        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Job Selection Dialog
        uiState.selectedEmployee?.let { employee ->
            JobSelectionDialog(
                employee = employee,
                jobs = uiState.jobs,
                isLoading = uiState.isJobLoading,
                onDismiss = { viewModel.selectEmployee(null) },
                onInvite = { job -> viewModel.inviteEmployee(job) }
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
                        contentScale = ContentScale.Crop
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
                    text = "Education: ${employee.user.education?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "None"}",
                    fontSize = 14.sp,
                    color = GrayText
                )
                Text(
                    text = "Language: ${employee.user.languageCertificate?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "None"}",
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

@Composable
fun JobSelectionDialog(
    employee: com.example.quickwork.data.models.User,
    jobs: List<JobWithEmployeeCount>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onInvite: (Job) -> Unit
) {
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