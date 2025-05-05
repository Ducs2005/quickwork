package com.example.quickwork.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.regex.Pattern
// Required imports at the top
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val cardBackgroundColor = Color(0xFFE8F5E9) // Light green for card

    var jobName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf(JobType.PARTTIME) }
    var jobDetail by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var insurance by remember { mutableStateOf("") }
    var workingHoursStart by remember { mutableStateOf("") }
    var workingHoursEnd by remember { mutableStateOf("") }
    var dateStart by remember { mutableStateOf("") }
    var dateEnd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // For JobType dropdown

    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Get current date for dateUpload
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Time format validation (HH:mm)
    val timePattern = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    // Date format validation (yyyy-MM-dd)
    val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                text = "Add New Job",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )

            // Card Background for Inputs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = cardBackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error Message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Input Fields
                    InputField(
                        label = "Job Title",
                        value = jobName,
                        onValueChange = { jobName = it }
                    )

                    // Job Type Dropdown
                    Box {
                        OutlinedTextField(
                            value = jobType.name,
                            onValueChange = {},
                            label = { Text("Job Type") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.ArrowDropUp else androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle dropdown"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            JobType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        jobType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    InputField(
                        label = "Job Details",
                        value = jobDetail,
                        onValueChange = { jobDetail = it },
                        singleLine = false,
                        modifier = Modifier.height(120.dp)
                    )
                    InputField(
                        label = "Salary (USD)",
                        value = salary,
                        onValueChange = { if (it.all { char -> char.isDigit() }) salary = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    InputField(
                        label = "Insurance (USD)",
                        value = insurance,
                        onValueChange = { if (it.all { char -> char.isDigit() }) insurance = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    val context = LocalContext.current

// Working Hours Start Picker
                    var showTimePickerStart by remember { mutableStateOf(false) }
                    if (showTimePickerStart) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                workingHoursStart = String.format("%02d:%02d", hour, minute)
                            },
                            8, 0, true
                        ).show()
                        showTimePickerStart = false
                    }
                    OutlinedTextField(
                        value = workingHoursStart,
                        onValueChange = {},
                        label = { Text("Working Hours Start") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePickerStart = true }
                    )

// Working Hours End Picker
                    var showTimePickerEnd by remember { mutableStateOf(false) }
                    if (showTimePickerEnd) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                workingHoursEnd = String.format("%02d:%02d", hour, minute)
                            },
                            17, 0, true
                        ).show()
                        showTimePickerEnd = false
                    }
                    OutlinedTextField(
                        value = workingHoursEnd,
                        onValueChange = {},
                        label = { Text("Working Hours End") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePickerEnd = true }
                    )

// Date Start Picker
                    var showDatePickerStart by remember { mutableStateOf(false) }
                    if (showDatePickerStart) {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dateStart = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                        showDatePickerStart = false
                    }
                    OutlinedTextField(
                        value = dateStart,
                        onValueChange = {},
                        label = { Text("Start Date") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerStart = true }
                    )

// Date End Picker
                    var showDatePickerEnd by remember { mutableStateOf(false) }
                    if (showDatePickerEnd) {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dateEnd = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                        showDatePickerEnd = false
                    }
                    OutlinedTextField(
                        value = dateEnd,
                        onValueChange = {},
                        label = { Text("End Date") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerEnd = true }
                    )


                    // Add Job Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (currentUser == null) {
                                    errorMessage = "You must be logged in to add a job"
                                    return@launch
                                }
                                if (jobName.isBlank() || jobDetail.isBlank() || salary.isBlank() || insurance.isBlank() ||
                                    workingHoursStart.isBlank() || workingHoursEnd.isBlank() || dateStart.isBlank() || dateEnd.isBlank()
                                ) {
                                    errorMessage = "Please fill in all fields"
                                    return@launch
                                }
                                val salaryInt = salary.toIntOrNull()
                                val insuranceInt = insurance.toIntOrNull()
                                if (salaryInt == null || insuranceInt == null) {
                                    errorMessage = "Salary and Insurance must be valid numbers"
                                    return@launch
                                }
                                if (!timePattern.matcher(workingHoursStart).matches() || !timePattern.matcher(workingHoursEnd).matches()) {
                                    errorMessage = "Working hours must be in HH:mm format (e.g., 08:00)"
                                    return@launch
                                }
                                if (!datePattern.matcher(dateStart).matches() || !datePattern.matcher(dateEnd).matches()) {
                                    errorMessage = "Dates must be in yyyy-MM-dd format (e.g., 2025-05-10)"
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                try {
                                    // Create Job object
                                    val job = Job(
                                        id = firestore.collection("jobs").document().id,
                                        name = jobName,
                                        type = jobType,
                                        employerId = currentUser.uid,
                                        detail = jobDetail,
                                        salary = salaryInt,
                                        insurance = insuranceInt,
                                        dateUpload = currentDate,
                                        workingHoursStart = workingHoursStart,
                                        workingHoursEnd = workingHoursEnd,
                                        dateStart = dateStart,
                                        dateEnd = dateEnd
                                    )
                                    // Save to Firestore
                                    firestore.collection("jobs")
                                        .document(job.id)
                                        .set(job)
                                        .await()
                                    // Navigate back
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to add job"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Add Job",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Cancel Button
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions
    )
}