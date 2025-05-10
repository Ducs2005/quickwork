package com.example.quickwork.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quickwork.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var startDestination by remember { mutableStateOf<String?>("login") }

    LaunchedEffect(currentUser) {
        startDestination = if (currentUser == null) {
            "login"
        } else {
            try {
                val uid = currentUser.uid
                val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
                val isRecruiter = userDoc.getString("userType") == "EMPLOYER"
                if (isRecruiter) "jobManage" else "jobList" /////jkhkuutiut
            } catch (e: Exception) {
                e.printStackTrace()
                "login" // Fallback in case of error
            }
        }
    }

    // Only show NavHost when startDestination is ready
    startDestination?.let {
        NavHost(
            navController = navController,
            startDestination = it
        ) {
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("jobList") { JobListScreen(navController) }
            composable("notification") { NotificationScreen(navController) }
            composable("schedule") { ScheduleScreen(navController) }

            composable("jobManage") { JobManageScreen(navController) }
            composable("addJobScreen") { AddJobScreen(navController) }
            composable("jobDetail/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")
                if (jobId != null) {
                    JobDetailScreen(navController = navController, jobId = jobId)
                }
            }
            composable("jobState") { JobStateScreen(navController) }
            composable("setting") { SettingScreen(navController) }
            composable("jobSearchResult/{keyword}") { backStackEntry ->
                val keyword = backStackEntry.arguments?.getString("keyword") ?: ""
                JobSearchResultsScreen(navController,keyword)
            }
            composable("profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ProfileScreen( userId = userId, navController)
            }
            composable("chatRoom") { MessageRoomsScreen(navController) }
            composable("chat/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ChatScreen(navController, userId)
            }


        }
    }
}
