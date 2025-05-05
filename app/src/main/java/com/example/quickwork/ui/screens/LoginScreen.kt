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
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val cardBackgroundColor = Color(0xFFE8F5E9) // Light green for card

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

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
                text = "Welcome Back",
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
                    InputField(label = "Email", value = email, onValueChange = { email = it })

                    PasswordField(
                        label = "Password",
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onVisibilityChange = { passwordVisible = !passwordVisible }
                    )

                    // Forgot Password Link
                    TextButton(
                        onClick = { /* TODO: Handle Forgot Password Navigation */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Login Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields"
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                try {
                                    // Sign in with Firebase Auth
                                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                                    if (authResult.user != null) {
                                        // Navigate to home screen or dashboard
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = when (e.message?.contains("INVALID_EMAIL")) {
                                        true -> "Invalid email format"
                                        false -> when {
                                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Incorrect email or password"
                                            e.message?.contains("TOO_MANY_ATTEMPTS") == true -> "Too many attempts, try again later"
                                            else -> e.message ?: "Login failed"
                                        }

                                        null -> TODO()
                                    }
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
                                text = "Log In",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Navigate to Register
                    TextButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Don't have an account? Sign Up",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
//
//@Composable
//fun InputField(
//    label: String,
//    value: String,
//    onValueChange: (String) -> Unit
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        label = { Text(label) },
//        shape = RoundedCornerShape(12.dp),
//        modifier = Modifier.fillMaxWidth(),
//        singleLine = true
//    )
//}
//
//@Composable
//fun PasswordField(
//    label: String,
//    password: String,
//    onPasswordChange: (String) -> Unit,
//    passwordVisible: Boolean,
//    onVisibilityChange: () -> Unit
//) {
//    OutlinedTextField(
//        value = password,
//        onValueChange = onPasswordChange,
//        label = { Text(label) },
//        shape = RoundedCornerShape(12.dp),
//        modifier = Modifier.fillMaxWidth(),
//        singleLine = true,
//        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//        trailingIcon = {
//            IconButton(onClick = onVisibilityChange) {
//                Icon(
//                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
//                )
//            }
//        }
//    )
//}