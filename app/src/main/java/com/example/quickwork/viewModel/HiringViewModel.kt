package com.example.quickwork.viewModel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.*
import com.example.quickwork.data.repository.JobRepository
import com.example.quickwork.data.repository.NotificationRepository
import com.example.quickwork.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID

data class EmployeeWithRating(
    val user: User,
    val averageRating: Double
)

data class JobWithEmployeeCount(
    val job: Job,
    val employeeCount: Int
)

data class HiringUiState(
    val employees: List<EmployeeWithRating> = emptyList(),
    val filteredEmployees: List<EmployeeWithRating> = emptyList(),
    val jobs: List<JobWithEmployeeCount> = emptyList(),
    val selectedEmployee: User? = null,
    val selectedEducation: EducationLevel? = null,
    val selectedLanguage: LanguageCertificate? = null,
    val isLoading: Boolean = true,
    val isJobLoading: Boolean = false,
    val errorMessage: String? = null
)

class HiringViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val jobRepository: JobRepository = JobRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HiringUiState())
    val uiState: StateFlow<HiringUiState> = _uiState
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val currentDate = LocalDate.now()

    init {
        fetchEmployees()
    }

    private fun fetchEmployees() {
        viewModelScope.launch {
            try {
                val employees = userRepository.getEmployeesWithRatings()
                _uiState.value = _uiState.value.copy(
                    employees = employees.sortedBy { it.user.name },
                    filteredEmployees = employees,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load employees: ${e.message}"
                )
            }
        }
    }

    fun updateEducationFilter(education: EducationLevel?) {
        _uiState.value = _uiState.value.copy(selectedEducation = education)
        applyFilters()
    }

    fun updateLanguageFilter(language: LanguageCertificate?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = _uiState.value.employees.filter { employee ->
            val matchesEducation = _uiState.value.selectedEducation?.let { employee.user.education == it } ?: true
            val matchesLanguage = _uiState.value.selectedLanguage?.let { employee.user.languageCertificate == it } ?: true
            matchesEducation && matchesLanguage
        }
        _uiState.value = _uiState.value.copy(filteredEmployees = filtered)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun selectEmployee(employee: User?) {
        _uiState.value = _uiState.value.copy(selectedEmployee = employee)
        if (employee != null) {
            fetchJobs()
        } else {
            _uiState.value = _uiState.value.copy(jobs = emptyList(), isJobLoading = false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchJobs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isJobLoading = true)
                val employerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val jobs = jobRepository.getActiveJobsWithEmployeeCount(employerId, currentDate, dateFormatter)
                _uiState.value = _uiState.value.copy(jobs = jobs, isJobLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isJobLoading = false,
                    errorMessage = "Failed to load jobs: ${e.message}"
                )
            }
        }
    }

    fun inviteEmployee(job: Job) {
        viewModelScope.launch {
            try {
                val employee = _uiState.value.selectedEmployee ?: return@launch
                val employerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val employerName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Employer"

                // Add employee to job
                val success = jobRepository.addEmployeeToJob(
                    jobId = job.id,
                    employeeId = employee.uid,
                    jobState = JobState.INVITING
                )
                if (!success) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Failed to invite employee")
                    return@launch
                }

                // Create notification
                val notification = Notification(
                    id = UUID.randomUUID().toString(),
                    title = "Job Invitation",
                    content = "You have been invited to join '${job.name}' by $employerName.",
                    from = employerId,
                    isReaded = false,
                    timestamp = System.currentTimeMillis()
                )
                notificationRepository.addNotification(employee.uid, notification)

                _uiState.value = _uiState.value.copy(
                    selectedEmployee = null,
                    jobs = emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to invite employee: ${e.message}"
                )
            }
        }
    }
}