package com.example.quickwork.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.quickwork.data.models.*
import com.example.quickwork.viewModel.AddJobViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.regex.Pattern

private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)
private val YellowHighlight = Color(0xFFFFCA28)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJobScreen(navController: NavController, viewModel: AddJobViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val timePattern = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val steps = listOf(
        "Basic Info",
        "Details",
        "Compensation",
        "Working Hours",
        "Date Range",
        "Requirements",
        "Address",
        "Image",
        "Review"
    )

    // Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> viewModel.updateSelectedImageUri(uri) }
    )

    // Handle address result from MapPickerScreen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.get<String>("selectedAddress")?.let { addressJson ->
            viewModel.handleAddressResult(addressJson)
            navBackStackEntry?.savedStateHandle?.remove<String>("selectedAddress")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add New Job",
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
                                if (index <= uiState.currentStep) GreenMain else Color.White
                            )
                            .border(1.dp, GrayText, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            color = if (index <= uiState.currentStep) Color.White else GrayText
                        )
                    }
                }
            }

            // Step Title
            Text(
                text = steps[uiState.currentStep],
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
                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    when (uiState.currentStep) {
                        0 -> BasicInfoStep(
                            jobName = uiState.jobName,
                            onJobNameChange = { viewModel.updateJobName(it) },
                            jobType = uiState.jobType,
                            onJobTypeChange = { viewModel.updateJobType(it) },
                            categories = uiState.categories,
                            selectedCategoryIds = uiState.selectedCategoryIds,
                            onCategoryToggle = { viewModel.toggleCategoryId(it) }
                        )
                        1 -> DetailsStep(
                            jobDetail = uiState.jobDetail,
                            onJobDetailChange = { viewModel.updateJobDetail(it) },
                            jobBenefit = uiState.jobBenefit,
                            onJobBenefitChange = { viewModel.updateJobBenefit(it) }
                        )
                        2 -> CompensationStep(
                            salary = uiState.salary,
                            onSalaryChange = { viewModel.updateSalary(it) },
                            insurance = uiState.insurance,
                            onInsuranceChange = { viewModel.updateInsurance(it) },
                            employeeRequired = uiState.employeeRequired,
                            onEmployeeRequiredChange = { viewModel.updateEmployeeRequired(it) }
                        )
                        3 -> WorkingHoursStep(
                            workingHoursStart = uiState.workingHoursStart,
                            onWorkingHoursStartChange = { viewModel.updateWorkingHoursStart(it) },
                            workingHoursEnd = uiState.workingHoursEnd,
                            onWorkingHoursEndChange = { viewModel.updateWorkingHoursEnd(it) },
                            context = context
                        )
                        4 -> DateRangeStep(
                            dateStart = uiState.dateStart,
                            onDateStartChange = { viewModel.updateDateStart(it) },
                            dateEnd = uiState.dateEnd,
                            onDateEndChange = { viewModel.updateDateEnd(it) },
                            context = context
                        )
                        5 -> RequirementsStep(
                            educationRequired = uiState.educationRequired,
                            onEducationChange = { viewModel.updateEducationRequired(it) },
                            languageRequired = uiState.languageRequired,
                            onLanguageChange = { viewModel.updateLanguageRequired(it) }
                        )
                        6 -> AddressStep(
                            address = uiState.address,
                            onSelectAddress = { navController.navigate("mapPicker") }
                        )
                        7 -> ImageStep(
                            selectedImageUri = uiState.selectedImageUri,
                            onSelectImage = { imageLauncher.launch("image/*") },
                            context = context
                        )
                        8 -> ReviewStep(
                            jobName = uiState.jobName,
                            jobType = uiState.jobType,
                            jobDetail = uiState.jobDetail,
                            jobBenefit = uiState.jobBenefit,
                            salary = uiState.salary,
                            insurance = uiState.insurance,
                            employeeRequired = uiState.employeeRequired,
                            workingHoursStart = uiState.workingHoursStart,
                            workingHoursEnd = uiState.workingHoursEnd,
                            dateStart = uiState.dateStart,
                            dateEnd = uiState.dateEnd,
                            educationRequired = uiState.educationRequired,
                            languageRequired = uiState.languageRequired,
                            address = uiState.address,
                            selectedCategoryIds = uiState.selectedCategoryIds,
                            categories = uiState.categories,
                            selectedImageUri = uiState.selectedImageUri,
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
                        if (uiState.currentStep > 0) {
                            viewModel.updateCurrentStep(uiState.currentStep - 1)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.currentStep == 0) "Cancel" else "Previous",
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        val error = viewModel.validateStep(uiState.currentStep)
                        viewModel.setErrorMessage(error)

                        if (error == null) {
                            if (uiState.currentStep < steps.size - 1) {
                                viewModel.updateCurrentStep(uiState.currentStep + 1)
                            } else {
                                viewModel.submitJob(context) {
                                    Toast.makeText(context, "Job added successfully!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenMain,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading && uiState.currentStep == steps.size - 1) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (uiState.currentStep == steps.size - 1) "Submit" else "Next",
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
        val dialog = android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                onWorkingHoursStartChange(String.format("%02d:%02d", hour, minute))
            },
            8, 0, true
        )
        dialog.show()
        showTimePickerStart = false
    }
    if (showTimePickerEnd) {
        val dialog = android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                onWorkingHoursEndChange(String.format("%02d:%02d", hour, minute))
            },
            17, 0, true
        )
        dialog.show()
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
        val calendar = java.util.Calendar.getInstance()
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateStartChange(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        dialog.show()
        showDatePickerStart = false
    }
    if (showDatePickerEnd) {
        val calendar = java.util.Calendar.getInstance()
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateEndChange(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        dialog.show()
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
fun AddressStep(
    address: Address,
    onSelectAddress: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSelectAddress,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenMain,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Select Address from Map", fontSize = 14.sp)
        }
        if (address.address.isNotBlank()) {
            Text(
                text = "Selected: ${address.address}",
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
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
    address: Address,
    selectedCategoryIds: List<String>,
    categories: List<Category>,
    selectedImageUri: Uri?,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
        ReviewItem("Address", address.address.ifEmpty { "Not selected" })
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