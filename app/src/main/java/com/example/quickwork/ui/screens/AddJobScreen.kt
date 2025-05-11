package com.example.quickwork.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.R
import com.example.quickwork.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)
private val YellowHighlight = Color(0xFFFFCA28)

data class Category(
    val id: String = "",
    val name: String = ""
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var userRole by remember { mutableStateOf("employee") } // Default to employee

    // Form state
    var jobName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf(JobType.PARTTIME) }
    var jobDetail by remember { mutableStateOf("") }
    var jobBenefit by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var insurance by remember { mutableStateOf("") }
    var employeeRequired by remember { mutableStateOf("") }
    var workingHoursStart by remember { mutableStateOf("") }
    var workingHoursEnd by remember { mutableStateOf("") }
    var dateStart by remember { mutableStateOf("") }
    var dateEnd by remember { mutableStateOf("") }
    var educationRequired by remember { mutableStateOf<EducationLevel?>(null) }
    var languageRequired by remember { mutableStateOf<LanguageCertificate?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch user role
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                userRole = userDoc.getString("role") ?: "employee"
            } catch (e: Exception) {
                Log.e("AddJobScreen", "Failed to fetch user role", e)
                userRole = "employee" // Default to employee on error
            }
        }
    }

    // Validation patterns
    val timePattern = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Image picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // Fetch categories
    LaunchedEffect(Unit) {
        try {
            val querySnapshot = firestore.collection("category").get().await()
            categories = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("AddJobScreen", "Error parsing category ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AddJobScreen", "Failed to load categories", e)
        }
    }

    // Steps definition
    val steps = listOf(
        "Basic Info",
        "Details",
        "Compensation",
        "Working Hours",
        "Date Range",
        "Requirements",
        "Image",
        "Review"
    )

    Scaffold(
        topBar = {
            ReusableTopAppBar(
                title = "Add New Job",
                navController = navController,
                //showBackButton = true
            )
        },
        bottomBar = {
            ReusableBottomNavBar(navController = navController)

        },
        containerColor = GreenLight
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) GreenMain else Color.White
                            )
                            .border(1.dp, GrayText, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            color = if (index <= currentStep) Color.White else GrayText
                        )
                    }
                }
            }

            // Step Title
            Text(
                text = steps[currentStep],
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GreenMain,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Step Content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    when (currentStep) {
                        0 -> BasicInfoStep(
                            jobName = jobName,
                            onJobNameChange = { jobName = it },
                            jobType = jobType,
                            onJobTypeChange = { jobType = it },
                            categories = categories,
                            selectedCategoryIds = selectedCategoryIds,
                            onCategoryToggle = { id ->
                                selectedCategoryIds = if (selectedCategoryIds.contains(id)) {
                                    selectedCategoryIds - id
                                } else {
                                    selectedCategoryIds + id
                                }
                            }
                        )
                        1 -> DetailsStep(
                            jobDetail = jobDetail,
                            onJobDetailChange = { jobDetail = it },
                            jobBenefit = jobBenefit,
                            onJobBenefitChange = { jobBenefit = it }
                        )
                        2 -> CompensationStep(
                            salary = salary,
                            onSalaryChange = { salary = it },
                            insurance = insurance,
                            onInsuranceChange = { insurance = it },
                            employeeRequired = employeeRequired,
                            onEmployeeRequiredChange = { employeeRequired = it }
                        )
                        3 -> WorkingHoursStep(
                            workingHoursStart = workingHoursStart,
                            onWorkingHoursStartChange = { workingHoursStart = it },
                            workingHoursEnd = workingHoursEnd,
                            onWorkingHoursEndChange = { workingHoursEnd = it },
                            context = context
                        )
                        4 -> DateRangeStep(
                            dateStart = dateStart,
                            onDateStartChange = { dateStart = it },
                            dateEnd = dateEnd,
                            onDateEndChange = { dateEnd = it },
                            context = context
                        )
                        5 -> RequirementsStep(
                            educationRequired = educationRequired,
                            onEducationChange = { educationRequired = it },
                            languageRequired = languageRequired,
                            onLanguageChange = { languageRequired = it }
                        )
                        6 -> ImageStep(
                            selectedImageUri = selectedImageUri,
                            onSelectImage = { launcher.launch("image/*") },
                            context = context
                        )
                        7 -> ReviewStep(
                            jobName = jobName,
                            jobType = jobType,
                            jobDetail = jobDetail,
                            jobBenefit = jobBenefit,
                            salary = salary,
                            insurance = insurance,
                            employeeRequired = employeeRequired,
                            workingHoursStart = workingHoursStart,
                            workingHoursEnd = workingHoursEnd,
                            dateStart = dateStart,
                            dateEnd = dateEnd,
                            educationRequired = educationRequired,
                            languageRequired = languageRequired,
                            selectedCategoryIds = selectedCategoryIds,
                            categories = categories,
                            selectedImageUri = selectedImageUri,
                            context = context
                        )
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (currentStep == 0) "Cancel" else "Previous",
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        // Validate current step
                        errorMessage = when (currentStep) {
                            0 -> {
                                if (jobName.isBlank()) "Job Title is required"
                                else if (selectedCategoryIds.isEmpty()) "Select at least one category"
                                else null
                            }
                            1 -> {
                                if (jobDetail.isBlank()) "Job Details are required"
                                else if (jobBenefit.isBlank()) "Job Benefits are required"
                                else null
                            }
                            2 -> {
                                if (salary.isBlank() || salary.toIntOrNull() == null) "Valid Salary is required"
                                else if (insurance.isBlank() || insurance.toIntOrNull() == null) "Valid Insurance is required"
                                else if (employeeRequired.isBlank() || employeeRequired.toIntOrNull() == null) "Valid Number of Employees is required"
                                else null
                            }
                            3 -> {
                                if (!timePattern.matcher(workingHoursStart).matches()) "Start Hours must be in HH:mm format"
                                else if (!timePattern.matcher(workingHoursEnd).matches()) "End Hours must be in HH:mm format"
                                else null
                            }
                            4 -> {
                                if (!datePattern.matcher(dateStart).matches()) "Start Date must be in yyyy-MM-dd format"
                                else if (!datePattern.matcher(dateEnd).matches()) "End Date must be in yyyy-MM-dd format"
                                else null
                            }
                            5 -> {
                                if (educationRequired == null) "Education Level is required"
                                else if (languageRequired == null) "Language Certificate is required"
                                else null
                            }
                            6 -> {
                                if (selectedImageUri == null) "Job Image is required"
                                else null
                            }
                            7 -> null // Review step has no validation
                            else -> null
                        }

                        if (errorMessage == null) {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                // Submit job
                                coroutineScope.launch {
                                    if (currentUser == null) {
                                        errorMessage = "You must be logged in to add a job"
                                        return@launch
                                    }
                                    isLoading = true
                                    try {
                                        val uploadedUrl = withContext(Dispatchers.IO) {
                                            uploadImageToCloudinary(context, selectedImageUri!!)
                                        }
                                        if (uploadedUrl == null) {
                                            errorMessage = "Failed to upload image"
                                            isLoading = false
                                            return@launch
                                        }
                                        imageUrl = uploadedUrl

                                        val job = Job(
                                            id = firestore.collection("jobs").document().id,
                                            name = jobName,
                                            type = jobType,
                                            employerId = currentUser.uid,
                                            detail = jobDetail,
                                            salary = salary.toInt(),
                                            insurance = insurance.toInt(),
                                            dateUpload = currentDate,
                                            workingHoursStart = workingHoursStart,
                                            workingHoursEnd = workingHoursEnd,
                                            dateStart = dateStart,
                                            dateEnd = dateEnd,
                                            employeeRequired = employeeRequired.toInt(),
                                            imageUrl = imageUrl!!,
                                            companyName = currentUser.displayName ?: "Unknown",
                                            categoryIds = selectedCategoryIds,
                                            educationRequired = educationRequired,
                                            languageRequired = languageRequired
                                        )
                                        firestore.collection("jobs")
                                            .document(job.id)
                                            .set(job)
                                            .await()
                                        Toast.makeText(context, "Job added successfully!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to add job"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading && currentStep == steps.size - 1) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (currentStep == steps.size - 1) "Submit" else "Next",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigation(navController: NavController, currentScreen: String) {
    val items = listOf(
        "jobManage" to Icons.Default.Home,
        "addJobScreen" to Icons.Default.Add,
        "chatRoom" to Icons.Default.Message,
        "hiring" to Icons.Default.Work,
        "setting" to Icons.Default.Settings
    )

    NavigationBar(
        containerColor = GreenMain,
        contentColor = Color.White
    ) {
        items.forEach { (route, icon) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = route,
                        tint = if (currentScreen == route) YellowHighlight else Color.White
                    )
                },
                selected = currentScreen == route,
                onClick = {
                    if (currentScreen != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = YellowHighlight,
                    unselectedIconColor = Color.White,
                    indicatorColor = GreenMain
                )
            )
        }
    }
}

@Composable
fun BasicInfoStep(
    jobName: String,
    onJobNameChange: (String) -> Unit,
    jobType: JobType,
    onJobTypeChange: (JobType) -> Unit,
    categories: List<Category>,
    selectedCategoryIds: List<String>,
    onCategoryToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = jobName,
            onValueChange = onJobNameChange,
            label = { Text("Job Title") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

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
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
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
                            onJobTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Select Categories",
            fontSize = 14.sp,
            color = GrayText
        )
        if (categories.isEmpty()) {
            Text(
                text = "Loading categories...",
                fontSize = 12.sp,
                color = GrayText
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryIds.contains(category.id),
                        onClick = { onCategoryToggle(category.id) },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenMain,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = GrayText
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsStep(
    jobDetail: String,
    onJobDetailChange: (String) -> Unit,
    jobBenefit: String,
    onJobBenefitChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = jobDetail,
            onValueChange = onJobDetailChange,
            label = { Text("Job Details") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            singleLine = false
        )
        OutlinedTextField(
            value = jobBenefit,
            onValueChange = onJobBenefitChange,
            label = { Text("Job Benefits") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            singleLine = false
        )
    }
}

@Composable
fun CompensationStep(
    salary: String,
    onSalaryChange: (String) -> Unit,
    insurance: String,
    onInsuranceChange: (String) -> Unit,
    employeeRequired: String,
    onEmployeeRequiredChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = salary,
            onValueChange = { if (it.all { char -> char.isDigit() }) onSalaryChange(it) },
            label = { Text("Salary (USD)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = insurance,
            onValueChange = { if (it.all { char -> char.isDigit() }) onInsuranceChange(it) },
            label = { Text("Insurance (USD)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = employeeRequired,
            onValueChange = { if (it.all { char -> char.isDigit() }) onEmployeeRequiredChange(it) },
            label = { Text("Number of Employees Required") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WorkingHoursStep(
    workingHoursStart: String,
    onWorkingHoursStartChange: (String) -> Unit,
    workingHoursEnd: String,
    onWorkingHoursEndChange: (String) -> Unit,
    context: Context
) {
    var showTimePickerStart by remember { mutableStateOf(false) }
    var showTimePickerEnd by remember { mutableStateOf(false) }

    if (showTimePickerStart) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onWorkingHoursStartChange(String.format("%02d:%02d", hour, minute))
            },
            8, 0, true
        ).show()
        showTimePickerStart = false
    }
    if (showTimePickerEnd) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onWorkingHoursEndChange(String.format("%02d:%02d", hour, minute))
            },
            17, 0, true
        ).show()
        showTimePickerEnd = false
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = workingHoursStart,
            onValueChange = {},
            label = { Text("Working Hours Start") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePickerStart = true },
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = workingHoursEnd,
            onValueChange = {},
            label = { Text("Working Hours End") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePickerEnd = true },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangeStep(
    dateStart: String,
    onDateStartChange: (String) -> Unit,
    dateEnd: String,
    onDateEndChange: (String) -> Unit,
    context: Context
) {
    var showDatePickerStart by remember { mutableStateOf(false) }
    var showDatePickerEnd by remember { mutableStateOf(false) }

    if (showDatePickerStart) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateStartChange(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
        showDatePickerStart = false
    }
    if (showDatePickerEnd) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateEndChange(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
        showDatePickerEnd = false
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = dateStart,
            onValueChange = {},
            label = { Text("Start Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerStart = true },
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = dateEnd,
            onValueChange = {},
            label = { Text("End Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerEnd = true },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun RequirementsStep(
    educationRequired: EducationLevel?,
    onEducationChange: (EducationLevel?) -> Unit,
    languageRequired: LanguageCertificate?,
    onLanguageChange: (LanguageCertificate?) -> Unit
) {
    var educationExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            OutlinedTextField(
                value = educationRequired?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "Select Education",
                onValueChange = {},
                label = { Text("Education Required") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { educationExpanded = !educationExpanded }) {
                        Icon(
                            imageVector = if (educationExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle dropdown"
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = educationExpanded,
                onDismissRequest = { educationExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onEducationChange(null)
                        educationExpanded = false
                    }
                )
                EducationLevel.values().forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.name.replace("_", " ").lowercase().capitalize()) },
                        onClick = {
                            onEducationChange(level)
                            educationExpanded = false
                        }
                    )
                }
            }
        }

        Box {
            OutlinedTextField(
                value = languageRequired?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "Select Language",
                onValueChange = {},
                label = { Text("Language Certificate Required") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { languageExpanded = !languageExpanded }) {
                        Icon(
                            imageVector = if (languageExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle dropdown"
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onLanguageChange(null)
                        languageExpanded = false
                    }
                )
                LanguageCertificate.values().forEach { cert ->
                    DropdownMenuItem(
                        text = { Text(cert.name.replace("_", " ").lowercase().capitalize()) },
                        onClick = {
                            onLanguageChange(cert)
                            languageExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageStep(
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    context: Context
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSelectImage,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenMain,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Select Job Image", fontSize = 14.sp)
        }
        selectedImageUri?.let { uri ->
            Image(
                bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ReviewStep(
    jobName: String,
    jobType: JobType,
    jobDetail: String,
    jobBenefit: String,
    salary: String,
    insurance: String,
    employeeRequired: String,
    workingHoursStart: String,
    workingHoursEnd: String,
    dateStart: String,
    dateEnd: String,
    educationRequired: EducationLevel?,
    languageRequired: LanguageCertificate?,
    selectedCategoryIds: List<String>,
    categories: List<Category>,
    selectedImageUri: Uri?,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Review Your Job",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = GreenMain
        )
        ReviewItem("Job Title", jobName)
        ReviewItem("Job Type", jobType.name)
        ReviewItem("Categories", selectedCategoryIds.map { id ->
            categories.find { it.id == id }?.name ?: id
        }.joinToString(", "))
        ReviewItem("Job Details", jobDetail)
        ReviewItem("Job Benefits", jobBenefit)
        ReviewItem("Salary", "$$salary")
        ReviewItem("Insurance", "$$insurance")
        ReviewItem("Employees Required", employeeRequired)
        ReviewItem("Working Hours", "$workingHoursStart - $workingHoursEnd")
        ReviewItem("Date Range", "$dateStart to $dateEnd")
        ReviewItem("Education Required", educationRequired?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "None")
        ReviewItem("Language Required", languageRequired?.name?.replace("_", " ")?.lowercase()?.capitalize() ?: "None")
        selectedImageUri?.let { uri ->
            Image(
                bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = GrayText
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

suspend fun uploadImageToCloudinary(context: Context, imageUri: Uri): String? {
    val cloudName = "dytggtwgy"
    val uploadPreset = "quickworks"
    val TAG = "CloudinaryUpload"

    return try {
        val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
        val boundary = "Boundary-${System.currentTimeMillis()}"
        val conn = url.openConnection() as HttpURLConnection
        conn.doOutput = true
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val outputStream = DataOutputStream(conn.outputStream)
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val fileBytes = inputStream!!.readBytes()
        inputStream.close()

        Log.d(TAG, "Writing upload_preset...")
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
        outputStream.writeBytes("$uploadPreset\r\n")

        Log.d(TAG, "Writing file data...")
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n")
        outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")
        outputStream.write(fileBytes)
        outputStream.writeBytes("\r\n--$boundary--\r\n")
        outputStream.flush()
        outputStream.close()

        val responseCode = conn.responseCode
        Log.d(TAG, "Response Code: $responseCode")

        if (responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Upload success. Response: $response")
            val imageUrl = Regex("\"url\":\"(.*?)\"").find(response)?.groupValues?.get(1)?.replace("\\/", "/")
            return imageUrl?.replace("http://", "https://")
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e(TAG, "Upload failed. Error response: $errorResponse")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception during upload: ${e.message}", e)
        null
    }
}