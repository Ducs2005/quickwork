package com.example.quickwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JobViewModel(private val jobRepository: JobRepository = JobRepository()) : ViewModel() {
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs

    private val _latestJob = MutableStateFlow<Job?>(null)
    val latestJob: StateFlow<Job?> = _latestJob

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadJobs()
    }

    private fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            val jobList = jobRepository.getJobs()
            _jobs.value = jobList
            _latestJob.value = jobRepository.getLatestJob(jobList)
            _isLoading.value = false
        }
    }
}