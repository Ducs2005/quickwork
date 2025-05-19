package com.example.quickwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.repository.JobRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JobStateViewModel(
    private val jobRepository: JobRepository = JobRepository()
) : ViewModel() {
    private val _jobList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val jobList: StateFlow<List<Map<String, Any>>> = _jobList

    private val _employeeStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val employeeStates: StateFlow<Map<String, String>> = _employeeStates

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId: StateFlow<String?> = _selectedJobId

    private val _attendanceList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val attendanceList: StateFlow<List<Map<String, Any>>> = _attendanceList

    private val _receiveSalary = MutableStateFlow(false)
    val receiveSalary: StateFlow<Boolean> = _receiveSalary

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadUserJobs()
    }

    private fun loadUserJobs() {
        if (userId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                val (jobs, states) = jobRepository.getUserJobs(userId)
                _jobList.value = jobs
                _employeeStates.value = states
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    fun selectJob(jobId: String?) {
        _selectedJobId.value = jobId
        if (jobId != null && userId != null) {
            viewModelScope.launch {
                val (attendance, salaryStatus) = jobRepository.getAttendanceAndSalaryStatus(userId, jobId)
                _attendanceList.value = attendance
                _receiveSalary.value = salaryStatus
            }
        } else {
            _attendanceList.value = emptyList()
            _receiveSalary.value = false
        }
    }

    fun claimSalary(jobId: String, stars: Int, comment: String, jobName: String, employerId: String) {
        if (userId != null) {
            viewModelScope.launch {
                val success = jobRepository.claimSalary(userId, jobId, stars, comment, jobName, employerId)
                if (success) {
                    _receiveSalary.value = true
                }
            }
        }
    }
}