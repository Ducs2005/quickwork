package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID

private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)

enum class JobCategory { MANAGING, INCOMING, ENDED }

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobManageScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }
    var selectedAttendanceJob by remember { mutableStateOf<Job?>(null) }
    var selectedCategory by remember { mutableStateOf(JobCategory.MANAGING) }

    // Categorize jobs
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now() // May 10, 2025
    val managingJobs by remember(jobs) {
        derivedStateOf {
            jobs.filter {
                try {
                    val startDate = LocalDate.parse(it.dateStart, formatter)
                    val endDate = LocalDate.parse(it.dateEnd, formatter)
                    !today.isBefore(startDate) && !today.isAfter(endDate)
                } catch (e: Exception) {
                    false // Exclude invalid dates
                }
            }
        }
    }
    val incomingJobs by remember(jobs) {
        derivedStateOf {
            jobs.filter {
                try {
                    val startDate = LocalDate.parse(it.dateStart, formatter)
                    today.isBefore(startDate)
                } catch (e: Exception) {
                    false // Exclude invalid dates
                }
            }
        }
    }
    val endedJobs by remember(jobs) {
        derivedStateOf {
            jobs.filter {
                try {
                    val endDate = LocalDate.parse(it.dateEnd, formatter)
                    today.isAfter(endDate)
                } catch (e: Exception) {
                    false // Exclude invalid dates
                }
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                firestore.collection("jobs")
                    .whereEqualTo("employerId", userId)
                    .get()
                    .await()
                    .let { querySnapshot ->
                        jobs = querySnapshot.documents.mapNotNull { doc ->
                            val employeeDocs = firestore.collection("jobs")
                                .document(doc.id)
                                .collection("employees")
                                .get()
                                .await()
                            val employeeList = employeeDocs.documents.mapNotNull { empDoc ->
                                val empId = empDoc.getString("id") ?: empDoc.id
                                if (empId.isBlank()) {
                                    Log.w("JobManageScreen", "Invalid employee document: ${empDoc.data}")
                                    null
                                } else {
                                    val attendanceDocs = firestore.collection("jobs")
                                        .document(doc.id)
                                        .collection("employees")
                                        .document(empId)
                                        .collection("attendance")
                                        .get()
                                        .await()
                                    val attendanceList = attendanceDocs.documents.mapNotNull { attDoc ->
                                        val date = attDoc.getString("date") ?: ""
                                        val status = attDoc.getString("status")?.let { AttendanceStatus.valueOf(it) }
                                        if (date.isNotBlank() && status != null) {
                                            DailyAttendance(date = date, status = status)
                                        } else {
                                            null
                                        }
                                    }
                                    Employee(id = empId, attendance = attendanceList)
                                }
                            }
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
                                employees = employeeList,
                                employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0,
                                companyName = doc.getString("companyName") ?: "Unknown",
                                categoryIds = doc.get("categoryIds") as? List<String> ?: emptyList(),
                                attendanceCode = doc.getString("attendanceCode")
                            )
                        }
                        isLoading = false
                    }
            } catch (e: Exception) {
                Log.e("JobManageScreen", "Failed to load jobs or employees", e)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            ReusableTopAppBar(
                title = "Manage Your Jobs",
                navController = navController,
                //showBackButton = true
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
        if (isLoading) {
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
                        isSelected = selectedCategory == JobCategory.MANAGING,
                        onClick = { selectedCategory = JobCategory.MANAGING }
                    )
                    CategoryHeader(
                        title = "Incoming",
                        isSelected = selectedCategory == JobCategory.INCOMING,
                        onClick = { selectedCategory = JobCategory.INCOMING }
                    )
                    CategoryHeader(
                        title = "Ended",
                        isSelected = selectedCategory == JobCategory.ENDED,
                        onClick = { selectedCategory = JobCategory.ENDED }
                    )
                }

                // Job List
                when (selectedCategory) {
                    JobCategory.MANAGING -> JobList(
                        jobs = managingJobs,
                        emptyMessage = "No managing jobs.",
                        onClick = { selectedJob = it },
                        onAttendanceClick = { selectedAttendanceJob = it }
                    )
                    JobCategory.INCOMING -> JobList(
                        jobs = incomingJobs,
                        emptyMessage = "No incoming jobs.",
                        onClick = { selectedJob = it },
                        onAttendanceClick = { selectedAttendanceJob = it }
                    )
                    JobCategory.ENDED -> JobList(
                        jobs = endedJobs,
                        emptyMessage = "No ended jobs.",
                        onClick = { selectedJob = it },
                        onAttendanceClick = { selectedAttendanceJob = it }
                    )
                }
            }
        }

        if (selectedJob != null) {
            EmployeeManagementDialog(
                job = selectedJob!!,
                navController = navController,
                onDismiss = { selectedJob = null },
                onUpdateJob = { updatedJob ->
                    jobs = jobs.map { if (it.id == updatedJob.id) updatedJob else it }
                }
            )
        }

        if (selectedAttendanceJob != null) {
            AttendanceDialog(
                job = selectedAttendanceJob!!,
                onDismiss = { selectedAttendanceJob = null },
                onUpdateJob = { updatedJob ->
                    jobs = jobs.map { if (it.id == updatedJob.id) updatedJob else it }
                }
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
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val startDate = try {
        LocalDate.parse(job.dateStart, formatter)
    } catch (e: Exception) {
        LocalDate.now().minusDays(1)
    }
    val endDate = try {
        LocalDate.parse(job.dateEnd, formatter)
    } catch (e: Exception) {
        LocalDate.now().plusDays(1)
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

private fun createNotification(
    firestore: FirebaseFirestore,
    employeeId: String,
    title: String,
    content: String,
    from: String
) {
    val notification = hashMapOf(
        "title" to title,
        "content" to content,
        "from" to from,
        "isReaded" to false,
        "timestamp" to System.currentTimeMillis()
    )

    firestore.collection("users")
        .document(employeeId)
        .collection("notifications")
        .add(notification)
        .addOnSuccessListener {
            Log.d("EmployeeManagementDialog", "Notification created for employee $employeeId")
        }
        .addOnFailureListener { e ->
            Log.e("EmployeeManagementDialog", "Failed to create notification for employee $employeeId", e)
        }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EmployeeManagementDialog(
    job: Job,
    navController: NavController,
    onDismiss: () -> Unit,
    onUpdateJob: (Job) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val employerId = currentUser?.uid ?: ""
    var employeeStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var employees by remember { mutableStateOf<List<Employee>>(job.employees) }

    LaunchedEffect(job.id) {
        try {
            val employeeDocs = firestore.collection("jobs")
                .document(job.id)
                .collection("employees")
                .get()
                .await()
            val updatedEmployees = employeeDocs.documents.mapNotNull { empDoc ->
                val empId = empDoc.getString("id") ?: empDoc.id
                if (empId.isBlank()) {
                    Log.w("EmployeeManagementDialog", "Invalid employee document: ${empDoc.data}")
                    null
                } else {
                    val attendanceDocs = firestore.collection("jobs")
                        .document(job.id)
                        .collection("employees")
                        .document(empId)
                        .collection("attendance")
                        .get()
                        .await()
                    val attendanceList = attendanceDocs.documents.mapNotNull { attDoc ->
                        val date = attDoc.getString("date") ?: ""
                        val status = attDoc.getString("status")?.let { AttendanceStatus.valueOf(it) }
                        if (date.isNotBlank() && status != null) {
                            DailyAttendance(date = date, status = status)
                        } else {
                            null
                        }
                    }
                    Employee(id = empId, attendance = attendanceList)
                }
            }
            val states = mutableMapOf<String, String>()
            employeeDocs.documents.forEach { empDoc ->
                val empId = empDoc.getString("id") ?: empDoc.id
                if (empId.isNotBlank()) {
                    states[empId] = empDoc.getString("jobState") ?: "APPLYING"
                }
            }
            employees = updatedEmployees
            employeeStates = states
            onUpdateJob(job.copy(employees = updatedEmployees))
        } catch (e: Exception) {
            Log.e("EmployeeManagementDialog", "Failed to load employees", e)
        }
    }

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
                if (employees.isEmpty()) {
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
                        items(employees) { employee ->
                            EmployeeItem(
                                employee = employee,
                                state = employeeStates[employee.id] ?: "APPLYING",
                                onClick = { navController.navigate("profile/${employee.id}") },
                                onAccept = {
                                    firestore.collection("jobs")
                                        .document(job.id)
                                        .collection("employees")
                                        .document(employee.id)
                                        .update("jobState", "WORKING")
                                        .addOnSuccessListener {
                                            employeeStates = employeeStates + (employee.id to "WORKING")
                                            createNotification(
                                                firestore = firestore,
                                                employeeId = employee.id,
                                                title = "Job Application Accepted",
                                                content = "You have been accepted for the job '${job.name}'.",
                                                from = job.companyName
                                            )
                                            Log.d("EmployeeManagementDialog", "Employee ${employee.id} accepted")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("EmployeeManagementDialog", "Failed to accept employee ${employee.id}", e)
                                        }
                                },
                                onFire = {
                                    firestore.collection("jobs")
                                        .document(job.id)
                                        .collection("employees")
                                        .document(employee.id)
                                        .update("jobState", "DENIED")
                                        .addOnSuccessListener {
                                            val updatedEmployees = employees.filter { it.id != employee.id }
                                            employees = updatedEmployees
                                            employeeStates = employeeStates + (employee.id to "DENIED")
                                            onUpdateJob(job.copy(employees = updatedEmployees))
                                            createNotification(
                                                firestore = firestore,
                                                employeeId = employee.id,
                                                title = "Job Termination Notice",
                                                content = "You have been removed from the job '${job.name}'.",
                                                from = employerId
                                            )
                                            Log.d("EmployeeManagementDialog", "Employee ${employee.id} fired")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("EmployeeManagementDialog", "Failed to fire employee ${employee.id}", e)
                                        }
                                }
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
    onDismiss: () -> Unit,
    onUpdateJob: (Job) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var employees by remember { mutableStateOf<List<Employee>>(job.employees) }
    var todayAttendance by remember { mutableStateOf<Map<String, AttendanceStatus>>(emptyMap()) }
    var employeeNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrCodeText by remember { mutableStateOf<String?>(null) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val todayStr = today.format(formatter)

    LaunchedEffect(job.id) {
        try {
            val updatedEmployees = mutableListOf<Employee>()
            val attendanceMap = mutableMapOf<String, AttendanceStatus>()
            val nameMap = mutableMapOf<String, String>()
            for (employee in job.employees) {
                val userDoc = firestore.collection("users")
                    .document(employee.id)
                    .get()
                    .await()
                val name = userDoc.getString("name") ?: "Unknown"
                nameMap[employee.id] = name

                val attendanceDocs = firestore.collection("jobs")
                    .document(job.id)
                    .collection("employees")
                    .document(employee.id)
                    .collection("attendance")
                    .get()
                    .await()
                val attendanceList = attendanceDocs.documents.mapNotNull { attDoc ->
                    val date = attDoc.getString("date") ?: ""
                    val status = attDoc.getString("status")?.let { AttendanceStatus.valueOf(it) }
                    if (date.isNotBlank() && status != null) {
                        DailyAttendance(date = date, status = status)
                    } else {
                        null
                    }
                }
                val todayAtt = attendanceList.find { it.date == todayStr }
                attendanceMap[employee.id] = todayAtt?.status ?: AttendanceStatus.ABSENT
                updatedEmployees.add(employee.copy(attendance = attendanceList))
            }
            employees = updatedEmployees
            todayAttendance = attendanceMap
            employeeNames = nameMap
            onUpdateJob(job.copy(employees = updatedEmployees))
        } catch (e: Exception) {
            Log.e("AttendanceDialog", "Failed to load attendance or names", e)
        }
    }

    fun generateQRCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.Black.hashCode() else Color.White.hashCode())
            }
        }
        return bitmap
    }

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
                    text = "Attendance for ${job.name} - $todayStr",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (employees.isEmpty()) {
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
                        items(employees) { employee ->
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
                        onClick = {
                            val newCode = UUID.randomUUID().toString()
                            firestore.collection("jobs")
                                .document(job.id)
                                .update("attendanceCode", newCode)
                                .addOnSuccessListener {
                                    qrCodeText = newCode
                                    qrCodeBitmap = generateQRCode(newCode)
                                    onUpdateJob(job.copy(attendanceCode = newCode))
                                    Log.d("AttendanceDialog", "Attendance code updated: $newCode")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AttendanceDialog", "Failed to update attendance code", e)
                                }
                        },
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

    if (qrCodeBitmap != null && qrCodeText != null) {
        QRCodeDialog(
            qrCodeBitmap = qrCodeBitmap!!,
            qrCodeText = qrCodeText!!,
            onDismiss = { qrCodeBitmap = null; qrCodeText = null }
        )
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