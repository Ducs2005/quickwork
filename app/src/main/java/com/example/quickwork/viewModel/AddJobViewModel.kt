package com.example.quickwork.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.*
import com.example.quickwork.data.repository.CategoryRepository
import com.example.quickwork.data.repository.ImageRepository
import com.example.quickwork.data.repository.JobRepository
import com.example.quickwork.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.regex.Pattern

data class AddJobUiState(
    val currentStep: Int = 0,
    val jobName: String = "",
    val jobType: JobType = JobType.PARTTIME,
    val jobDetail: String = "",
    val jobBenefit: String = "",
    val salary: String = "",
    val insurance: String = "",
    val employeeRequired: String = "",
    val workingHoursStart: String = "",
    val workingHoursEnd: String = "",
    val dateStart: String = "",
    val dateEnd: String = "",
    val educationRequired: EducationLevel? = null,
    val languageRequired: LanguageCertificate? = null,
    val address: Address = Address(),
    val selectedImageUri: Uri? = null,
    val imageUrl: String? = null,
    val selectedCategoryIds: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val userRole: String = "employee",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class AddJobViewModel(
    private val jobRepository: JobRepository = JobRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val imageRepository: ImageRepository = ImageRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddJobUiState())
    val uiState: StateFlow<AddJobUiState> = _uiState

    private val timePattern = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    private val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Fetch user role
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val role = userRepository.getUserRole(userId)
                _uiState.value = _uiState.value.copy(userRole = role)
            }

            // Fetch categories
            val categories = categoryRepository.getCategories()
            _uiState.value = _uiState.value.copy(categories = categories)
        }
    }

    fun updateCurrentStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun updateJobName(name: String) {
        _uiState.value = _uiState.value.copy(jobName = name, errorMessage = null)
    }

    fun updateJobType(type: JobType) {
        _uiState.value = _uiState.value.copy(jobType = type, errorMessage = null)
    }

    fun updateJobDetail(detail: String) {
        _uiState.value = _uiState.value.copy(jobDetail = detail, errorMessage = null)
    }

    fun updateJobBenefit(benefit: String) {
        _uiState.value = _uiState.value.copy(jobBenefit = benefit, errorMessage = null)
    }

    fun updateSalary(salary: String) {
        _uiState.value = _uiState.value.copy(salary = salary, errorMessage = null)
    }

    fun updateInsurance(insurance: String) {
        _uiState.value = _uiState.value.copy(insurance = insurance, errorMessage = null)
    }

    fun updateEmployeeRequired(count: String) {
        _uiState.value = _uiState.value.copy(employeeRequired = count, errorMessage = null)
    }

    fun updateWorkingHoursStart(time: String) {
        _uiState.value = _uiState.value.copy(workingHoursStart = time, errorMessage = null)
    }

    fun updateWorkingHoursEnd(time: String) {
        _uiState.value = _uiState.value.copy(workingHoursEnd = time, errorMessage = null)
    }

    fun updateDateStart(date: String) {
        _uiState.value = _uiState.value.copy(dateStart = date, errorMessage = null)
    }

    fun updateDateEnd(date: String) {
        _uiState.value = _uiState.value.copy(dateEnd = date, errorMessage = null)
    }

    fun updateEducationRequired(level: EducationLevel?) {
        _uiState.value = _uiState.value.copy(educationRequired = level, errorMessage = null)
    }

    fun updateLanguageRequired(cert: LanguageCertificate?) {
        _uiState.value = _uiState.value.copy(languageRequired = cert, errorMessage = null)
    }

    fun updateAddress(address: Address) {
        _uiState.value = _uiState.value.copy(address = address, errorMessage = null)
    }

    fun updateSelectedImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri, errorMessage = null)
    }

    fun updateImageUrl(url: String?) {
        _uiState.value = _uiState.value.copy(imageUrl = url)
    }

    fun toggleCategoryId(categoryId: String) {
        val currentIds = _uiState.value.selectedCategoryIds
        val updatedIds = if (currentIds.contains(categoryId)) {
            currentIds - categoryId
        } else {
            currentIds + categoryId
        }
        _uiState.value = _uiState.value.copy(selectedCategoryIds = updatedIds, errorMessage = null)
    }

    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }

    fun handleAddressResult(addressJson: String) {
        try {
            val address = Json.decodeFromString(Address.serializer(), addressJson)
            updateAddress(address)
        } catch (e: Exception) {
            setErrorMessage("Failed to parse address")
        }
    }

    fun validateStep(step: Int): String? {
        return when (step) {
            0 -> {
                if (_uiState.value.jobName.isBlank()) "Job Title is required"
                else if (_uiState.value.selectedCategoryIds.isEmpty()) "Select at least one category"
                else null
            }
            1 -> {
                if (_uiState.value.jobDetail.isBlank()) "Job Details are required"
                else if (_uiState.value.jobBenefit.isBlank()) "Job Benefits are required"
                else null
            }
            2 -> {
                if (_uiState.value.salary.isBlank() || _uiState.value.salary.toIntOrNull() == null) "Valid Salary is required"
                else if (_uiState.value.insurance.isBlank() || _uiState.value.insurance.toIntOrNull() == null) "Valid Insurance is required"
                else if (_uiState.value.employeeRequired.isBlank() || _uiState.value.employeeRequired.toIntOrNull() == null) "Valid Number of Employees is required"
                else null
            }
            3 -> {
                if (!timePattern.matcher(_uiState.value.workingHoursStart).matches()) "Start Hours must be in HH:mm format"
                else if (!timePattern.matcher(_uiState.value.workingHoursEnd).matches()) "End Hours must be in HH:mm format"
                else null
            }
            4 -> {
                if (!datePattern.matcher(_uiState.value.dateStart).matches()) "Start Date must be in yyyy-MM-dd format"
                else if (!datePattern.matcher(_uiState.value.dateEnd).matches()) "End Date must be in yyyy-MM-dd format"
                else null
            }
            5 -> {
                if (_uiState.value.educationRequired == null) "Education Level is required"
                else if (_uiState.value.languageRequired == null) "Language Certificate is required"
                else null
            }
            6 -> {
                if (_uiState.value.address.address.isBlank()) "Address is required"
                else null
            }
            7 -> {
                if (_uiState.value.selectedImageUri == null) "Job Image is required"
                else null
            }
            8 -> null // Review step has no validation
            else -> null
        }
    }

    fun submitJob(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                setErrorMessage("You must be logged in to add a job")
                return@launch
            }

