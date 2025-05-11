package com.example.quickwork.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.quickwork.data.models.JobType
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

    var targetDestination by remember { mutableStateOf<String?>(null) }

    // Set after checking auth and Firestore
    LaunchedEffect(Unit) {
        targetDestination = if (currentUser == null) {
            "login"
        } else {
            try {
                val uid = currentUser.uid
                val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
                val isRecruiter = userDoc.getString("userType") == "EMPLOYER"
                if (isRecruiter) "jobManage" else "jobList"
            } catch (e: Exception) {
                e.printStackTrace()
                "login"
            }
        }
        targetDestination = "register"
    }

    // Always start from splash
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen()
            LaunchedEffect(targetDestination) {
                if (targetDestination != null) {
                    navController.navigate(targetDestination!!) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        composable("login") { LoginScreen(navController) }
        composable("mapPicker") { MapPickerScreen(navController) }

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
        composable(
            route = "jobSearchResult/{keyword}/{jobType}",
            arguments = listOf(
                navArgument("keyword") { type = NavType.StringType },
                navArgument("jobType") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val keyword = backStackEntry.arguments?.getString("keyword") ?: ""
            val jobTypeString = backStackEntry.arguments?.getString("jobType")
            val jobType = try {
                jobTypeString?.let { JobType.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
            JobSearchResultsScreen(
                navController = navController,
                keyword = keyword,
                selectedJobType = jobType
            )
        }
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(userId = userId, navController)
        }
        composable("hiring") { HiringScreen(navController) }

        composable("chatRoom") { MessageRoomsScreen(navController) }
        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChatScreen(navController, userId)
        }
    }
}
