package com.example.quickwork.data.models
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: UserType = UserType.EMPLOYEE,
    val address: Address, // Updated to use Address data class

    // Merged Employee fields
    val education: EducationLevel = EducationLevel.NONE,
    val languageCertificate: LanguageCertificate = LanguageCertificate.NONE,
    val jobList: List<String> = emptyList(),

    // Merged Employer fields
    val companyName: String = "",
    val avatarUrl: String? = null
)
@Parcelize
@Serializable
data class Address(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

enum class UserType {
    EMPLOYER, EMPLOYEE
}

enum class EducationLevel {
    NONE, HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE
}

enum class LanguageCertificate {
    NONE, TOEFL, IELTS, TOEIC, CEFR_B2, CEFR_C1
}