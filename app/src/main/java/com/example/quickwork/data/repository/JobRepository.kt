package com.example.quickwork.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.quickwork.data.models.*
import com.example.quickwork.viewModel.JobWithEmployeeCount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.ChronoLocalDate
import java.time.format.DateTimeFormatter

class JobRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getJobs(): List<Job> {
        return try {
            val querySnapshot = firestore.collection("jobs").get().await()
            querySnapshot.documents.mapNotNull { doc ->
                try {
                    Job(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type")?.let { JobType.valueOf(it) } ?: JobType.PARTTIME,
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
                        address = doc.get("address")?.let { addressMap ->
                            Address(
                                latitude = (addressMap as Map<*, *>)["latitude"] as? Double ?: 0.0,
                                longitude = addressMap["longitude"] as? Double ?: 0.0,
                                address = addressMap["address"] as? String ?: "",
                                timestamp = addressMap["timestamp"] as? Long ?: 0L
                            )
                        } ?: Address(),
                        attendanceCode = doc.getString("attendanceCode")
                    )
                } catch (e: Exception) {
                    Log.w("JobRepository", "Error parsing job ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load jobs", e)
            emptyList()
        }
    }

    suspend fun getCategories(): List<Category> {
        return try {
            val querySnapshot = firestore.collection("category").get().await()
            querySnapshot.documents.mapNotNull { doc ->
                try {
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("JobRepository", "Error parsing category ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load categories", e)
            emptyList()
        }
    }

    suspend fun getUserLocation(): Address? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.address
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load user location", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLatestJob(jobs: List<Job>): Job? {
        return jobs.maxByOrNull { job ->
            try {
                LocalDate.parse(job.dateUpload, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: Exception) {
                LocalDate.MIN
            }
        }
    }

    suspend fun getJobDetails(jobId: String): Job? {
        return try {
            val document = firestore.collection("jobs").document(jobId).get().await()
            if (document.exists()) {
                val baseJob = Job(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    type = JobType.valueOf(document.getString("type") ?: "PARTTIME"),
                    employerId = document.getString("employerId") ?: "",
                    detail = document.getString("detail") ?: "",
                    imageUrl = document.getString("imageUrl") ?: "",
                    salary = document.getLong("salary")?.toInt() ?: 0,
                    insurance = document.getLong("insurance")?.toInt() ?: 0,
                    dateUpload = document.getString("dateUpload") ?: "",
                    workingHoursStart = document.getString("workingHoursStart") ?: "",
                    workingHoursEnd = document.getString("workingHoursEnd") ?: "",
                    dateStart = document.getString("dateStart") ?: "",
                    dateEnd = document.getString("dateEnd") ?: "",
                    employees = emptyList(),
                    employeeRequired = document.getLong("employeeRequired")?.toInt() ?: 0,
                    companyName = document.getString("companyName") ?: "Unknown",
                    categoryIds = document.get("categoryIds") as? List<String> ?: emptyList(),
                    address = Address(),
                    attendanceCode = document.getString("attendanceCode")
                )

                val employeeDocs = firestore.collection("jobs")
                    .document(jobId)
                    .collection("employees")
                    .get()
                    .await()
                val employeeList = employeeDocs.map { empDoc ->
                    val attendanceList = (empDoc["attendance"] as? List<Map<String, Any>>)?.map { att ->
                        DailyAttendance(
                            date = att["date"] as? String ?: "",
                            status = AttendanceStatus.valueOf(att["status"] as? String ?: "ABSENT")
                        )
                    } ?: emptyList()

                    Employee(
                        id = empDoc.getString("id") ?: "",
                        jobState = JobState.valueOf(empDoc.getString("jobState") ?: "APPLYING"),
                        attendance = attendanceList
                    )
                }

                baseJob.copy(employees = employeeList)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load job $jobId", e)
            null
        }
    }

    suspend fun getEmployerName(employerId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(employerId).get().await()
            userDoc.getString("name") ?: "Unknown Employer"
        } catch (e: Exception) {
            Log.w("JobRepository", "Error fetching employer $employerId", e)
            "Unknown Employer"
        }
    }

    suspend fun getCategoryNames(categoryIds: List<String>): List<String> {
        val categoryNames = mutableListOf<String>()
        for (categoryId in categoryIds) {
            try {
                val categoryDoc = firestore.collection("category").document(categoryId).get().await()
                val categoryName = categoryDoc.getString("name") ?: "Unknown Category"
                categoryNames.add(categoryName)
            } catch (e: Exception) {
                Log.w("JobRepository", "Error fetching category $categoryId", e)
                categoryNames.add("Unknown Category")
            }
        }
        return categoryNames
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun applyForJob(userId: String, jobId: String, dateStart: String, dateEnd: String): Boolean {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val jobRef = firestore.collection("jobs").document(jobId)
            val jobEmployeesRef = jobRef.collection("employees").document(userId)
            val attendanceRef = jobEmployeesRef.collection("attendance")

            // Update user document
            userRef.update("jobList", FieldValue.arrayUnion(jobId)).await()

            // Save employee data
            val employeeData = mapOf(
                "id" to userId,
                "jobState" to JobState.APPLYING.name
            )
            jobEmployeesRef.set(employeeData).await()

            // Generate attendance data
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = LocalDate.parse(dateStart, formatter)
            val endDate = LocalDate.parse(dateEnd, formatter)

            val batch = firestore.batch()
            var date = startDate
            while (!date.isAfter(endDate)) {
                val dateStr = date.format(formatter)
                val attendanceData = mapOf(
                    "date" to dateStr,
                    "status" to AttendanceStatus.ABSENT.name
                )
                val attendanceDoc = attendanceRef.document(dateStr)
                batch.set(attendanceDoc, attendanceData)
                date = date.plusDays(1)
            }

            // Commit attendance records
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to apply for job $jobId", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getUserJobs(userId: String): Pair<List<Map<String, Any>>, Map<String, String>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val jobIds = userDoc.get("jobList") as? List<String> ?: emptyList()
            val jobs = mutableListOf<Map<String, Any>>()
            val states = mutableMapOf<String, String>()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()

            for (jobId in jobIds) {
                val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                if (jobDoc.exists()) {
                    jobDoc.data?.let { jobData ->
                        jobs.add(jobData)
                        val employeeDoc = firestore.collection("jobs")
                            .document(jobId)
                            .collection("employees")
                            .document(userId)
                            .get()
                            .await()
                        var state = if (employeeDoc.exists()) {
                            employeeDoc.getString("jobState") ?: "UNKNOWN"
                        } else {
                            "NOT_APPLIED"
                        }
                        val dateEndStr = jobData["dateEnd"] as? String
                        if (dateEndStr != null && state != JobState.ENDED.name) {
                            try {
                                val dateEnd = LocalDate.parse(dateEndStr, formatter)
                                if (dateEnd.isBefore(today)) {
                                    firestore.collection("jobs")
                                        .document(jobId)
                                        .collection("employees")
                                        .document(userId)
                                        .set(mapOf("jobState" to JobState.ENDED.name))
                                        .await()
                                    state = JobState.ENDED.name
                                    Log.d("JobRepository", "Updated job $jobId to ENDED")
                                }
                            } catch (e: Exception) {
                                Log.w("JobRepository", "Invalid dateEnd format for job $jobId", e)
                            }
                        }
                        states[jobId] = state
                    }
                }
            }
            jobs to states
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load user jobs or states", e)
            emptyList<Map<String, Any>>() to emptyMap()
        }
    }

    suspend fun getAttendanceAndSalaryStatus(userId: String, jobId: String): Pair<List<Map<String, Any>>, Boolean> {
        return try {
            val attendanceDocs = firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(userId)
                .collection("attendance")
                .get()
                .await()
            val attendanceList = attendanceDocs.documents.mapNotNull { it.data }
            val employeeDoc = firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(userId)
                .get()
                .await()
            val receiveSalary = employeeDoc.getBoolean("receiveSalary") ?: false
            attendanceList to receiveSalary
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load attendance or salary status for job $jobId", e)
            emptyList<Map<String, Any>>() to false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun claimSalary(userId: String, jobId: String, stars: Int, comment: String, jobName: String, employerId: String): Boolean {
        return try {
            val ratingData = mapOf(
                "stars" to stars,
                "comment" to comment,
                "jobName" to jobName,
                "date" to LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "ratedId" to userId
            )
            firestore.collection("users")
                .document(employerId)
                .collection("rated")
                .add(ratingData)
                .await()
            firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(userId)
                .update("receiveSalary", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to claim salary for job $jobId", e)
            false
        }
    }

    suspend fun getUserWorkingJobs(userId: String): List<Job> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val jobList = userDoc.get("jobList") as? List<String> ?: emptyList()
            val workingJobs = mutableListOf<Job>()

            for (jobId in jobList) {
                try {
                    val employeeDoc = firestore.collection("jobs")
                        .document(jobId)
                        .collection("employees")
                        .document(userId)
                        .get()
                        .await()
                    val jobState = employeeDoc.getString("jobState")
                    if (jobState == JobState.WORKING.name) {
                        val jobDoc = firestore.collection("jobs").document(jobId).get().await()
                        if (jobDoc.exists()) {
                            workingJobs.add(
                                Job(
                                    id = jobDoc.id,
                                    name = jobDoc.getString("name") ?: "",
                                    type = try {
                                        jobDoc.getString("type")?.let { JobType.valueOf(it) } ?: JobType.PARTTIME
                                    } catch (e: IllegalArgumentException) {
                                        JobType.PARTTIME
                                    },
                                    employerId = jobDoc.getString("employerId") ?: "",
                                    detail = jobDoc.getString("detail") ?: "",
                                    imageUrl = jobDoc.getString("imageUrl") ?: "",
                                    salary = jobDoc.getLong("salary")?.toInt() ?: 0,
                                    insurance = jobDoc.getLong("insurance")?.toInt() ?: 0,
                                    dateUpload = jobDoc.getString("dateUpload") ?: "",
                                    workingHoursStart = jobDoc.getString("workingHoursStart") ?: "",
                                    workingHoursEnd = jobDoc.getString("workingHoursEnd") ?: "",
                                    dateStart = jobDoc.getString("dateStart") ?: "",
                                    dateEnd = jobDoc.getString("dateEnd") ?: "",
                                    employees = emptyList(),
                                    employeeRequired = jobDoc.getLong("employeeRequired")?.toInt() ?: 0,
                                    companyName = jobDoc.getString("companyName") ?: "Unknown",
                                    categoryIds = jobDoc.get("categoryIds") as? List<String> ?: emptyList(),
                                    address = Address(),
                                    attendanceCode = jobDoc.getString("attendanceCode")
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("JobRepository", "Error fetching job $jobId", e)
                }
            }
            workingJobs
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load working jobs", e)
            emptyList()
        }
    }
    fun listenToEmployerJobs(
        employerId: String,
        onUpdate: (List<Job>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        if (employerId.isEmpty()) {
            onError(Exception("No employer ID provided"))
            return null
        }

        return try {
            firestore.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("JobRepository", "Error listening to jobs", error)
                        onError(error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.let { docs ->
                        MainScope().launch {
                            val jobs = docs.mapNotNull { doc ->
                                try {
                                    val employeeDocs = firestore.collection("jobs")
                                        .document(doc.id)
                                        .collection("employees")
                                        .get()
                                        .await()
                                    val employeeList = employeeDocs.documents.mapNotNull { empDoc ->
                                        val empId = empDoc.getString("id") ?: empDoc.id
                                        if (empId.isBlank()) {
                                            Log.w("JobRepository", "Invalid employee document: ${empDoc.data}")
                                            null
                                        } else {
                                            val attendanceDocs = firestore.collection("jobs")
                                                .document(doc.id)
                                                .collection("employees")
                                                .document(empId)
                                                .collection("attendance")
                                                .get()
                                                .await()
                                            val attendanceList = attendanceDocs.documents.mapNotNull { attDoc ->
                                                val date = attDoc.getString("date") ?: ""
                                                val status = attDoc.getString("status")?.let { AttendanceStatus.valueOf(it) }
                                                if (date.isNotBlank() && status != null) {
                                                    DailyAttendance(date = date, status = status)
                                                } else {
                                                    null
                                                }
                                            }
                                            Employee(id = empId, attendance = attendanceList)
                                        }
                                    }
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
                                        employees = employeeList,
                                        employeeRequired = doc.getLong("employeeRequired")?.toInt() ?: 0,
                                        companyName = doc.getString("companyName") ?: "Unknown",
                                        categoryIds = doc.get("categoryIds") as? List<String> ?: emptyList(),
                                        attendanceCode = doc.getString("attendanceCode"),
                                        address = Address()
                                    )
                                } catch (e: Exception) {
                                    Log.w("JobRepository", "Error parsing job ${doc.id}", e)
                                    null
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onUpdate(jobs)
                            }
                        }
                    } ?: run {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to set up jobs listener", e)
            onError(e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun markAttendance(userId: String, job: Job, qrCode: String): String {
        return try {
            if (qrCode != job.attendanceCode) {
                return "Invalid QR code"
            }
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val currentTime = LocalTime.now()
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val startTime = try {
                LocalTime.parse(job.workingHoursStart, timeFormatter)
            } catch (e: Exception) {
                LocalTime.now()
            }
            val status = if (currentTime.isAfter(startTime)) AttendanceStatus.LATE.name else AttendanceStatus.PRESENT.name
            val attendanceData = mapOf(
                "date" to todayStr,
                "status" to status
            )
            firestore.collection("jobs")
                .document(job.id)
                .collection("employees")
                .document(userId)
                .collection("attendance")
                .document(todayStr)
                .set(attendanceData)
                .await()
            "Attendance marked as $status"
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to mark attendance for job ${job.id}", e)
            "Failed to mark attendance"
        }
    }

    suspend fun updateEmployeeState(jobId: String, employeeId: String, state: String): Boolean {
        return try {
            firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(employeeId)
                .update("jobState", state)
                .await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to update employee $employeeId state to $state", e)
            false
        }
    }

    suspend fun removeEmployee(jobId: String, employeeId: String): Boolean {
        return try {
            firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(employeeId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to remove employee $employeeId", e)
            false
        }
    }
    suspend fun addJob(job: Job): Boolean {
        return try {
            val jobId = if (job.id.isBlank()) {
                firestore.collection("jobs").document().id
            } else {
                job.id
            }

            val jobToSave = job.copy(id = jobId)

            firestore.collection("jobs")
                .document(jobId)
                .set(jobToSave)
                .await()

            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to add job ${job.id}", e)
            false
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getActiveJobsWithEmployeeCount(
        employerId: String,
        currentDate: org.threeten.bp.LocalDate?,
        dateFormatter: org.threeten.bp.format.DateTimeFormatter?
    ): List<JobWithEmployeeCount> {
        return try {
            val jobDocs = firestore.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            val jobList = mutableListOf<JobWithEmployeeCount>()
            for (doc in jobDocs) {
                try {
                    val job = doc.toObject(Job::class.java).copy(id = doc.id)
                    val dateEnd = try {
                        LocalDate.parse(job.dateEnd, dateFormatter as DateTimeFormatter?)
                    } catch (e: Exception) {
                        Log.w("JobRepository", "Invalid dateEnd format for job ${doc.id}", e)
                        null
                    }
                    if (dateEnd != null && !dateEnd.isBefore(currentDate as ChronoLocalDate?)) {
                        val employeeDocs = firestore.collection("jobs")
                            .document(doc.id)
                            .collection("employees")
                            .get()
                            .await()
                        val employeeCount = employeeDocs.size()
                        jobList.add(JobWithEmployeeCount(job, employeeCount))
                    }
                } catch (e: Exception) {
                    Log.w("JobRepository", "Error parsing job ${doc.id}", e)
                }
            }
            jobList
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to load jobs for employer $employerId", e)
            emptyList()
        }
    }

    suspend fun addEmployeeToJob(jobId: String, employeeId: String, jobState: JobState): Boolean {
        return try {
            val employeeData = mapOf(
                "id" to employeeId,
                "jobState" to jobState.name,
                "receiveSalary" to false
            )
            firestore.collection("jobs")
                .document(jobId)
                .collection("employees")
                .document(employeeId)
                .set(employeeData)
                .await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to add employee $employeeId to job $jobId", e)
            false
        }
    }


    suspend fun updateAttendanceCode(jobId: String, newCode: String): Boolean {
        return try {
            firestore.collection("jobs")
                .document(jobId)
                .update("attendanceCode", newCode)
                .await()
            true
        } catch (e: Exception) {
            Log.e("JobRepository", "Failed to update attendance code for job $jobId", e)
            false
        }
    }
}