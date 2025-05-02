package com.example.quickwork.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.quickwork.R
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header

@Composable
fun JobListScreen(navController: NavController) {
    Scaffold(
        topBar = { Header() },
        bottomBar = { BottomNavigation(navController) },
        content = { innerPadding ->
            HomeContent(Modifier.padding(innerPadding))
        }
    )
}


@Composable
fun HomeContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // Suggested Jobs Section
        SectionHeader(title = "Việc đề xuất cho bạn", onSeeMoreClick = { /* Handle See More */ })
        SuggestedJobCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Internships Section
        SectionHeader(title = "Việc thực tập", onSeeMoreClick = { /* Handle See More */ })
        LazyRow(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(count = 5) {
                InternshipCard()
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        TextButton(onClick = onSeeMoreClick) {
            Text(
                text = "Xem thêm >>",
                color = Color.Blue,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SuggestedJobCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(150.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1976D2))
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_job_image),
                contentDescription = "Job Image",
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Lập Trình Viên Python Yêu Cầu Dj...",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "Thỏa thuận - Toàn quốc - 608 km",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun InternshipCard() {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(150.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Thực Tập Sinh Marketing",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
