package com.example.ems

data class AttendanceModel(
    val employeeName: String = "",
    val attendanceType: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val imageUrl: String = ""
)
