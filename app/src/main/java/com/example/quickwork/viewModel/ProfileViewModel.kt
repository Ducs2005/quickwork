package com.example.quickwork.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.quickwork.data.models.User
import com.example.quickwork.data.models.Rating
import com.example.quickwork.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val ratings: List<Rating> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editedUser: User? = null,
    val errorMessage: String? = null,
    val averageRating: Double = 0.0
)

class ProfileViewModel(
    private val userId: String?,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    val effectiveUserId: String? = userId.takeIf { it?.isNotEmpty() == true } ?: auth.currentUser?.uid
    val isCurrentUser: Boolean = effectiveUserId == auth.currentUser?.uid

    init {
        loadProfile()
    }

    private fun loadProfile() {
        if (effectiveUserId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No user logged in"
            )
            return
        }

        viewModelScope.launch {
            try {
                // Fetch user data
                val user = userRepository.getUser(effectiveUserId)
                // Fetch ratings
                val ratings = userRepository.getRatings(effectiveUserId)
                val averageRating = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()

                _uiState.value = _uiState.value.copy(
                    user = user,
                    ratings = ratings,
                    averageRating = averageRating,
                    isLoading = false,
                    editedUser = if (isCurrentUser) user else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editedUser = _uiState.value.user,
            errorMessage = null
        )
    }

    fun updateEditedUser(updatedUser: User?) {
        _uiState.value = _uiState.value.copy(editedUser = updatedUser)
    }

    fun saveProfile() {
        val editedUser = _uiState.value.editedUser ?: return
        viewModelScope.launch {
            try {
                userRepository.updateUser(editedUser)
                _uiState.value = _uiState.value.copy(
                    user = editedUser,
                    isEditing = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val downloadUrl = userRepository.uploadAvatar(context, uri)
                if (downloadUrl != null) {
                    val updatedUser = _uiState.value.editedUser?.copy(avatarUrl = downloadUrl)
                    if (updatedUser != null) {
                        userRepository.updateUser(updatedUser)
                        _uiState.value = _uiState.value.copy(
                            user = updatedUser,
                            editedUser = updatedUser,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to update avatar"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to upload avatar"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to upload avatar: ${e.message}"
                )
            }
        }
    }
}

class ProfileViewModelFactory(
    private val userId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}