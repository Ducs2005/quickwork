package com.example.quickwork.data.models

data class Job(
    val id: String = "",
    val name: String = "",
    val type: JobType = JobType.PARTTIME,
    val employerId: String = "",
    val detail: String = "",
    val imageUrl: String = "",
    val salary: Int,
    val insurance: Int,
    val dateUpload: String = "",
    val workingHoursStart: String = "",  // ví dụ: "08:00"
    val workingHoursEnd: String = "",    // ví dụ: "17:00"
    val dateStart: String = "",          // ví dụ: "2025-05-10"
    val dateEnd: String = "" ,            // ví dụ: "2025-06-30"
    val employees: List<Employee> = emptyList(),
    val employeeRequired: Int,
    val companyName : String = "Unknow",
    val categoryIds : List<String> = emptyList(),
    val attendanceCode : String? = null
)


enum class AttendanceStatus {
    PRESENT, LATE, ABSENT
}

data class DailyAttendance(
    val date: String = "", // "2025-05-10"
    val status: AttendanceStatus = AttendanceStatus.ABSENT
)

data class Employee(
    val id: String = "", // Employee ID
    val jobState:JobState  = JobState.APPLYING,
    val attendance: List<DailyAttendance> = emptyList()
)
enum class JobState {
    APPLYING, PRESENT, WORKING, ENDED, DENIED
}
enum class JobType {
    FULLTIME, PARTTIME
}
