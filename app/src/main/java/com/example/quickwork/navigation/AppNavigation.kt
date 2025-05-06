package com.example.quickwork.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quickwork.ui.screens.AddJobScreen
import com.example.quickwork.ui.screens.JobDetailScreen
import com.example.quickwork.ui.screens.JobListScreen
import com.example.quickwork.ui.screens.JobManageScreen
import com.example.quickwork.ui.screens.LoginScreen
import com.example.quickwork.ui.screens.RegisterScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "jobList"
    ) {
        composable("login") { LoginScreen(navController) }
          composable("register") { RegisterScreen(navController) }
        //composable("profile") { ProfileScreen(navController) }
        composable("jobList") { JobListScreen(navController) }
        composable("jobManage") { JobManageScreen(navController) }
        composable("addJobScreen") { AddJobScreen(navController) }
        composable("jobDetail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            if (jobId != null) {
                JobDetailScreen(navController = navController, jobId = jobId)
            }
        }

//        composable("jobDetail") { JobDetailScreen(navController) }
//        composable("jobManage") { JobManageScreen(navController) }
//        composable("jobState") { JobStateScreen(navController) }
//        composable("hiring") { HiringScreen(navController) }
//        composable("search") { SearchScreen(navController) }
//        composable("checkNotification") { CheckNotificationScreen(navController) }
    }
}