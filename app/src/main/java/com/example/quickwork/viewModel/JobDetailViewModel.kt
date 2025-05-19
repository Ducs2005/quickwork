package com.example.quickwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Employee
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobState
import com.example.quickwork.data.repository.JobRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JobDetailViewModel(
    private val jobId: String,
    private val jobRepository: JobRepository = JobRepository()
) : ViewModel() {
    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasApplied = MutableStateFlow(false)
    val hasApplied: StateFlow<Boolean> = _hasApplied

    private val _employerName = MutableStateFlow("")
    val employerName: StateFlow<String> = _employerName

    private val _categoryNames = MutableStateFlow<List<String>>(emptyList())
    val categoryNames: StateFlow<List<String>> = _categoryNames

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadJobDetails()
    }

    private fun loadJobDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            val jobDetails = jobRepository.getJobDetails(jobId)
            _job.value = jobDetails
            if (jobDetails != null) {
                _employerName.value = jobRepository.getEmployerName(jobDetails.employerId)
                _categoryNames.value = jobRepository.getCategoryNames(jobDetails.categoryIds)
                _hasApplied.value = userId?.let { uid ->
                    jobDetails.employees.any { it.id == uid }
                } ?: false
            }
            _isLoading.value = false
        }
    }

    fun applyForJob() {
        if (userId != null && _job.value != null) {
            viewModelScope.launch {
                val job = _job.value!!
                val success = jobRepository.applyForJob(
                    userId = userId,
                    jobId = job.id,
                    dateStart = job.dateStart,
                    dateEnd = job.dateEnd
                )
                if (success) {
                    _job.value = job.copy(
                        employees = job.employees + Employee(
                            id = userId,
                            jobState = JobState.APPLYING,
                            attendance = emptyList()
                        )
                    )
                    _hasApplied.value = true
                }
            }
        }
    }
}