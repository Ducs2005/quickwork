package com.example.quickwork.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.R
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.example.quickwork.data.models.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

private val GreenMain = Color(0xFF4CAF50)

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
fun JobSearchResultsScreen(
    navController: NavController,
    keyword: String,
    selectedJobType: JobType? = null
) {
    val firestore = FirebaseFirestore.getInstance()
    var searchKeyword by remember { mutableStateOf(keyword) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedJobTypes by remember { mutableStateOf<List<JobType>>(selectedJobType?.let { listOf(it) } ?: JobType.values().toList()) }
    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }
    var startTime by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.SALARY_DESC) }
    var isLoading by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val jobsPerPage = 4 // Changed from 20 to 4 jobs per page
    val totalPages = ceil(jobs.size.toDouble() / jobsPerPage).toInt()
    val displayedJobs = jobs.subList(
        fromIndex = (currentPage - 1) * jobsPerPage,
        toIndex = min(currentPage * jobsPerPage, jobs.size)
    )

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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

    LaunchedEffect(searchKeyword, selectedCategoryIds, selectedJobTypes, startDate, endDate, startTime, sortOption) {
        isLoading = true
        currentPage = 1 // Reset to page 1 on filter/sort change
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

            val filteredJobs = allJobs.filter { job ->
                val keywordLower = searchKeyword.lowercase()
                val nameMatch = job.name.lowercase().contains(keywordLower)
                val detailMatch = job.detail.lowercase().contains(keywordLower)
                val keywordPass = searchKeyword.isBlank() || nameMatch || detailMatch
                val categoryPass = selectedCategoryIds.isEmpty() || job.categoryIds.any { it in selectedCategoryIds }
                val typePass = selectedJobType?.let { job.type == it } ?: (job.type in selectedJobTypes)
                val startDatePass = startDate?.let { filterDate ->
                    try {
                        val jobStartDate = LocalDate.parse(job.dateStart, dateFormatter)
                        val filterStartDate = LocalDate.parse(filterDate, dateFormatter)
                        !jobStartDate.isBefore(filterStartDate)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                val endDatePass = endDate?.let { filterDate ->
                    try {
                        val jobEndDate = LocalDate.parse(job.dateEnd, dateFormatter)
                        val filterEndDate = LocalDate.parse(filterDate, dateFormatter)
                        !jobEndDate.isAfter(filterEndDate)
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
                keywordPass && categoryPass && typePass && startDatePass && endDatePass && timePass
            }

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

    val filterCount = selectedCategoryIds.size +
            (if (selectedJobTypes.size < JobType.values().size) selectedJobTypes.size else 0) +
            (if (startDate != null) 1 else 0) +
            (if (endDate != null) 1 else 0) +
            (if (startTime != null) 1 else 0)
    val filterSummary = when {
        filterCount > 0 -> "$filterCount filters applied"
        else -> "No filters applied"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Results", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
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
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.7f)
                )
            )
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                label = { Text("Search jobs") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = GreenMain)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenMain,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = GreenMain,
                    focusedLabelColor = GreenMain
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Triggered by enter */ })
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Filters", fontSize = 16.sp)
                }
                Text(
                    text = filterSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenMain)
                }
            } else if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No jobs found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayedJobs) { job ->
                        JobItem(
                            job = job,
                            categories = categories,
                            onClick = { navController.navigate("jobDetail/${job.id}") }
                        )
                    }
                }
                if (totalPages > 1) {
                    PaginationControl(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { newPage -> currentPage = newPage }
                    )
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                content = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GreenMain
                            )

                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
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
                                        val isSelected = selectedCategoryIds.contains(category.id)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedCategoryIds = if (isSelected) {
                                                    selectedCategoryIds - category.id
                                                } else {
                                                    selectedCategoryIds + category.id
                                                }
                                            },
                                            label = { Text(category.name, fontSize = 14.sp) },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GreenMain,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color.White,
                                                labelColor = Color.Gray
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = Color.Gray.copy(alpha = 0.3f),
                                                selected = false,
                                                enabled = false
                                            ),
                                            modifier = Modifier.animateContentSize()
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Job Type",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(JobType.values()) { type ->
                                    val isSelected = type in selectedJobTypes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedJobTypes = if (isSelected) {
                                                selectedJobTypes - type
                                            } else {
                                                selectedJobTypes + type
                                            }
                                        },
                                        label = { Text(type.name, fontSize = 14.sp) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GreenMain,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.White,
                                            labelColor = Color.Gray
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = Color.Gray.copy(alpha = 0.3f),
                                            selected = false,
                                            enabled = false
                                        ),
                                        modifier = Modifier.animateContentSize()
                                    )
                                }
                            }

                            val context = LocalContext.current
                            var showStartDatePicker by remember { mutableStateOf(false) }
                            var showEndDatePicker by remember { mutableStateOf(false) }
                            var showTimePicker by remember { mutableStateOf(false) }

                            if (showStartDatePicker) {
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
                                showStartDatePicker = false
                            }

                            if (showEndDatePicker) {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        endDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                                showEndDatePicker = false
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
                                text = "Date and Time",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = startDate ?: "",
                                    onValueChange = {},
                                    label = { Text("Start Date", fontSize = 12.sp) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { showStartDatePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenMain,
                                        unfocusedBorderColor = Color.Gray,
                                        cursorColor = GreenMain,
                                        focusedLabelColor = GreenMain
                                    ),
                                    trailingIcon = {
                                        if (startDate != null) {
                                            IconButton(onClick = { startDate = null }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropUp,
                                                    contentDescription = "Clear Start Date",
                                                    tint = GreenMain
                                                )
                                            }
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = endDate ?: "",
                                    onValueChange = {},
                                    label = { Text("End Date", fontSize = 12.sp) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { showEndDatePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenMain,
                                        unfocusedBorderColor = Color.Gray,
                                        cursorColor = GreenMain,
                                        focusedLabelColor = GreenMain
                                    ),
                                    trailingIcon = {
                                        if (endDate != null) {
                                            IconButton(onClick = { endDate = null }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropUp,
                                                    contentDescription = "Clear End Date",
                                                    tint = GreenMain
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = startTime ?: "",
                                onValueChange = {},
                                label = { Text("Start Time", fontSize = 12.sp) },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { showTimePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenMain,
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = GreenMain,
                                    focusedLabelColor = GreenMain
                                ),
                                trailingIcon = {
                                    if (startTime != null) {
                                        IconButton(onClick = { startTime = null }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropUp,
                                                contentDescription = "Clear Time",
                                                tint = GreenMain
                                            )
                                        }
                                    }
                                }
                            )

                            var expandedSort by remember { mutableStateOf(false) }
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Box {
                                OutlinedTextField(
                                    value = sortOption.displayName,
                                    onValueChange = {},
                                    label = { Text("Sort By", fontSize = 14.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenMain,
                                        unfocusedBorderColor = Color.Gray,
                                        cursorColor = GreenMain,
                                        focusedLabelColor = GreenMain
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { expandedSort = !expandedSort }) {
                                            Icon(
                                                imageVector = if (expandedSort) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Toggle sort dropdown",
                                                tint = GreenMain
                                            )
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = expandedSort,
                                    onDismissRequest = { expandedSort = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                ) {
                                    SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.displayName, fontSize = 14.sp) },
                                            onClick = {
                                                sortOption = option
                                                expandedSort = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (sortOption == option) GreenMain.copy(alpha = 0.1f)
                                                    else Color.Transparent
                                                )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        showFilterSheet = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenMain,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Apply Filters", fontSize = 16.sp)
                            }
                        }
                    }
                }
            )
        }
    }
}

