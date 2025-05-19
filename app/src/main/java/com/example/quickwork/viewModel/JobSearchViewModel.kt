package com.example.quickwork.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.Address
import com.example.quickwork.data.models.Category
import com.example.quickwork.data.models.Job
import com.example.quickwork.data.models.JobType
import com.example.quickwork.data.repository.JobRepository
import com.example.quickwork.ui.screens.JobWithDistance
import com.example.quickwork.ui.screens.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.atan2

@RequiresApi(Build.VERSION_CODES.O)
class JobSearchViewModel(
    private val jobRepository: JobRepository = JobRepository(),
    initialKeyword: String = "",
    initialJobType: JobType? = null
) : ViewModel() {
    private val _jobsWithDistance = MutableStateFlow<List<JobWithDistance>>(emptyList())
    val jobsWithDistance: StateFlow<List<JobWithDistance>> = _jobsWithDistance

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _userLocation = MutableStateFlow<Address?>(null)
    val userLocation: StateFlow<Address?> = _userLocation

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchKeyword = MutableStateFlow(initialKeyword)
    val searchKeyword: StateFlow<String> = _searchKeyword

    private val _selectedCategoryIds = MutableStateFlow<List<String>>(emptyList())
    val selectedCategoryIds: StateFlow<List<String>> = _selectedCategoryIds

    private val _selectedJobTypes = MutableStateFlow<List<JobType>>(
        initialJobType?.let { listOf(it) } ?: JobType.values().toList()
    )
    val selectedJobTypes: StateFlow<List<JobType>> = _selectedJobTypes

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate

    private val _startTime = MutableStateFlow<String?>(null)
    val startTime: StateFlow<String?> = _startTime

    private val _sortOption = MutableStateFlow(SortOption.SALARY_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        loadInitialData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _userLocation.value = jobRepository.getUserLocation()
            _categories.value = jobRepository.getCategories()
            updateJobs()
            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
        updateJobs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSelectedCategoryIds(categoryIds: List<String>) {
        _selectedCategoryIds.value = categoryIds
        updateJobs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSelectedJobTypes(jobTypes: List<JobType>) {
        _selectedJobTypes.value = jobTypes
        updateJobs()
    }

    fun updateStartDate(date: String?) {
        _startDate.value = date
        updateJobs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateEndDate(date: String?) {
        _endDate.value = date
        updateJobs()
    }

    fun updateStartTime(time: String?) {
        _startTime.value = time
        updateJobs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
        updateJobs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            val allJobs = jobRepository.getJobs()
            val filteredJobs = allJobs.filter { job ->
                val keywordLower = _searchKeyword.value.lowercase()
                val nameMatch = job.name.lowercase().contains(keywordLower)
                val detailMatch = job.detail.lowercase().contains(keywordLower)
                val keywordPass = _searchKeyword.value.isBlank() || nameMatch || detailMatch
                val categoryPass = _selectedCategoryIds.value.isEmpty() || job.categoryIds.any { it in _selectedCategoryIds.value }
                val typePass = _selectedJobTypes.value.isEmpty() || job.type in _selectedJobTypes.value
                val startDatePass = _startDate.value?.let { filterDate ->
                    try {
                        val jobStartDate = LocalDate.parse(job.dateStart, dateFormatter)
                        val filterStartDate = LocalDate.parse(filterDate, dateFormatter)
                        !jobStartDate.isBefore(filterStartDate)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                val endDatePass = _endDate.value?.let { filterDate ->
                    try {
                        val jobEndDate = LocalDate.parse(job.dateEnd, dateFormatter)
                        val filterEndDate = LocalDate.parse(filterDate, dateFormatter)
                        !jobEndDate.isAfter(filterEndDate)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                val timePass = _startTime.value?.let { filterTime ->
                    try {
                        val jobStartTime = LocalTime.parse(job.workingHoursStart, timeFormatter)
                        val filterStartTime = LocalTime.parse(filterTime, timeFormatter)
                        !jobStartTime.isBefore(filterStartTime)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
                keywordPass && categoryPass && typePass && startDatePass && endDatePass && timePass
            }

            _jobsWithDistance.value = filteredJobs.map { job ->
                val distance = _userLocation.value?.let { userLoc ->
                    if (job.address.latitude != 0.0 && job.address.longitude != 0.0) {
                        calculateDistance(
                            userLoc.latitude,
                            userLoc.longitude,
                            job.address.latitude,
                            job.address.longitude
                        )
                    } else {
                        null
                    }
                }
                JobWithDistance(job, distance)
            }.sortedWith(when (_sortOption.value) {
                SortOption.SALARY_ASC -> compareBy { it.job.salary }
                SortOption.SALARY_DESC -> compareByDescending { it.job.salary }
                SortOption.DATE_NEWEST -> compareByDescending { it.job.dateUpload }
                SortOption.DATE_OLDEST -> compareBy { it.job.dateUpload }
                SortOption.NAME_ASC -> compareBy { it.job.name.lowercase() }
                SortOption.DISTANCE_ASC -> compareBy(nullsLast()) { it.distance }
            })
            _isLoading.value = false
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}