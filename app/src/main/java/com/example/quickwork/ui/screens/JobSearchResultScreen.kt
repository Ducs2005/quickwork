package com.example.quickwork.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


// Enum for sorting options
enum class SortOption(val displayName: String) {
    SALARY_ASC("Salary: Low to High"),
    SALARY_DESC("Salary: High to Low"),
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    NAME_ASC("Name: A to Z")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSearchResultsScreen(navController: NavController, keyword: String) {
    val firestore = FirebaseFirestore.getInstance()
    var searchKeyword by remember { mutableStateOf(keyword) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedJobTypes by remember { mutableStateOf<List<JobType>>(JobType.values().toList()) }
    var startDate by remember { mutableStateOf<String?>(null) }
    var startTime by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.SALARY_DESC) }
    var isLoading by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Formatter for parsing dates and times
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
                    Log.w("JobSearchResultsScreen", "Error parsing category ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("JobSearchResultsScreen", "Failed to load categories", e)
        }
    }

    // Fetch and filter jobs
    LaunchedEffect(searchKeyword, selectedCategoryIds, selectedJobTypes, startDate, startTime, sortOption) {
        isLoading = true
        try {
            val querySnapshot = firestore.collection("jobs").get().await()
            val allJobs = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Job(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type")?.let { JobType.valueOf(it) } ?: JobType.PARTTIME,
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
                        companyName = doc.getString("companyName") ?: "Unknown",
                        categoryIds = doc.get("categoryIds") as? List<String> ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.w("JobSearchResultsScreen", "Error parsing job ${doc.id}", e)
                    null
                }
            }

            // Apply filters
            val filteredJobs = allJobs.filter { job ->
                val keywordLower = searchKeyword.lowercase()
                val nameMatch = job.name.lowercase().contains(keywordLower)
                val detailMatch = job.detail.lowercase().contains(keywordLower)
                val keywordPass = searchKeyword.isBlank() || nameMatch || detailMatch
                val categoryPass = selectedCategoryIds.isEmpty() || job.categoryIds.any { it in selectedCategoryIds }
                val typePass = job.type in selectedJobTypes
                val datePass = startDate?.let { filterDate ->
                    try {
                        val jobStartDate = LocalDate.parse(job.dateStart, dateFormatter)
                        val filterStartDate = LocalDate.parse(filterDate, dateFormatter)
                        !jobStartDate.isBefore(filterStartDate)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                val timePass = startTime?.let { filterTime ->
                    try {
                        val jobStartTime = LocalTime.parse(job.workingHoursStart, timeFormatter)
                        val filterStartTime = LocalTime.parse(filterTime, timeFormatter)
                        !jobStartTime.isBefore(filterStartTime)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                keywordPass && categoryPass && typePass && datePass && timePass
            }

            // Apply sorting
            jobs = when (sortOption) {
                SortOption.SALARY_ASC -> filteredJobs.sortedBy { it.salary }
                SortOption.SALARY_DESC -> filteredJobs.sortedByDescending { it.salary }
                SortOption.DATE_NEWEST -> filteredJobs.sortedByDescending { it.dateUpload }
                SortOption.DATE_OLDEST -> filteredJobs.sortedBy { it.dateUpload }
                SortOption.NAME_ASC -> filteredJobs.sortedBy { it.name.lowercase() }
            }
        } catch (e: Exception) {
            Log.e("JobSearchResultsScreen", "Failed to load jobs", e)
        } finally {
            isLoading = false
        }
    }

    // Filter summary text
    val filterCount = selectedCategoryIds.size +
            (if (selectedJobTypes.size < JobType.values().size) selectedJobTypes.size else 0) +
            (if (startDate != null) 1 else 0) +
            (if (startTime != null) 1 else 0)
    val filterSummary = when {
        filterCount > 0 -> "$filterCount filters applied"
        else -> "No filters applied"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Results") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                label = { Text("Search jobs") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Triggered by enter */ })
            )

            // Filter Button and Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    )
                ) {
                    Text("Filters")
                }
                Text(
                    text = filterSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Job Results
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No jobs found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs) { job ->
                        JobItem(
                            job = job,
                            categories = categories,
                            onClick = { navController.navigate("jobDetail/${job.id}") }
                        )
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Category Filter
                    Text(
                        text = "Categories",
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

                    // Job Type Filter
                    Text(
                        text = "Job Type",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(JobType.values()) { type ->
                            FilterChip(
                                selected = type in selectedJobTypes,
                                onClick = {
                                    selectedJobTypes = if (type in selectedJobTypes) {
                                        selectedJobTypes - type
                                    } else {
                                        selectedJobTypes + type
                                    }
                                },
                                label = { Text(type.name) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Start Date/Time Filter
                    val context = LocalContext.current
                    var showDatePicker by remember { mutableStateOf(false) }
                    var showTimePicker by remember { mutableStateOf(false) }

                    if (showDatePicker) {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                        showDatePicker = false
                    }

                    if (showTimePicker) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                startTime = String.format("%02d:%02d", hour, minute)
                            },
                            8, 0, true
                        ).show()
                        showTimePicker = false
                    }

                    Text(
                        text = "Start Date/Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = startDate ?: "",
                            onValueChange = {},
                            label = { Text("Start Date") },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (startDate != null) {
                                    IconButton(onClick = { startDate = null }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropUp,
                                            contentDescription = "Clear Date"
                                        )
                                    }
                                }
                            }
                        )
                        OutlinedTextField(
                            value = startTime ?: "",
                            onValueChange = {},
                            label = { Text("Start Time") },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (startTime != null) {
                                    IconButton(onClick = { startTime = null }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropUp,
                                            contentDescription = "Clear Time"
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Sort Option
                    var expandedSort by remember { mutableStateOf(false) }
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Box {
                        OutlinedTextField(
                            value = sortOption.displayName,
                            onValueChange = {},
                            label = { Text("Sort By") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedSort = !expandedSort }) {
                                    Icon(
                                        imageVector = if (expandedSort) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle sort dropdown"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedSort,
                            onDismissRequest = { expandedSort = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        sortOption = option
                                        expandedSort = false
                                    }
                                )
                            }
                        }
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                showFilterSheet = false
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }
}

@Composable
fun JobItem(job: Job, categories: List<Category>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = job.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Company: ${job.companyName}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Type: ${job.type.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Salary: $${job.salary}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            val categoryNames = job.categoryIds.mapNotNull { catId ->
                categories.find { it.id == catId }?.name
            }.joinToString(", ")
            Text(
                text = "Categories: ${if (categoryNames.isEmpty()) "None" else categoryNames}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}