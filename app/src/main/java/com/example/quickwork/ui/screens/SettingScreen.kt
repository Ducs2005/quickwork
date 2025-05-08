package com.example.quickwork.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        SettingItem(title = "Language") {
            // Navigate to your LanguageScreen (you must define this destination in NavGraph)
            navController.navigate("language")
        }

        SettingItem(title = "Notifications") {
            // Navigate to your NotificationScreen
            navController.navigate("notifications")
        }

        Spacer(modifier = Modifier.weight(1f)) // Push logout to bottom

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                Log.d("SettingScreen", "User logged out.")
                // Navigate back to login screen after logout
                navController.navigate("login") {
                    popUpTo("settings") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }
}

@Composable
fun SettingItem(title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Divider()
    }
}
