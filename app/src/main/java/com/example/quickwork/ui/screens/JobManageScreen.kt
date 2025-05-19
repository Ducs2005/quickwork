package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.*
import com.example.quickwork.ui.viewmodels.JobCategory
import com.example.quickwork.ui.viewmodels.JobManageViewModel
import com.google.firebase.auth.FirebaseAuth

private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobManageScreen(
    navController: NavController,
    viewModel: JobManageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val employerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            ReusableTopAppBar(
                title = "Manage Your Jobs",
                navController = navController
            )
        },
        bottomBar = {
            ReusableBottomNavBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addJobScreen") },
                modifier = Modifier.padding(16.dp),
                containerColor = GreenMain,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Job"
                )
            }
        },
        containerColor = GreenLight
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Category Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CategoryHeader(
                        title = "Managing",
                        isSelected = uiState.selectedCategory == JobCategory.MANAGING,
                        onClick = { viewModel.selectCategory(JobCategory.MANAGING) }
                    )
                    CategoryHeader(
                        title = "Incoming",
                        isSelected = uiState.selectedCategory == JobCategory.INCOMING,
                        onClick = { viewModel.selectCategory(JobCategory.INCOMING) }
                    )
                    CategoryHeader(
                        title = "Ended",
                        isSelected = uiState.selectedCategory == JobCategory.ENDED,
                        onClick = { viewModel.selectCategory(JobCategory.ENDED) }
                    )
                }

                // Job List
                when (uiState.selectedCategory) {
                    JobCategory.MANAGING -> JobList(
                        jobs = viewModel.managingJobs,
                        emptyMessage = "No managing jobs.",
                        onClick = { viewModel.selectJob(it) },
                        onAttendanceClick = { viewModel.selectAttendanceJob(it) }
                    )
                    JobCategory.INCOMING -> JobList(
                        jobs = viewModel.incomingJobs,
                        emptyMessage = "No incoming jobs.",
                        onClick = { viewModel.selectJob(it) },
                        onAttendanceClick = { viewModel.selectAttendanceJob(it) }
                    )
                    JobCategory.ENDED -> JobList(
                        jobs = viewModel.endedJobs,
                        emptyMessage = "No ended jobs.",
                        onClick = { viewModel.selectJob(it) },
                        onAttendanceClick = { viewModel.selectAttendanceJob(it) }
                    )
                }
            }
        }

        if (uiState.selectedJob != null) {
            EmployeeManagementDialog(
                job = uiState.selectedJob!!,
                navController = navController,
                employeeStates = uiState.employeeStates,
                onDismiss = { viewModel.selectJob(null) },
                onAccept = { employeeId, companyName ->
                    viewModel.acceptEmployee(uiState.selectedJob!!, employeeId, companyName)
                },
                onFire = { employeeId ->
                    viewModel.fireEmployee(uiState.selectedJob!!, employeeId, employerId)
                }
            )
        }

        if (uiState.selectedAttendanceJob != null) {
            AttendanceDialog(
                job = uiState.selectedAttendanceJob!!,
                employeeNames = uiState.employeeNames,
                todayAttendance = uiState.todayAttendance,
                onDismiss = { viewModel.selectAttendanceJob(null) },
                onGenerateQR = { viewModel.generateQRCode(it) }
            )
        }

        if (uiState.qrCodeBitmap != null && uiState.qrCodeText != null) {
            QRCodeDialog(
                qrCodeBitmap = uiState.qrCodeBitmap!!,
                qrCodeText = uiState.qrCodeText!!,
                onDismiss = { viewModel.clearQRCode() }
            )
        }
    }
}

