package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.R
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.example.quickwork.data.models.Employee

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("jobs")
                .get()
                .addOnSuccessListener { querySnapshot ->

                    jobs = querySnapshot.documents.map { doc ->
                        Job(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = JobType.valueOf(doc.getString("type") ?: "PARTTIME"),
                            employerId = doc.getString("employerId") ?: "",
                            detail = doc.getString("detail") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            salary = doc.getLong("salary")?.toInt() ?: 0,
                            insurance = doc.getLong("insurance")?.toInt() ?: 0,
                            dateUpload = doc.getString("dateUpload") ?: "",
                            workingHoursStart = doc.getString("workingHoursStart") ?: "",
                            workingHoursEnd = doc.getString("workingHoursEnd") ?: "",
                            dateStart = doc.getString("dateStart") ?: "",
                            dateEnd = doc.getString("dateEnd") ?: "",
                            employees = (doc.get("employees") as? List<Map<String, Any>>)?.map { employeeMap ->
                                Employee(
                                    id = employeeMap["id"] as? String ?: ""
                                )
                            } ?: emptyList(),
                            employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("JobListScreen", "Failed to load jobs", e)
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = { Header(navController) },
        bottomBar = { BottomNavigation(navController) },

    ) { innerPadding ->
        HomeContent(
            jobs = jobs,
            isLoading = isLoading,
            modifier = Modifier.padding(innerPadding),
            navController
        )
    }
}

@Composable
fun HomeContent(
    jobs: List<Job>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Suggested Jobs Section
        SectionHeader(
            title = "Việc đề xuất cho bạn",
            onSeeMoreClick = { /* Handle See More */ }
        )
        SuggestedJobCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Internships Section
        SectionHeader(
            title = "Việc bán thời gian",
            onSeeMoreClick = { /* Handle See More */ }
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val internshipJobs = jobs.filter { it.type == JobType.PARTTIME }
            if (internshipJobs.isEmpty()) {
                Text(
                    text = "Không có việc thực tập nào.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(internshipJobs) { job ->
                        InternshipCard(job = job, navController)
                    }
                }
            }
        }
        SectionHeader(
            title = "Việc toàn thời gian",
            onSeeMoreClick = { /* Handle See More */ }
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val internshipJobs = jobs.filter { it.type == JobType.FULLTIME }
            if (internshipJobs.isEmpty()) {
                Text(
                    text = "Không có việc làm toàn thời gian nào.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(internshipJobs) { job ->
                        InternshipCard(job = job, navController = navController )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeMoreClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
        TextButton(onClick = onSeeMoreClick) {
            Text(
                text = "Xem thêm >>",
                color = Color(0xFF1976D2),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_job_image),
                contentDescription = "Job Image",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lập Trình Viên Python Yêu Cầu Dj...",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Thỏa thuận - Toàn quốc - 608 km",
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InternshipCard(job: Job, navController: NavController) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clickable {navController.navigate("jobDetail/${job.id}")},
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (job.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(job.imageUrl),
                    contentDescription = "Job Image",
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}