package com.sda.attendance.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sda.attendance.data.model.AttendanceRecord
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AttendanceRepository(context: Context) {

    private val dbHelper = AttendanceDbHelper(context.applicationContext)
    private val gson = Gson()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _sessions = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val sessions: StateFlow<List<AttendanceSession>> = _sessions.asStateFlow()

    suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        refreshStudents()
        refreshSessions()
    }

    suspend fun refreshStudents() = withContext(Dispatchers.IO) {
        val list = dbHelper.getAllStudents()
        _students.value = list
    }

    suspend fun refreshSessions() = withContext(Dispatchers.IO) {
        val list = dbHelper.getAllSessions()
        _sessions.value = list
    }

    // -----------------------------------------------------------------
    // Student Methods
    // -----------------------------------------------------------------
    suspend fun saveStudent(student: Student) = withContext(Dispatchers.IO) {
        dbHelper.upsertStudent(student)
        refreshStudents()
    }

    suspend fun deleteStudent(rollNumber: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteStudent(rollNumber)
        refreshStudents()
    }

    suspend fun resetStudentsToDefault() = withContext(Dispatchers.IO) {
        dbHelper.resetStudentsToDefault()
        refreshStudents()
    }

    suspend fun bulkImportStudents(students: List<Student>, replaceExisting: Boolean = false) = withContext(Dispatchers.IO) {
        if (replaceExisting) {
            dbHelper.replaceAllStudents(students)
        } else {
            students.forEach { dbHelper.upsertStudent(it) }
        }
        refreshStudents()
    }

    suspend fun clearAllStudents() = withContext(Dispatchers.IO) {
        dbHelper.clearAllStudents()
        refreshStudents()
    }

    suspend fun loadSampleStudents() = withContext(Dispatchers.IO) {
        dbHelper.replaceAllStudents(com.sda.attendance.data.model.DefaultStudents.sampleList)
        refreshStudents()
    }

    suspend fun convertAllToInitialNames() = withContext(Dispatchers.IO) {
        val current = dbHelper.getAllStudents()
        val updated = current.map { s ->
            s.copy(displayName = s.toInitialName())
        }
        dbHelper.updateAllStudents(updated)
        refreshStudents()
    }

    suspend fun convertAllToFullNames() = withContext(Dispatchers.IO) {
        val current = dbHelper.getAllStudents()
        val updated = current.map { s ->
            s.copy(displayName = s.fullName)
        }
        dbHelper.updateAllStudents(updated)
        refreshStudents()
    }

    // -----------------------------------------------------------------
    // Session Methods (Taking & Editing Attendance / Adding Late Comers)
    // -----------------------------------------------------------------
    suspend fun saveSession(session: AttendanceSession) = withContext(Dispatchers.IO) {
        dbHelper.saveSession(session)
        refreshSessions()
    }

    suspend fun updateSession(session: AttendanceSession) = withContext(Dispatchers.IO) {
        val updated = session.copy(updatedAt = System.currentTimeMillis())
        dbHelper.saveSession(updated)
        refreshSessions()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteSession(sessionId)
        refreshSessions()
    }

    suspend fun markSessionSynced(sessionId: String, isSynced: Boolean = true) = withContext(Dispatchers.IO) {
        dbHelper.markSessionSynced(sessionId, isSynced)
        refreshSessions()
    }

    suspend fun getSessionById(sessionId: String): AttendanceSession? = withContext(Dispatchers.IO) {
        dbHelper.getSessionById(sessionId)
    }

    // -----------------------------------------------------------------
    // Backup / Export
    // -----------------------------------------------------------------
    fun exportAllSessionsJson(): String {
        return gson.toJson(_sessions.value)
    }

    fun exportToCsv(): String {
        val sb = StringBuilder()
        sb.append("SessionID,Date,Time,SessionName,RollNumber,StudentName,IsGate,Status,IsLate\n")
        val studentMap = _students.value.associateBy { it.rollNumber }
        _sessions.value.forEach { session ->
            session.records.forEach { (roll, record) ->
                val s = studentMap[roll]
                val name = s?.displayName ?: ""
                val isGate = if (s?.isGate == true) "GATE" else "NON-GATE"
                sb.append("\"${session.id}\",\"${session.date}\",\"${session.time}\",\"${session.sessionName}\",\"$roll\",\"$name\",\"$isGate\",\"${record.status}\",\"${record.isLate}\"\n")
            }
        }
        return sb.toString()
    }
}