//            if (_uiState.value.userRole != "EMPLOYER") {
//                setErrorMessage("Only employers can add jobs")
//                return@launch
//            }

            setLoading(true)
            try {
                val imageUri = _uiState.value.selectedImageUri
                if (imageUri == null) {
                    setErrorMessage("Job image is required")
                    setLoading(false)
                    return@launch
                }

                val uploadedUrl = imageRepository.uploadImageToCloudinary(context, imageUri)
                if (uploadedUrl == null) {
                    setErrorMessage("Failed to upload image")
                    setLoading(false)
                    return@launch
                }
                updateImageUrl(uploadedUrl)

                val job = Job(
                    id = "", // Will be set by Firestore
                    name = _uiState.value.jobName,
                    type = _uiState.value.jobType,
                    employerId = user.uid,
                    detail = _uiState.value.jobDetail,
                    salary = _uiState.value.salary.toInt(),
                    insurance = _uiState.value.insurance.toInt(),
                    dateUpload = LocalDate.now().format(formatter),
                    workingHoursStart = _uiState.value.workingHoursStart,
                    workingHoursEnd = _uiState.value.workingHoursEnd,
                    dateStart = _uiState.value.dateStart,
                    dateEnd = _uiState.value.dateEnd,
                    employeeRequired = _uiState.value.employeeRequired.toInt(),
                    imageUrl = uploadedUrl,
                    companyName = user.displayName ?: "Unknown",
                    categoryIds = _uiState.value.selectedCategoryIds,
                    educationRequired = _uiState.value.educationRequired,
                    languageRequired = _uiState.value.languageRequired,
                    address = _uiState.value.address
                )

                val success = jobRepository.addJob(job)
                if (success) {
                    onSuccess()
                } else {
                    setErrorMessage("Failed to add job")
                }
            } catch (e: Exception) {
                setErrorMessage(e.message ?: "Failed to add job")
            } finally {
                setLoading(false)
            }
        }
    }
}