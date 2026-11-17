package com.sda.attendance.data.model

data class Student(
    val rollNumber: String,
    val fullName: String,
    val displayName: String = fullName,
    val isGate: Boolean = false,
    val phoneNumber: String = "",
    val gender: String = "",
    val category: String = ""
) {
    /**
     * Generates an initial name format, e.g.:
     * "ALEX CARTER" -> "A. CARTER"
     * "EMILY DAVIS" -> "E. DAVIS"
     */
    fun toInitialName(): String {
        val trimmed = fullName.trim()
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size <= 1) return trimmed

        val first = parts[0].uppercase()
        val rest = parts.drop(1).joinToString(" ")

        val initial = when {
            first == "SHAIK" -> "SK."
            first.endsWith(".") -> first
            first.length == 1 -> "$first."
            else -> "${first.first()}."
        }
        return "$initial $rest"
    }
}
