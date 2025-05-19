package com.example.quickwork.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.quickwork.data.models.Rating
import com.example.quickwork.data.models.User
import com.example.quickwork.data.models.UserType
import com.example.quickwork.viewModel.EmployeeWithRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUser(userId: String): User? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.toObject(User::class.java)?.copy(uid = userId)
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to fetch user $userId", e)
            null
        }
    }

    suspend fun getUserRole(userId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("role") ?: "employee"
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to fetch user role for $userId", e)
            "employee"
        }
    }

    suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("name") ?: "Unknown"
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to fetch user name for $userId", e)
            "Unknown"
        }
    }

    suspend fun getRatings(userId: String): List<Rating> {
        return try {
            val ratingDocs = firestore.collection("users")
                .document(userId)
                .collection("rated")
                .get()
                .await()
            ratingDocs.documents.mapNotNull { doc ->
                try {
                    Rating(
                        stars = doc.getLong("stars")?.toInt() ?: 0,
                        comment = doc.getString("comment") ?: "",
                        jobName = doc.getString("jobName") ?: "",
                        date = doc.getString("date") ?: "",
                        ratedId = doc.getString("ratedId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("UserRepository", "Error parsing rating ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to fetch ratings for $userId", e)
            emptyList()
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            // Validate
            if (user.name.isBlank()) throw IllegalArgumentException("Name is required")
            if (user.phone.isBlank()) throw IllegalArgumentException("Phone is required")
            if (user.userType == UserType.EMPLOYER && user.companyName.isBlank()) {
                throw IllegalArgumentException("Company Name is required")
            }

            // Update Firebase Auth profile
            auth.currentUser?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(user.name)
                    .setPhotoUri(user.avatarUrl?.let { Uri.parse(it) })
                    .build()
            )?.await()

            // Update Firestore
            firestore.collection("users").document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to update user ${user.uid}", e)
            throw e
        }
    }

    suspend fun uploadAvatar(context: Context, uri: Uri): String? {
        return try {
            withContext(Dispatchers.IO) {
                val imageRepository = ImageRepository()
                imageRepository.uploadImageToCloudinary(context, uri)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to upload avatar", e)
            null
        }
    }




    suspend fun getEmployeesWithRatings(): List<EmployeeWithRating> {
        return try {
            val userDocs = firestore.collection("users")
                .whereEqualTo("userType", UserType.EMPLOYEE)
                .get()
                .await()
            val employeeList = mutableListOf<EmployeeWithRating>()
            for (doc in userDocs) {
                try {
                    val user = doc.toObject(User::class.java).copy(uid = doc.id)
                    val ratingDocs = firestore.collection("users")
                        .document(user.uid)
                        .collection("rated")
                        .get()
                        .await()
                    val ratings = ratingDocs.documents.mapNotNull { ratingDoc ->
                        try {
                            ratingDoc.toObject(Rating::class.java)
                        } catch (e: Exception) {
                            Log.w("UserRepository", "Error parsing rating ${ratingDoc.id}", e)
                            null
                        }
                    }
                    val averageRating = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
                    employeeList.add(EmployeeWithRating(user, averageRating))
                } catch (e: Exception) {
                    Log.w("UserRepository", "Error parsing user ${doc.id}", e)
                }
            }
            employeeList
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to load employees", e)
            emptyList()
        }
    }
}