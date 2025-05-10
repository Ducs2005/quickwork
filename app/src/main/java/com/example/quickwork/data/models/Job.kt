package com.example.quickwork.data.models

enum class AttendanceStatus {
    PRESENT, LATE, ABSENT
}

data class DailyAttendance(
    val date: String = "", // "2025-05-10"
    val status: AttendanceStatus = AttendanceStatus.ABSENT
)

data class Employee(
    val id: String = "", // Employee ID
    val jobState: JobState = JobState.APPLYING,
    val attendance: List<DailyAttendance> = emptyList(),
    val receiveSalary: Boolean = false
)

enum class JobState {
    APPLYING, INVITING, PRESENT, WORKING, ENDED, DENIED
}

enum class JobType {
    FULLTIME, PARTTIME
}

data class Job(
    val id: String = "",
    val name: String = "",
    val type: JobType = JobType.PARTTIME,
    val employerId: String = "",
    val detail: String = "",
    val imageUrl: String = "",
    val salary: Int = 0,
    val insurance: Int = 0,
    val dateUpload: String = "",
    val workingHoursStart: String = "", // e.g., "08:00"
    val workingHoursEnd: String = "", // e.g., "17:00"
    val dateStart: String = "", // e.g., "2025-05-10"
    val dateEnd: String = "", // e.g., "2025-06-30"
    val employees: List<Employee> = emptyList(),
    val employeeRequired: Int = 0,
    val companyName: String = "Unknown",
    val categoryIds: List<String> = emptyList(),
    val attendanceCode: String? = null,
    val educationRequired: EducationLevel? = null,
    val languageRequired: LanguageCertificate? = null
)