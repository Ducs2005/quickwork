package com.example.quickwork.viewModel


import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.quickwork.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class AddJobViewModel : ViewModel() {
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _jobName = MutableStateFlow("")
    val jobName: StateFlow<String> = _jobName.asStateFlow()

    private val _jobType = MutableStateFlow(JobType.PARTTIME)
    val jobType: StateFlow<JobType> = _jobType.asStateFlow()

    private val _jobDetail = MutableStateFlow("")
    val jobDetail: StateFlow<String> = _jobDetail.asStateFlow()

    private val _jobBenefit = MutableStateFlow("")
    val jobBenefit: StateFlow<String> = _jobBenefit.asStateFlow()

    private val _salary = MutableStateFlow("")
    val salary: StateFlow<String> = _salary.asStateFlow()

    private val _insurance = MutableStateFlow("")
    val insurance: StateFlow<String> = _insurance.asStateFlow()

    private val _employeeRequired = MutableStateFlow("")
    val employeeRequired: StateFlow<String> = _employeeRequired.asStateFlow()

    private val _workingHoursStart = MutableStateFlow("")
    val workingHoursStart: StateFlow<String> = _workingHoursStart.asStateFlow()

    private val _workingHoursEnd = MutableStateFlow("")
    val workingHoursEnd: StateFlow<String> = _workingHoursEnd.asStateFlow()

    private val _dateStart = MutableStateFlow("")
    val dateStart: StateFlow<String> = _dateStart.asStateFlow()

    private val _dateEnd = MutableStateFlow("")
    val dateEnd: StateFlow<String> = _dateEnd.asStateFlow()

    private val _educationRequired = MutableStateFlow<EducationLevel?>(null)
    val educationRequired: StateFlow<EducationLevel?> = _educationRequired.asStateFlow()

    private val _languageRequired = MutableStateFlow<LanguageCertificate?>(null)
    val languageRequired: StateFlow<LanguageCertificate?> = _languageRequired.asStateFlow()

    private val _address = MutableStateFlow(Address())
    val address: StateFlow<Address> = _address.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<List<String>>(emptyList())
    val selectedCategoryIds: StateFlow<List<String>> = _selectedCategoryIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateCurrentStep(step: Int) {
        _currentStep.value = step
    }

    fun updateJobName(name: String) {
        _jobName.value = name
    }

    fun updateJobType(type: JobType) {
        _jobType.value = type
    }

    fun updateJobDetail(detail: String) {
        _jobDetail.value = detail
    }

    fun updateJobBenefit(benefit: String) {
        _jobBenefit.value = benefit
    }

    fun updateSalary(salary: String) {
        _salary.value = salary
    }

    fun updateInsurance(insurance: String) {
        _insurance.value = insurance
    }

    fun updateEmployeeRequired(count: String) {
        _employeeRequired.value = count
    }

    fun updateWorkingHoursStart(start: String) {
        _workingHoursStart.value = start
    }

    fun updateWorkingHoursEnd(end: String) {
        _workingHoursEnd.value = end
    }

    fun updateDateStart(start: String) {
        _dateStart.value = start
    }

    fun updateDateEnd(end: String) {
        _dateEnd.value = end
    }

    fun updateEducationRequired(level: EducationLevel?) {
        _educationRequired.value = level
    }

    fun updateLanguageRequired(cert: LanguageCertificate?) {
        _languageRequired.value = cert
    }

    fun updateAddress(address: Address) {
        _address.value = address
    }

    fun updateSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun updateImageUrl(url: String?) {
        _imageUrl.value = url
    }

    fun toggleCategoryId(id: String) {
        _selectedCategoryIds.value = if (_selectedCategoryIds.value.contains(id)) {
            _selectedCategoryIds.value - id
        } else {
            _selectedCategoryIds.value + id
        }
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun handleAddressResult(addressJson: String?) {
        addressJson?.let {
            try {
                val selectedAddress = Json.decodeFromString<Address>(it)
                updateAddress(selectedAddress)
            } catch (e: Exception) {
                setErrorMessage("Failed to parse address")
            }
        }
    }
}