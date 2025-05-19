package com.example.quickwork.ui.viewmodels

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.*
import com.example.quickwork.data.repository.JobRepository
import com.example.quickwork.data.repository.NotificationRepository
import com.example.quickwork.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID

enum class JobCategory { MANAGING, INCOMING, ENDED }

data class JobManageUiState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = true,
    val selectedJob: Job? = null,
    val selectedAttendanceJob: Job? = null,
    val selectedCategory: JobCategory = JobCategory.MANAGING,
    val qrCodeBitmap: Bitmap? = null,
    val qrCodeText: String? = null,
    val employeeNames: Map<String, String> = emptyMap(),
    val employeeStates: Map<String, String> = emptyMap(),
    val todayAttendance: Map<String, AttendanceStatus> = emptyMap()
)

class JobManageViewModel(
    private val jobRepository: JobRepository = JobRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobManageUiState())
    val uiState: StateFlow<JobManageUiState> = _uiState

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val today = LocalDate.now()

    init {
        initialize()
    }

    private fun initialize() {
        if (userId != null) {
            listenToJobs()
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun listenToJobs() {
        if (userId != null) {
            listenerRegistration = jobRepository.listenToEmployerJobs(
                employerId = userId,
                onUpdate = { jobs ->
                    _uiState.value = _uiState.value.copy(
                        jobs = jobs,
                        isLoading = false
                    )
                },
                onError = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    fun selectCategory(category: JobCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectJob(job: Job?) {
        _uiState.value = _uiState.value.copy(selectedJob = job)
        if (job != null) {
            loadEmployeeData(job)
        } else {
            _uiState.value = _uiState.value.copy(
                employeeStates = emptyMap(),
                employeeNames = emptyMap(),
                todayAttendance = emptyMap()
            )
        }
    }

    fun selectAttendanceJob(job: Job?) {
        _uiState.value = _uiState.value.copy(selectedAttendanceJob = job)
        if (job != null) {
            loadEmployeeData(job)
        } else {
            _uiState.value = _uiState.value.copy(
                employeeNames = emptyMap(),
                todayAttendance = emptyMap()
            )
        }
    }

    private fun loadEmployeeData(job: Job) {
        viewModelScope.launch {
            val employeeStates = mutableMapOf<String, String>()
            val employeeNames = mutableMapOf<String, String>()
            val todayAttendance = mutableMapOf<String, AttendanceStatus>()
            val todayStr = today.format(formatter)

            job.employees.forEach { employee ->
                // Fetch employee state
                val state = Firebase.firestore.collection("jobs")
                    .document(job.id)
                    .collection("employees")
                    .document(employee.id)
                    .get()
                    .await()
                    .getString("jobState") ?: "APPLYING"
                employeeStates[employee.id] = state

                // Fetch employee name
                val name = userRepository.getUserName(employee.id)
                employeeNames[employee.id] = name

                // Fetch today's attendance
                val attendance = employee.attendance.find { it.date == todayStr }
                todayAttendance[employee.id] = attendance?.status ?: AttendanceStatus.ABSENT
            }

            _uiState.value = _uiState.value.copy(
                employeeStates = employeeStates,
                employeeNames = employeeNames,
                todayAttendance = todayAttendance
            )
        }
    }

    fun acceptEmployee(job: Job, employeeId: String, companyName: String) {
        viewModelScope.launch {
            val success = jobRepository.updateEmployeeState(job.id, employeeId, "WORKING")
            if (success) {
                _uiState.value = _uiState.value.copy(
                    employeeStates = _uiState.value.employeeStates + (employeeId to "WORKING")
                )
                notificationRepository.createNotification(
                    employeeId = employeeId,
                    title = "Job Application Accepted",
                    content = "You have been accepted for the job '${job.name}'.",
                    from = companyName
                )
            }
        }
    }

    fun fireEmployee(job: Job, employeeId: String, employerId: String) {
        viewModelScope.launch {
            val success = jobRepository.removeEmployee(job.id, employeeId)
            if (success) {
                val updatedEmployees = job.employees.filter { it.id != employeeId }
                val updatedJob = job.copy(employees = updatedEmployees)
                _uiState.value = _uiState.value.copy(
                    jobs = _uiState.value.jobs.map { if (it.id == job.id) updatedJob else it },
                    selectedJob = if (_uiState.value.selectedJob?.id == job.id) updatedJob else _uiState.value.selectedJob,
                    selectedAttendanceJob = if (_uiState.value.selectedAttendanceJob?.id == job.id) updatedJob else _uiState.value.selectedAttendanceJob,
                    employeeStates = _uiState.value.employeeStates + (employeeId to "DENIED")
                )
                notificationRepository.createNotification(
                    employeeId = employeeId,
                    title = "Job Termination Notice",
                    content = "You have been removed from the job '${job.name}'.",
                    from = employerId
                )
            }
        }
    }

    fun generateQRCode(job: Job) {
        viewModelScope.launch {
            val newCode = UUID.randomUUID().toString()
            val success = jobRepository.updateAttendanceCode(job.id, newCode)
            if (success) {
                val bitmap = createQRCodeBitmap(newCode)
                val updatedJob = job.copy(attendanceCode = newCode)
                _uiState.value = _uiState.value.copy(
                    jobs = _uiState.value.jobs.map { if (it.id == job.id) updatedJob else it },
                    selectedAttendanceJob = if (_uiState.value.selectedAttendanceJob?.id == job.id) updatedJob else _uiState.value.selectedAttendanceJob,
                    qrCodeBitmap = bitmap,
                    qrCodeText = newCode
                )
            }
        }
    }

    private fun createQRCodeBitmap(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.Black.hashCode() else Color.White.hashCode())
            }
        }
        return bitmap
    }

    fun clearQRCode() {
        _uiState.value = _uiState.value.copy(qrCodeBitmap = null, qrCodeText = null)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }

    val managingJobs: List<Job>
        get() = _uiState.value.jobs.filter {
            try {
                val startDate = LocalDate.parse(it.dateStart, formatter)
                val endDate = LocalDate.parse(it.dateEnd, formatter)
                !today.isBefore(startDate) && !today.isAfter(endDate)
            } catch (e: Exception) {
                false
            }
        }

    val incomingJobs: List<Job>
        get() = _uiState.value.jobs.filter {
            try {
                val startDate = LocalDate.parse(it.dateStart, formatter)
                today.isBefore(startDate)
            } catch (e: Exception) {
                false
            }
        }

    val endedJobs: List<Job>
        get() = _uiState.value.jobs.filter {
            try {
                val endDate = LocalDate.parse(it.dateEnd, formatter)
                today.isAfter(endDate)
            } catch (e: Exception) {
                false
            }
        }
}