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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
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
import java.util.Calendar
import java.util.regex.Pattern

// Data class for categories
data class Category(
    val id: String = "",
    val name: String = ""
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val cardBackgroundColor = Color(0xFFE8F5E9) // Light green for card

    var jobName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf(JobType.PARTTIME) }
    var jobDetail by remember { mutableStateOf("") }
    var jobBenefit by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var salary by remember { mutableStateOf("") }
    var employeeRequired by remember { mutableStateOf("") }
    var insurance by remember { mutableStateOf("") }
    var workingHoursStart by remember { mutableStateOf("") }
    var workingHoursEnd by remember { mutableStateOf("") }
    var dateStart by remember { mutableStateOf("") }
    var dateEnd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // For JobType dropdown
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }

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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // Fetch categories from Firestore
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
                                        jobType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Selection
                    Text(
                        text = "Select Categories:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    if (categories.isEmpty()) {
                        Text(
                            text = "Loading categories...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategoryIds.contains(category.id),
                                    onClick = {
                                        selectedCategoryIds = if (selectedCategoryIds.contains(category.id)) {
                                            selectedCategoryIds - category.id
                                        } else {
                                            selectedCategoryIds + category.id
                                        }
                                    },
                                    label = { Text(category.name) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
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
                        label = "Job Benefit",
                        value = jobBenefit,
                        onValueChange = { jobBenefit = it },
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
                    InputField(
                        label = "Number of Employees Required",
                        value = employeeRequired,
                        onValueChange = { if (it.all { char -> char.isDigit() }) employeeRequired = it },
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
                            .clickable { showTimePickerStart = true },
                        shape = RoundedCornerShape(12.dp)
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
                            .clickable { showTimePickerEnd = true },
                        shape = RoundedCornerShape(12.dp)
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
                            .clickable { showDatePickerStart = true },
                        shape = RoundedCornerShape(12.dp)
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
                            .clickable { showDatePickerEnd = true },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Image Selection
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text("Select Job Image")
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

                    // Add Job Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (currentUser == null) {
                                    errorMessage = "You must be logged in to add a job"
                                    return@launch
                                }
                                if (selectedImageUri == null) {
                                    errorMessage = "Please select an image"
                                    return@launch
                                }
                                if (selectedCategoryIds.isEmpty()) {
                                    errorMessage = "Please select at least one category"
                                    return@launch
                                }

                                val uploadedUrl = withContext(Dispatchers.IO) {
                                    uploadImageToCloudinary(context, selectedImageUri!!)
                                }

                                if (uploadedUrl == null) {
                                    errorMessage = "Failed to upload image"
                                    return@launch
                                }

                                imageUrl = uploadedUrl

                                if (jobName.isBlank() || jobDetail.isBlank() || salary.isBlank() || insurance.isBlank() ||
                                    workingHoursStart.isBlank() || workingHoursEnd.isBlank() || dateStart.isBlank() ||
                                    dateEnd.isBlank() || employeeRequired.isBlank()
                                ) {
                                    errorMessage = "Please fill in all fields"
                                    return@launch
                                }
                                val salaryInt = salary.toIntOrNull()
                                val insuranceInt = insurance.toIntOrNull()
                                val employeeRequiredInt = employeeRequired.toIntOrNull()
                                if (salaryInt == null || insuranceInt == null || employeeRequiredInt == null) {
                                    errorMessage = "Salary, Insurance, and Number of Employees must be valid numbers"
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
                                        dateEnd = dateEnd,
                                        employeeRequired = employeeRequiredInt,
                                        imageUrl = imageUrl!!,
                                        companyName = currentUser.displayName ?: "Unknown",
                                        categoryIds = selectedCategoryIds
                                    )
                                    // Save to Firestore
                                    firestore.collection("jobs")
                                        .document(job.id)
                                        .set(job)
                                        .await()
                                    // Navigate back
                                    Toast.makeText(context, "Job added successfully!", Toast.LENGTH_SHORT).show()
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