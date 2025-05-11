package com.example.quickwork.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.quickwork.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _currentStep = MutableStateFlow(savedStateHandle.get<Int>("currentStep") ?: 0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _name = MutableStateFlow(savedStateHandle.get<String>("name") ?: "")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow(savedStateHandle.get<String>("email") ?: "")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _phone = MutableStateFlow(savedStateHandle.get<String>("phone") ?: "")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow(savedStateHandle.get<String>("password") ?: "")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow(savedStateHandle.get<String>("confirmPassword") ?: "")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _passwordVisible = MutableStateFlow(savedStateHandle.get<Boolean>("passwordVisible") ?: false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _confirmPasswordVisible = MutableStateFlow(savedStateHandle.get<Boolean>("confirmPasswordVisible") ?: false)
    val confirmPasswordVisible: StateFlow<Boolean> = _confirmPasswordVisible.asStateFlow()

    private val _userType = MutableStateFlow(savedStateHandle.get<UserType>("userType") ?: UserType.EMPLOYEE)
    val userType: StateFlow<UserType> = _userType.asStateFlow()

    private val _address = MutableStateFlow(savedStateHandle.get<Address>("address") ?: Address())
    val address: StateFlow<Address> = _address.asStateFlow()

    private val _educationLevel = MutableStateFlow(savedStateHandle.get<EducationLevel>("educationLevel") ?: EducationLevel.NONE)
    val educationLevel: StateFlow<EducationLevel> = _educationLevel.asStateFlow()

    private val _languageCertificate = MutableStateFlow(savedStateHandle.get<LanguageCertificate>("languageCertificate") ?: LanguageCertificate.NONE)
    val languageCertificate: StateFlow<LanguageCertificate> = _languageCertificate.asStateFlow()

    private val _companyName = MutableStateFlow(savedStateHandle.get<String>("companyName") ?: "")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateCurrentStep(step: Int) {
        _currentStep.value = step
        savedStateHandle["currentStep"] = step
    }

    fun updateName(value: String) {
        _name.value = value
        savedStateHandle["name"] = value
    }

    fun updateEmail(value: String) {
        _email.value = value
        savedStateHandle["email"] = value
    }

    fun updatePhone(value: String) {
        _phone.value = value
        savedStateHandle["phone"] = value
    }

    fun updatePassword(value: String) {
        _password.value = value
        savedStateHandle["password"] = value
    }

    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
        savedStateHandle["confirmPassword"] = value
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
        savedStateHandle["passwordVisible"] = _passwordVisible.value
    }

    fun toggleConfirmPasswordVisibility() {
        _confirmPasswordVisible.value = !_confirmPasswordVisible.value
        savedStateHandle["confirmPasswordVisible"] = _confirmPasswordVisible.value
    }

    fun updateUserType(type: UserType) {
        _userType.value = type
        savedStateHandle["userType"] = type
    }

    fun updateAddress(value: Address) {
        _address.value = value
        savedStateHandle["address"] = value
    }

    fun updateEducationLevel(level: EducationLevel) {
        _educationLevel.value = level
        savedStateHandle["educationLevel"] = level
    }

    fun updateLanguageCertificate(cert: LanguageCertificate) {
        _languageCertificate.value = cert
        savedStateHandle["languageCertificate"] = cert
    }

    fun updateCompanyName(value: String) {
        _companyName.value = value
        savedStateHandle["companyName"] = value
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}