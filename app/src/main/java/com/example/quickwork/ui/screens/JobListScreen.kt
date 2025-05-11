package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.quickwork.R
import com.example.quickwork.data.models.Address
import com.example.quickwork.data.models.Employee
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.example.quickwork.ui.components.BottomNavigation
import com.example.quickwork.ui.components.Header
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GreenMain = Color(0xFF4CAF50)

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var latestJob by remember { mutableStateOf<Job?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("jobs")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    jobs = querySnapshot.documents.mapNotNull { doc ->
                        try {
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
                                employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0,
                                companyName = doc.getString("companyName") ?: "Unknown",
                                categoryIds = doc.get("categoryIds") as? List<String> ?: emptyList(),
                                address = Address()

                            )
                        } catch (e: Exception) {
                            Log.w("JobListScreen", "Error parsing job ${doc.id}", e)
                            null
                        }
                    }

                    latestJob = jobs.maxByOrNull { job ->
                        try {
                            LocalDate.parse(job.dateUpload, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        } catch (e: Exception) {
                            LocalDate.MIN
                        }
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
        bottomBar = { BottomNavigation(navController, currentScreen = "jobList") },
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.7f)
                )
            )
        )
    ) { innerPadding ->
        HomeContent(
            jobs = jobs,
            latestJob = latestJob,
            isLoading = isLoading,
            modifier = Modifier.padding(innerPadding),
            navController = navController
        )
    }
}

@Composable
fun HomeContent(
    jobs: List<Job>,
    latestJob: Job?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        SectionHeader(
            title = "Việc mới tải lên",
 //           onSeeMoreClick = { navController.navigate("jobSearchResult/recommended/null") }
            onSeeMoreClick = { navController.navigate("jobSearchResult//null") }

        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else if (latestJob == null) {
            Text(
                text = "Không có việc đề xuất nào.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 16.sp,
                color = Color.Gray
            )
        } else {
            SuggestedJobCard(job = latestJob, navController = navController)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(
            title = "Việc bán thời gian",
            onSeeMoreClick = { navController.navigate("jobSearchResult//PARTTIME") }
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else {
            val partTimeJobs = jobs.filter { it.type == JobType.PARTTIME }
            if (partTimeJobs.isEmpty()) {
                Text(
                    text = "Không có việc bán thời gian nào.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(partTimeJobs) { job ->
                        InternshipCard(job = job, navController = navController)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(
            title = "Việc toàn thời gian",
            onSeeMoreClick = { navController.navigate("jobSearchResult//FULLTIME") }
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenMain)
            }
        } else {
            val fullTimeJobs = jobs.filter { it.type == JobType.FULLTIME }
            if (fullTimeJobs.isEmpty()) {
                Text(
                    text = "Không có việc toàn thời gian nào.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fullTimeJobs) { job ->
                        InternshipCard(job = job, navController = navController)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black
        )
        TextButton(onClick = onSeeMoreClick) {
            Text(
                text = "Xem thêm >>",
                color = GreenMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SuggestedJobCard(job: Job, navController: NavController) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp)
            .clickable(
                onClick = { navController.navigate("jobDetail/${job.id}") },
                onClickLabel = "View job details"
            )
            .scale(animatedScale)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GreenMain)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = job.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Company: ${job.companyName}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Salary: $${job.salary}/hour",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Start: ${job.dateStart}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Hours: ${job.workingHoursStart}-${job.workingHoursEnd}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (job.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(job.imageUrl),
                    contentDescription = "Job Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_job_image),
                    contentDescription = "Default Job Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun InternshipCard(job: Job, navController: NavController) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(200.dp)
            .clickable(
                onClick = { navController.navigate("jobDetail/${job.id}") },
                onClickLabel = "View job details"
            )
            .scale(animatedScale)
            .animateContentSize(),
        shape = RoundedCornerShape(1.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (job.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(job.imageUrl),
                    contentDescription = "Job Image",
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.padding(horizontal = 2.dp),
                //verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = job.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${job.salary}/hour",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = GreenMain
                )
                Text(
                    text = "Start: ${job.dateStart}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Hours: ${job.workingHoursStart}-${job.workingHoursEnd}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}