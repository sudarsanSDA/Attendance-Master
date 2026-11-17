package com.sda.attendance.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val time: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val sessionName: String = "Session 1",
    val records: Map<String, AttendanceRecord> = emptyMap(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val totalCount: Int get() = records.size
    val presentCount: Int get() = records.values.count { it.status == AttendanceStatus.PRESENT }
    val absentCount: Int get() = records.values.count { it.status == AttendanceStatus.ABSENT }
    val lateCount: Int get() = records.values.count { it.isLate }

    fun getPresentPercentage(): Int {
        return if (totalCount > 0) (presentCount * 100) / totalCount else 0
    }

    /**
     * Generates a beautifully formatted WhatsApp / Text report for teachers
     * with GATE and NON-GATE clearly indicated, icons, counts, and absentees roll numbers.
     */
    fun formatTeacherReport(students: List<Student>): String {
        val studentMap = students.associateBy { it.rollNumber }
        val gateStudents = students.filter { it.isGate }
        val nonGateStudents = students.filter { !it.isGate }

        val gatePresent = gateStudents.count { records[it.rollNumber]?.status == AttendanceStatus.PRESENT }
        val gateTotal = gateStudents.size
        val nonGatePresent = nonGateStudents.count { records[it.rollNumber]?.status == AttendanceStatus.PRESENT }
        val nonGateTotal = nonGateStudents.size

        val sb = StringBuilder()
        sb.append("📋 *ATTENDANCE REPORT*\n")
        sb.append("📅 Date: $date | ⏰ $time\n")
        sb.append("🏷️ Session: $sessionName\n")
        sb.append("━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📊 Summary: *${presentCount}/${totalCount} Present* (${absentCount} Absent)\n")
        sb.append("• GATE: $gatePresent/$gateTotal Present\n")
        sb.append("• NON-GATE: $nonGatePresent/$nonGateTotal Present\n")
        sb.append("━━━━━━━━━━━━━━━━━━━\n\n")

        // GATE Section
        sb.append("🎓 *GATE STUDENTS* ($gatePresent/$gateTotal):\n")
        gateStudents.forEach { student ->
            val record = records[student.rollNumber]
            val isPresent = record?.status == AttendanceStatus.PRESENT
            val isLate = record?.isLate == true
            val icon = if (isPresent) (if (isLate) "⏱️" else "✅") else "❌"
            val statusText = if (isPresent) (if (isLate) "Present (Late)" else "Present") else "ABSENT"
            sb.append("$icon ${student.rollNumber} - ${student.displayName} ($statusText)\n")
        }

        // NON-GATE Section
        sb.append("\n📚 *NON-GATE STUDENTS* ($nonGatePresent/$nonGateTotal):\n")
        nonGateStudents.forEach { student ->
            val record = records[student.rollNumber]
            val isPresent = record?.status == AttendanceStatus.PRESENT
            val isLate = record?.isLate == true
            val icon = if (isPresent) (if (isLate) "⏱️" else "✅") else "❌"
            val statusText = if (isPresent) (if (isLate) "Present (Late)" else "Present") else "ABSENT"
            sb.append("$icon ${student.rollNumber} - ${student.displayName} ($statusText)\n")
        }

        // Absentees Section
        val absentees = students.filter { records[it.rollNumber]?.status != AttendanceStatus.PRESENT }
        sb.append("\n━━━━━━━━━━━━━━━━━━━\n")
        if (absentees.isEmpty()) {
            sb.append("🎉 *100% Full Attendance! No Absentees.*")
        } else {
            sb.append("🚨 *ABSENTEES LIST (${absentees.size}):*\n")
            absentees.forEach { student ->
                val typeTag = if (student.isGate) "[GATE]" else "[NON-GATE]"
                sb.append("• $typeTag ${student.rollNumber} - ${student.displayName}\n")
            }
            sb.append("\n📌 *Absentees Roll Nos:* ")
            sb.append(absentees.joinToString(", ") { it.rollNumber })
        }

        if (notes.isNotBlank()) {
            sb.append("\n\n📝 Note: $notes")
        }

        return sb.toString()
    }

    /**
     * Compact format focusing on Absentees (many teachers request this).
     */
    fun formatAbsenteesOnlyReport(students: List<Student>): String {
        val absentees = students.filter { records[it.rollNumber]?.status != AttendanceStatus.PRESENT }
        val gateAbsentees = absentees.filter { it.isGate }
        val nonGateAbsentees = absentees.filter { !it.isGate }

        val sb = StringBuilder()
        sb.append("🚨 *ABSENTEES - $sessionName ($date, $time)*\n")
        sb.append("Present: $presentCount/$totalCount | Absent: $absentCount\n\n")

        if (gateAbsentees.isNotEmpty()) {
            sb.append("🎓 *GATE Absentees (${gateAbsentees.size}):*\n")
            gateAbsentees.forEach {
                sb.append("• ${it.rollNumber} - ${it.displayName}\n")
            }
            sb.append("\n")
        }

        if (nonGateAbsentees.isNotEmpty()) {
            sb.append("📚 *NON-GATE Absentees (${nonGateAbsentees.size}):*\n")
            nonGateAbsentees.forEach {
                sb.append("• ${it.rollNumber} - ${it.displayName}\n")
            }
            sb.append("\n")
        }

        if (absentees.isEmpty()) {
            sb.append("✅ All 20 students are present!\n")
        } else {
            sb.append("📌 *Absent Roll Nos:* ${absentees.joinToString(", ") { it.rollNumber }}")
        }

        return sb.toString()
    }

    /**
     * Plain roll numbers summary (e.g. Present: ..., Absent: ...)
     */
    fun formatRollNumbersOnlyReport(students: List<Student>): String {
        val presentRolls = students.filter { records[it.rollNumber]?.status == AttendanceStatus.PRESENT }
            .map { it.rollNumber }
        val absentRolls = students.filter { records[it.rollNumber]?.status != AttendanceStatus.PRESENT }
            .map { it.rollNumber }

        return buildString {
            append("📅 $sessionName ($date $time)\n")
            append("✅ Present (${presentRolls.size}): ${presentRolls.joinToString(", ")}\n\n")
            append("❌ Absent (${absentRolls.size}): ${absentRolls.ifEmpty { listOf("None") }.joinToString(", ")}")
        }
    }
}
