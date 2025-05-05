package com.example.quickwork.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quickwork.ui.screens.AddJobScreen
import com.example.quickwork.ui.screens.CheckNotificationScreen
import com.example.quickwork.ui.screens.HiringScreen
import com.example.quickwork.ui.screens.JobDetailScreen
import com.example.quickwork.ui.screens.JobListScreen
import com.example.quickwork.ui.screens.JobManageScreen
import com.example.quickwork.ui.screens.JobStateScreen
import com.example.quickwork.ui.screens.LoginScreen
import com.example.quickwork.ui.screens.ProfileScreen
import com.example.quickwork.ui.screens.RegisterScreen
import com.example.quickwork.ui.screens.SearchScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "addJobScreen"
    ) {
        composable("login") { LoginScreen(navController) }
          composable("register") { RegisterScreen(navController) }
        //composable("profile") { ProfileScreen(navController) }
        composable("jobList") { JobListScreen(navController) }
        composable("jobManage") { JobManageScreen(navController) }
        composable("addJobScreen") { AddJobScreen(navController) }

//        composable("jobDetail") { JobDetailScreen(navController) }
//        composable("jobManage") { JobManageScreen(navController) }
//        composable("jobState") { JobStateScreen(navController) }
//        composable("hiring") { HiringScreen(navController) }
//        composable("search") { SearchScreen(navController) }
//        composable("checkNotification") { CheckNotificationScreen(navController) }
    }
}