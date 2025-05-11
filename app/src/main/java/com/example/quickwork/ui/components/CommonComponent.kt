package com.example.quickwork.ui.screens

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickwork.ui.screens.GreenMain

import com.example.quickwork.R


private val GreenMain = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)
private val GrayText = Color(0xFF616161)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReusableTopAppBar(
    title: String,
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate("jobManage") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.logo_jobsgo),
                    contentDescription = "JobsGo Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = {
                //Log.w("ReusableTopAppBar", "Search icon clicked, empty route specified")
                 navController.navigate("hiring") // Skipped due to empty route
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                //Log.w("ReusableTopAppBar", "Notification icon clicked, empty route specified")
                 navController.navigate("notification") // Skipped due to empty route
            }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GreenMain,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = Modifier.shadow(8.dp)
    )
}

data class BottomNavItem(val name: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)


@Composable
fun ReusableBottomNavBar(navController: NavController) {
    val navItems = listOf(
        BottomNavItem("Home", "jobManage", Icons.Default.Home),
        BottomNavItem("Add", "addJobScreen", Icons.Default.Add),
        BottomNavItem("Message", "chatRoom", Icons.AutoMirrored.Default.Chat),
        BottomNavItem("Hiring", "hiring", Icons.Default.Person),
        BottomNavItem("Setting", "setting", Icons.Default.Settings)
    )

    // Log current route for debugging
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    Log.d("ReusableBottomNavBar", "Current route: $currentRoute")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenMain)
            .padding(vertical = 8.dp)
            .heightIn(min = 56.dp), // Ensure enough height for labels
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        navItems.forEach { item ->
            // Improved isSelected check to handle nested/dynamic routes
            val isSelected = currentRoute?.startsWith(item.route) == true || currentRoute == item.route
            Log.d("ReusableBottomNavBar", "Checking item: ${item.route}, isSelected: $isSelected")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = if (isSelected) Color.Yellow else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    color = if (isSelected) Color.Yellow else Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}