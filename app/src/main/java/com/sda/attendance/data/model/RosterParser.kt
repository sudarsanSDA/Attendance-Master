package com.sda.attendance.data.model

object RosterParser {

    /**
     * Parses a multi-line pasted string into a list of Student objects.
     * Supports:
     * - Tab-separated values (copied from Excel / Google Sheets)
     * - Comma-separated values (CSV)
     * - Space-separated (e.g. "101 John Doe" or "101 John Doe GATE")
     */
    fun parse(text: String): List<Student> {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val students = mutableListOf<Student>()

        for (line in lines) {
            val student = parseLine(line)
            if (student != null) {
                students.add(student)
            }
        }
        return students
    }

    private fun parseLine(line: String): Student? {
        // 1. Check for Tab-Separated (Excel / Sheets paste)
        if (line.contains("\t")) {
            val tokens = line.split("\t").map { it.trim() }.filter { it.isNotEmpty() }
            return parseTokens(tokens, line)
        }

        // 2. Check for Comma-Separated (CSV)
        if (line.contains(",")) {
            val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
            return parseTokens(tokens, line)
        }

        // 3. Fallback: Whitespace separated (e.g. "101 Alex Carter GATE")
        val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        val roll = tokens.firstOrNull { it.matches("\\d{2,}".toRegex()) } ?: tokens.first()
        val isGate = line.contains("GATE", ignoreCase = true)

        val nameTokens = tokens.filter {
            it != roll && !it.equals("GATE", ignoreCase = true) &&
                    !it.equals("NON-GATE", ignoreCase = true) &&
                    !it.matches("\\d{10}".toRegex()) &&
                    it.length > 1
        }
        val fullName = if (nameTokens.isNotEmpty()) nameTokens.joinToString(" ") else "Student $roll"

        val phone = tokens.firstOrNull { it.matches("\\d{10}".toRegex()) } ?: ""

        val temp = Student(
            rollNumber = roll,
            fullName = fullName,
            isGate = isGate,
            phoneNumber = phone
        )
        return temp.copy(displayName = temp.toInitialName())
    }

    private fun parseTokens(tokens: List<String>, rawLine: String): Student? {
        if (tokens.isEmpty()) return null

        // Find roll number: look for 4+ digit number, or first integer token
        var roll = ""
        var name = ""
        var isGate = rawLine.contains("GATE", ignoreCase = true)
        var phone = ""

        // Try to identify phone (10 digits)
        val phoneCandidate = tokens.firstOrNull { it.matches("\\d{10}".toRegex()) }
        if (phoneCandidate != null) phone = phoneCandidate

        // Identify roll number
        val rollCandidate = tokens.firstOrNull {
            it != phone && it.matches("\\d{3,}".toRegex())
        } ?: tokens.firstOrNull { it != phone && it.matches("\\d+".toRegex()) && it.length >= 2 }
        ?: tokens[0]

        roll = rollCandidate

        // Identify name: longest alphabetic/space string
        val nameCandidate = tokens.filter {
            it != roll && it != phone &&
                    !it.equals("GATE", ignoreCase = true) &&
                    !it.equals("PGECET", ignoreCase = true) &&
                    !it.equals("PGECET-GATE", ignoreCase = true) &&
                    !it.equals("Male", ignoreCase = true) &&
                    !it.equals("Female", ignoreCase = true) &&
                    it.length >= 2
        }.maxByOrNull { it.length }

        name = nameCandidate ?: "Student $roll"

        val temp = Student(
            rollNumber = roll,
            fullName = name,
            isGate = isGate,
            phoneNumber = phone
        )
        return temp.copy(displayName = temp.toInitialName())
    }
}
