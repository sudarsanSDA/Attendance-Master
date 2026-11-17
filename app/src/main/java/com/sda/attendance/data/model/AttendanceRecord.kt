package com.sda.attendance.data.model

enum class AttendanceStatus {
    PRESENT,
    ABSENT;

    val isPresent: Boolean get() = this == PRESENT
}

data class AttendanceRecord(
    val rollNumber: String,
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val isLate: Boolean = false,
    val markedAt: Long = System.currentTimeMillis()
)
