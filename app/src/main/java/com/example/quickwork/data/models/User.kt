package com.example.quickwork.data.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: UserType = UserType.EMPLOYEE,

    // Merged Employee fields
    val education: EducationLevel = EducationLevel.NONE,
    val languageCertificate: LanguageCertificate = LanguageCertificate.NONE,
    val jobList: List<String> = emptyList(),

    // Merged Employer fields
    val companyName: String = "",
    val avatarUrl : String? = null

)

enum class UserType {
    EMPLOYER, EMPLOYEE
}
enum class EducationLevel {
    NONE, HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE
}

enum class LanguageCertificate {
    NONE, TOEFL, IELTS, TOEIC, CEFR_B2, CEFR_C1
}