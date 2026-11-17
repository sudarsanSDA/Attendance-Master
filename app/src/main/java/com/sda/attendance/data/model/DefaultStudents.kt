package com.sda.attendance.data.model

object DefaultStudents {
    /**
     * Generic sample students for public/demo mode.
     * Contains no private or personal classmate data.
     */
    val sampleList = listOf(
        Student(rollNumber = "101", fullName = "ALEX CARTER", displayName = "A. CARTER", isGate = true),
        Student(rollNumber = "102", fullName = "EMILY DAVIS", displayName = "E. DAVIS", isGate = true),
        Student(rollNumber = "103", fullName = "LIAM JOHNSON", displayName = "L. JOHNSON", isGate = false),
        Student(rollNumber = "104", fullName = "SOPHIA WILLIAMS", displayName = "S. WILLIAMS", isGate = false),
        Student(rollNumber = "105", fullName = "NOAH BROWN", displayName = "N. BROWN", isGate = false)
    )

    val list = sampleList
}