@Composable
fun CategoryHeader(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) GreenMain else GrayText,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(
                if (isSelected) GreenMain.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JobList(
    jobs: List<Job>,
    emptyMessage: String,
    onClick: (Job) -> Unit,
    onAttendanceClick: (Job) -> Unit
) {
    if (jobs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                fontSize = 16.sp,
                color = GrayText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(jobs) { job ->
                JobCard(
                    job = job,
                    onClick = { onClick(job) },
                    onAttendanceClick = { onAttendanceClick(job) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JobCard(job: Job, onClick: () -> Unit, onAttendanceClick: () -> Unit) {
    val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = org.threeten.bp.LocalDate.now()
    val startDate = try {
        org.threeten.bp.LocalDate.parse(job.dateStart, formatter)
    } catch (e: Exception) {
        org.threeten.bp.LocalDate.now().minusDays(1)
    }
    val endDate = try {
        org.threeten.bp.LocalDate.parse(job.dateEnd, formatter)
    } catch (e: Exception) {
        org.threeten.bp.LocalDate.now().plusDays(1)
    }
    val isActive = !today.isBefore(startDate) && !today.isAfter(endDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) GreenLight else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (job.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(job.imageUrl),
                    contentDescription = "Job Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = GrayText,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = job.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = job.type.name,
                fontSize = 14.sp,
                color = GrayText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.detail,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Salary: $${job.salary}",
                fontSize = 14.sp
            )
            Text(
                text = "Insurance: $${job.insurance}",
                fontSize = 14.sp
            )
            Text(
                text = "Working Hours: ${job.workingHoursStart} - ${job.workingHoursEnd}",
                fontSize = 12.sp
            )
            Text(
                text = "Date: ${job.dateStart} to ${job.dateEnd}",
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Employees: ${job.employees.size}/${job.employeeRequired}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (job.employees.size >= job.employeeRequired) Color.Red else Color.Black
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAttendanceClick,
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EmployeeManagementDialog(
    job: Job,
    navController: NavController,
    employeeStates: Map<String, String>,
    onDismiss: () -> Unit,
    onAccept: (String, String) -> Unit,
    onFire: (String) -> Unit
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
                    .padding(16.dp)
            ) {
                Text(
                    text = "Manage Employees for ${job.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (job.employees.isEmpty()) {
                    Text(
                        text = "No employees assigned",
                        fontSize = 14.sp,
                        color = GrayText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(job.employees) { employee ->
                            EmployeeItem(
                                employee = employee,
                                state = employeeStates[employee.id] ?: "APPLYING",
                                onClick = { navController.navigate("profile/${employee.id}") },
                                onAccept = { onAccept(employee.id, job.companyName) },
                                onFire = { onFire(employee.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Close", fontSize = 14.sp)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceDialog(
    job: Job,
    employeeNames: Map<String, String>,
    todayAttendance: Map<String, AttendanceStatus>,
    onDismiss: () -> Unit,
    onGenerateQR: (Job) -> Unit
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
                    .padding(16.dp)
            ) {
                Text(
                    text = "Attendance for ${job.name} - ${org.threeten.bp.LocalDate.now().format(org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (job.employees.isEmpty()) {
                    Text(
                        text = "No employees assigned",
                        fontSize = 14.sp,
                        color = GrayText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(job.employees) { employee ->
                            AttendanceItem(
                                employee = employee,
                                employeeName = employeeNames[employee.id] ?: "Unknown",
                                currentStatus = todayAttendance[employee.id] ?: AttendanceStatus.ABSENT
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onGenerateQR(job) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenMain,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Create QR", fontSize = 12.sp)
                    }
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
}

@Composable
fun QRCodeDialog(
    qrCodeBitmap: Bitmap,
    qrCodeText: String,
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
                    text = "QR Code for Attendance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Code: $qrCodeText",
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
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

@Composable
fun EmployeeItem(
    employee: Employee,
    state: String,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onFire: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Employee ID: ${employee.id}",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "State: $state",
                    fontSize = 12.sp,
                    color = when (state) {
                        "WORKING" -> GreenMain
                        "APPLYING" -> Color(0xFFFF9800)
                        "DENIED" -> Color.Red
                        else -> GrayText
                    }
                )
            }
            if (state == "APPLYING") {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(text = "Accept", fontSize = 12.sp)
                }
            }
            if (state == "WORKING") {
                Button(
                    onClick = onFire,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Fire", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AttendanceItem(
    employee: Employee,
    employeeName: String,
    currentStatus: AttendanceStatus
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Employee: $employeeName",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "Attendance: $currentStatus",
                    fontSize = 12.sp,
                    color = when (currentStatus) {
                        AttendanceStatus.PRESENT -> GreenMain
                        AttendanceStatus.ABSENT -> Color.Red
                        else -> GrayText
                    }
                )
            }
        }
    }
}