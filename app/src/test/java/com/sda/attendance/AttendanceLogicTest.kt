package com.sda.attendance

import com.sda.attendance.data.model.AttendanceRecord
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.AttendanceStatus
import com.sda.attendance.data.model.DefaultStudents
import com.sda.attendance.data.model.RosterParser
import com.sda.attendance.data.model.Student
import org.junit.Assert.*
import org.junit.Test

class AttendanceLogicTest {

    @Test
    fun testSampleStudentsList() {
        val students = DefaultStudents.sampleList
        assertEquals(5, students.size)
        assertTrue(students.any { it.isGate })
        assertTrue(students.any { !it.isGate })
    }

    @Test
    fun testRosterParserBulkImport() {
        val rawPastedText = """
1	101	9277	ALEX CARTER	GATE	Male	9876543210
2	102	21237	EMILY DAVIS	GATE	Female	9876543211
3	103	16061	LIAM JOHNSON	NON-GATE	Male	9876543212
4	104	35362	SOPHIA WILLIAMS	NON-GATE	Female	9876543213
5	105	19915	NOAH BROWN	NON-GATE	Male	9876543214
        """.trimIndent()

        val parsed = RosterParser.parse(rawPastedText)
        assertEquals(5, parsed.size)

        // Check first student
        assertEquals("101", parsed[0].rollNumber)
        assertEquals("ALEX CARTER", parsed[0].fullName)
        assertEquals("A. CARTER", parsed[0].displayName)
        assertTrue(parsed[0].isGate)
        assertEquals("9876543210", parsed[0].phoneNumber)

        // Check GATE students
        val gateCount = parsed.count { it.isGate }
        assertEquals(2, gateCount)

        // Check Non-GATE students
        val nonGateCount = parsed.count { !it.isGate }
        assertEquals(3, nonGateCount)
    }

    @Test
    fun testInitialNamesConversion() {
        val s1 = Student("101", "ALEX CARTER")
        assertEquals("A. CARTER", s1.toInitialName())

        val s2 = Student("102", "PETER PARKER")
        assertEquals("P. PARKER", s2.toInitialName())

        val s3 = Student("103", "BRUCE WAYNE")
        assertEquals("B. WAYNE", s3.toInitialName())
    }

    @Test
    fun testTeacherReportFormatting() {
        val students = DefaultStudents.sampleList
        val records = students.associate { s ->
            val status = if (s.rollNumber == "102") AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
            s.rollNumber to AttendanceRecord(rollNumber = s.rollNumber, status = status)
        }

        val session = AttendanceSession(
            date = "2026-11-17",
            time = "09:30 AM",
            sessionName = "Session 1",
            records = records
        )

        assertEquals(5, session.totalCount)
        assertEquals(4, session.presentCount)
        assertEquals(1, session.absentCount)

        val report = session.formatTeacherReport(students)
        assertTrue(report.contains("4/5 Present"))
        assertTrue(report.contains("102"))

        val absenteesReport = session.formatAbsenteesOnlyReport(students)
        assertTrue(absenteesReport.contains("102"))
    }
}
