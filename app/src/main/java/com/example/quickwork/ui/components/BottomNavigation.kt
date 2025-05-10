package com.example.quickwork.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.R

private val GreenMain = Color(0xFF4CAF50)

@Composable
fun BottomNavigation(
    navController: NavController,
    currentScreen: String = "jobList"
) {
    Surface(
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.7f)
                    )
                )
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                BottomNavItem("Trang Chủ", R.drawable.ic_home, "jobList"),
                BottomNavItem("Hồ Sơ", R.drawable.ic_part_time_job, "jobState"),
                BottomNavItem("Lịch trình", R.drawable.ic_utilities, "schedule"),
                BottomNavItem("Chat", R.drawable.ic_chat, "chatRoom"),
                BottomNavItem("Setting", R.drawable.ic_menu, "setting")
            )

            navItems.forEach { item ->
                val isSelected = currentScreen == item.route
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "scale_${item.route}"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .scale(scale)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title,
                        tint = if (isSelected) GreenMain else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) GreenMain else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 32.dp else 0.dp)
                            .height(3.dp)
                            .background(
                                color = if (isSelected) GreenMain else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: Int,
    val route: String
)