// JobItem and PaginationControl remain unchanged from the original code
@Composable
fun JobItem(job: Job, categories: List<Category>, onClick: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "View job details"
            )
            .scale(scale)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (job.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(job.imageUrl),
                    contentDescription = "Job Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_job_image),
                    contentDescription = "Default Job Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = "Company: ${job.companyName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Type: ${job.type.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Salary: $${job.salary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreenMain,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                val categoryNames = job.categoryIds.mapNotNull { catId ->
                    categories.find { it.id == catId }?.name
                }.joinToString(", ")
                Text(
                    text = "Categories: ${if (categoryNames.isEmpty()) "None" else categoryNames}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PaginationControl(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        TextButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "Previous",
                color = if (currentPage > 1) GreenMain else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Page Numbers
        val maxPagesToShow = 5
        val pageRange = when {
            totalPages <= maxPagesToShow -> 1..totalPages
            currentPage <= 3 -> 1..maxPagesToShow
            currentPage > totalPages - 3 -> (totalPages - maxPagesToShow + 1)..totalPages
            else -> (currentPage - 2)..(currentPage + 2)
        }

        if (pageRange.first > 1) {
            Text(
                text = "1",
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onPageChange(1) },
                color = GreenMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (pageRange.first > 2) {
                Text(
                    text = "...",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        pageRange.forEach { page ->
            Text(
                text = page.toString(),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onPageChange(page) }
                    .background(
                        if (page == currentPage) GreenMain else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (page == currentPage) Color.White else GreenMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (pageRange.last < totalPages) {
            if (pageRange.last < totalPages - 1) {
                Text(
                    text = "...",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Text(
                text = totalPages.toString(),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onPageChange(totalPages) },
                color = GreenMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Next Button
        TextButton(
            onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "Next",
                color = if (currentPage < totalPages) GreenMain else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}