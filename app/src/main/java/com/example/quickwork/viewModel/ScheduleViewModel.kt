package com.example.quickwork.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.repository.JobRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields

class ScheduleViewModel(
    private val jobRepository: JobRepository = JobRepository()
) : ViewModel() {
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    @RequiresApi(Build.VERSION_CODES.O)
    private val _currentWeekStart = MutableStateFlow(LocalDate.now().with(WeekFields.ISO.firstDayOfWeek))
    @RequiresApi(Build.VERSION_CODES.O)
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    private val _scanFeedback = MutableStateFlow<String?>(null)
    val scanFeedback: StateFlow<String?> = _scanFeedback

    private val _selectedJob = MutableStateFlow<Job?>(null)
    private val _scanResult = MutableStateFlow<String?>(null)

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadWorkingJobs()
    }

    private fun loadWorkingJobs() {
        if (userId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                _jobs.value = jobRepository.getUserWorkingJobs(userId)
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setWeekStart(weekStart: LocalDate) {
        _currentWeekStart.value = weekStart
    }

    fun selectJobAndScan(job: Job, qrCode: String?) {
        if (qrCode != null) {
            _selectedJob.value = job
            _scanResult.value = qrCode
            markAttendance()
        } else {
            _scanFeedback.value = "No QR code result received"
            _selectedJob.value = null
            _scanResult.value = null
        }
    }

    private fun markAttendance() {
        val job = _selectedJob.value
        val qrCode = _scanResult.value
        if (userId != null && job != null && qrCode != null) {
            viewModelScope.launch {
                val feedback = jobRepository.markAttendance(userId, job, qrCode)
                _scanFeedback.value = feedback
                _selectedJob.value = null
                _scanResult.value = null
            }
        }
    }

    fun clearFeedback() {
        _scanFeedback.value = null
    }
}