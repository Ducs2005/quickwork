package com.example.quickwork.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.quickwork.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Composable
fun RegisterScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val cardBackgroundColor = Color(0xFFE8F5E9) // Light green for card
    val buttonColor = Color(0xFF81C784) // Green for buttons

    var currentStep by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var userType by remember { mutableStateOf(UserType.EMPLOYEE) }
    var address by remember { mutableStateOf(Address()) } // Address object
    var educationLevel by remember { mutableStateOf(EducationLevel.NONE) } // Enum
    var languageCertificate by remember { mutableStateOf(LanguageCertificate.NONE) } // Enum
    var companyName by remember { mutableStateOf("") } // For Employer
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Handle address result from MapPickerScreen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.get<String>("selectedAddress")?.let { addressJson ->
            address = Json.decodeFromString<Address>(addressJson) // No need for .serializer()
            navBackStackEntry?.savedStateHandle?.remove<String>("selectedAddress")
        }
    }

    // Steps definition
    val steps = listOf(
        "Personal Info",
        "Credentials & Type",
        "Additional Details",
        "Review & Submit"
    )

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            color = cardBackgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                                .background(if (index <= currentStep) buttonColor else Color.White)
                                .border(1.dp, Color.Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                color = if (index <= currentStep) Color.White else Color.Gray
                            )
                        }
                    }
                }

                // Step Title
                Text(
                    text = steps[currentStep],
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonColor,
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
                            0 -> { // Personal Info: 3 fields
                                InputField(
                                    label = "Full Name",
                                    value = name,
                                    onValueChange = { name = it }
                                )
                                InputField(
                                    label = "Email",
                                    value = email,
                                    onValueChange = { email = it }
                                )
                                InputField(
                                    label = "Phone Number",
                                    value = phone,
                                    onValueChange = { phone = it }
                                )
                            }
                            1 -> { // Credentials & Type: 3 fields
                                PasswordField(
                                    label = "Password",
                                    password = password,
                                    onPasswordChange = { password = it },
                                    passwordVisible = passwordVisible,
                                    onVisibilityChange = { passwordVisible = !passwordVisible }
                                )
                                PasswordField(
                                    label = "Confirm Password",
                                    password = confirmPassword,
                                    onPasswordChange = { confirmPassword = it },
                                    passwordVisible = confirmPasswordVisible,
                                    onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
                                )
                                Text(
                                    text = "Select Account Type:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    UserType.values().forEach { type ->
                                        FilterChip(
                                            selected = userType == type,
                                            onClick = { userType = type },
                                            label = { Text(type.name) },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = buttonColor,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                            2 -> { // Additional Details: 2–3 fields
                                Button(
                                    onClick = {
                                        navController.navigate("mapPicker")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                                ) {
                                    Text("Select Address from Map", color = Color.White)
                                }
                                if (address.address.isNotEmpty()) {
                                    Text(
                                        text = "Selected: ${address.address}",
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Coordinates: (${address.latitude}, ${address.longitude})",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (userType == UserType.EMPLOYEE) {
                                    EducationLevelDropdown(
                                        selectedLevel = educationLevel,
                                        onLevelChange = { educationLevel = it }
                                    )
                                    LanguageCertificateDropdown(
                                        selectedCertificate = languageCertificate,
                                        onCertificateChange = { languageCertificate = it }
                                    )
                                } else {
                                    InputField(
                                        label = "Company Name",
                                        value = companyName,
                                        onValueChange = { companyName = it }
                                    )
                                }
                            }
                            3 -> { // Review & Submit
                                Text(
                                    text = "By continuing, you agree to our\nTerms of Use and Privacy Policy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ReviewStep(
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    userType = userType,
                                    address = address,
                                    educationLevel = educationLevel,
                                    languageCertificate = languageCertificate,
                                    companyName = companyName
                                )
                            }
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
                                errorMessage = null
                            } else {
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (currentStep == 0) "Cancel" else "Previous",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            // Validate current step
                            errorMessage = when (currentStep) {
                                0 -> {
                                    when {
                                        name.isBlank() -> "Full Name is required"
                                        email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Valid Email is required"
                                        phone.isBlank() -> "Phone Number is required"
                                        else -> null
                                    }
                                }
                                1 -> {
                                    when {
                                        password.isBlank() -> "Password is required"
                                        password != confirmPassword -> "Passwords do not match"
                                        else -> null
                                    }
                                }
                                2 -> {
                                    when {
                                        address.address.isEmpty() -> "Address is required"
                                        userType == UserType.EMPLOYEE && educationLevel == EducationLevel.NONE -> "Education Level is required"
                                        userType == UserType.EMPLOYER && companyName.isBlank() -> "Company Name is required"
                                        else -> null
                                    }
                                }
                                3 -> null // Review step
                                else -> null
                            }

                            if (errorMessage == null) {
                                if (currentStep < steps.size - 1) {
                                    currentStep++
                                } else {
                                    // Submit
                                    coroutineScope.launch {
                                        isLoading = true
                                        try {
                                            // Create user with Firebase Auth
                                            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                            val firebaseUser = authResult.user
                                            if (firebaseUser != null) {
                                                // Update Firebase user profile
                                                val profileUpdates = UserProfileChangeRequest.Builder()
                                                    .setDisplayName(name)
                                                    .build()
                                                firebaseUser.updateProfile(profileUpdates).await()

                                                // Create User object
                                                val user = User(
                                                    uid = firebaseUser.uid,
                                                    name = name,
                                                    email = email,
                                                    phone = phone,
                                                    userType = userType,
                                                    address = address,
                                                    education = if (userType == UserType.EMPLOYEE) educationLevel else EducationLevel.NONE,
                                                    languageCertificate = if (userType == UserType.EMPLOYEE) languageCertificate else LanguageCertificate.NONE,
                                                    companyName = if (userType == UserType.EMPLOYER) companyName else ""
                                                )

                                                // Save to Firestore
                                                firestore.collection("users")
                                                    .document(firebaseUser.uid)
                                                    .set(user)
                                                    .await()

                                                // Navigate
                                                val destination = if (userType == UserType.EMPLOYEE) "jobList" else "jobManage"
                                                navController.navigate(destination) {
                                                    popUpTo(navController.graph.startDestinationId)
                                                    launchSingleTop = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Registration failed"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
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
                                text = if (currentStep == steps.size - 1) "Sign Up" else "Next",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Navigate to Sign In
                if (currentStep == 0) {
                    TextButton(
                        onClick = { navController.navigate("login") },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Already have an account? Sign In",
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
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun PasswordField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}

@Composable
fun EducationLevelDropdown(
    selectedLevel: EducationLevel,
    onLevelChange: (EducationLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedLevel.name.replace("_", " ").lowercase().capitalize(),
            onValueChange = {},
            label = { Text("Education Level") },
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
            EducationLevel.values().forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.name.replace("_", " ").lowercase().capitalize()) },
                    onClick = {
                        onLevelChange(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageCertificateDropdown(
    selectedCertificate: LanguageCertificate,
    onCertificateChange: (LanguageCertificate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedCertificate.name.replace("_", " ").lowercase().capitalize(),
            onValueChange = {},
            label = { Text("Language Certificate") },
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
            LanguageCertificate.values().forEach { cert ->
                DropdownMenuItem(
                    text = { Text(cert.name.replace("_", " ").lowercase().capitalize()) },
                    onClick = {
                        onCertificateChange(cert)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReviewStep(
    name: String,
    email: String,
    phone: String,
    userType: UserType,
    address: Address,
    educationLevel: EducationLevel,
    languageCertificate: LanguageCertificate,
    companyName: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReviewItem("Full Name", name)
        ReviewItem("Email", email)
        ReviewItem("Phone Number", phone)
        ReviewItem("User Type", userType.name)
        ReviewItem("Address", address.address)
        ReviewItem("Coordinates", "(${address.latitude}, ${address.longitude})")
        if (userType == UserType.EMPLOYEE) {
            ReviewItem("Education Level", educationLevel.name.replace("_", " ").lowercase().capitalize())
            ReviewItem("Language Certificate", languageCertificate.name.replace("_", " ").lowercase().capitalize())
        } else {
            ReviewItem("Company Name", companyName)
        }
    }
}
