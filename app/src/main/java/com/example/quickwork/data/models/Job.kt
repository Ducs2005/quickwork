package com.example.quickwork.data.models


// Job Data Class
data class Job(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val employerId: String = "",
    val detail: String = "",
    val salary: Int,
    val insurance: Int,
    val dateUpload: String = ""
)
