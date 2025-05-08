package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.data.models.Employee
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.tasks.await

data class BottomNavItem(val name: String, val route: String, val icon: ImageVector)

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

    // Define bottom navigation items
    val navItems = listOf(
        BottomNavItem("Home", "homeScreen", Icons.Filled.Home),
        BottomNavItem("Jobs", "jobManageScreen", Icons.Filled.Work),
        BottomNavItem("Profile", "profileScreen", Icons.Filled.Person),
        BottomNavItem("Logout", "logout", Icons.Filled.ExitToApp)
    )

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
                                    Log.d("JobManageScreen", "Employee ID: $empId")
                                    Employee(id = empId)
                                }
                            }

                            Log.d("JobManageScreen", "Employees for job ${doc.id}: $employeeList")

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
                                employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0
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
            TopAppBar(
                title = { Text("Manage Your Jobs", fontWeight = FontWeight.Bold) },
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
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = if (navController.currentDestination?.route == item.route && item.route != "logout") Color.Yellow else Color.White
                            )
                        },
                        label = {
                            Text(
                                text = item.name,
                                color = if (navController.currentDestination?.route == item.route && item.route != "logout") Color.Yellow else Color.White
                            )
                        },
                        selected = navController.currentDestination?.route == item.route && item.route != "logout",
                        onClick = {
                            if (item.route == "logout") {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
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
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(jobs) { job ->
                    JobCard(
                        job = job,
                        onClick = { selectedJob = job }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (jobs.isEmpty()) {
                    item {
                        Text(
                            text = "No jobs found.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        if (selectedJob != null) {
            EmployeeManagementDialog(
                job = selectedJob!!,
                onDismiss = { selectedJob = null },
                onUpdateJob = { updatedJob ->
                    jobs = jobs.map { if (it.id == updatedJob.id) updatedJob else it }
                }
            )
        }
    }
}

@Composable
fun JobCard(job: Job, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = job.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
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
                text = "Salary: $${job.salary}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Insurance: $${job.insurance}",
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Employees: ${job.employees.size}/${job.employeeRequired}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (job.employees.size >= job.employeeRequired) Color.Red else Color.Black
            )
        }
    }
}

// Helper function to create a notification
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

@Composable
fun EmployeeManagementDialog(
    job: Job,
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
                    Employee(id = empId)
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (employees.isEmpty()) {
                    Text(
                        text = "No employees assigned",
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
                        items(employees) { employee ->
                            EmployeeItem(
                                employee = employee,
                                state = employeeStates[employee.id] ?: "APPLYING",
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
fun EmployeeItem(
    employee: Employee,
    state: String,
    onAccept: () -> Unit,
    onFire: () -> Unit
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
                    text = "Employee ID: ${employee.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = "State: $state",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (state) {
                        "WORKING" -> Color(0xFF4CAF50)
                        "APPLYING" -> Color(0xFFFF9800)
                        "DENIED" -> Color.Red
                        else -> Color.Gray
                    }
                )
            }
            if (state == "APPLYING") {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
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

@Preview(showBackground = true)
@Composable
fun JobManageScreenPreview() {
    JobManageScreen(navController = NavController(LocalContext.current))
}