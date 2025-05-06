package com.example.quickwork.data.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: UserType = UserType.EMPLOYEE,

    // Merged Employee fields
    val education: String = "",
    val languageCertificate: String = "",
    val jobList: List<String> = emptyList(),

    // Merged Employer fields
    val companyName: String = "",


)

enum class UserType {
    EMPLOYER, EMPLOYEE
}
