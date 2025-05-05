package com.example.quickwork.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.data.models.User
import com.example.quickwork.data.models.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val cardBackgroundColor = Color(0xFFE8F5E9) // Light green for card

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var userType by remember { mutableStateOf(UserType.EMPLOYEE) }
    var education by remember { mutableStateOf("") } // For Employee
    var languageCertificate by remember { mutableStateOf("") } // For Employee
    var companyName by remember { mutableStateOf("") } // For Employer
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

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
                text = "Create Account",
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
                    InputField(label = "Full Name", value = name, onValueChange = { name = it })
                    InputField(label = "Email", value = email, onValueChange = { email = it })
                    InputField(label = "Phone Number", value = phone, onValueChange = { phone = it })

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

                    // User Type Selection
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Conditional Fields Based on User Type
                    if (userType == UserType.EMPLOYEE) {
                        InputField(label = "Education", value = education, onValueChange = { education = it })
                        InputField(
                            label = "Language Certificate",
                            value = languageCertificate,
                            onValueChange = { languageCertificate = it }
                        )
                    } else if (userType == UserType.EMPLOYER) {
                        InputField(label = "Company Name", value = companyName, onValueChange = { companyName = it })
                    }

                    // Terms and Conditions
                    Text(
                        text = "By continuing, you agree to our\nTerms of Use and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    // Sign Up Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@launch
                                }
                                if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all required fields"
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                try {
                                    // Create user with Firebase Auth
                                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                    val firebaseUser = authResult.user
                                    if (firebaseUser != null) {
                                        // Create User object
                                        val user = User(
                                            uid = firebaseUser.uid,
                                            name = name,
                                            email = email,
                                            phone = phone,
                                            userType = userType,
                                            education = if (userType == UserType.EMPLOYEE) education else "",
                                            languageCertificate = if (userType == UserType.EMPLOYEE) languageCertificate else "",
                                            companyName = if (userType == UserType.EMPLOYER) companyName else ""
                                        )

                                        // Save to Firestore
                                        firestore.collection("users")
                                                .document(firebaseUser.uid)
                                                .set(user)
                                                .await()
                                        // Navigate to login or home screen
                                        if (userType == UserType.EMPLOYEE)
                                        {
                                            navController.navigate("jobList") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                        else{
                                            navController.navigate("jobManage") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }

                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Registration failed"
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
                                text = "Sign Up",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Navigate to Sign In
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
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                )
            }
        }
    )
}